package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother;
import zone.moddev.mc.skysterrainsmoother.api.MaterialGroup;
import zone.moddev.mc.skysterrainsmoother.content.SandSlabBlock;
import zone.moddev.mc.skysterrainsmoother.internal.MaterialCatalogue;

/** Confirm that the BoP registrations resolve to the exact full blocks and their matching pieces. */
final class NativeSurfaceChecks {
    private NativeSurfaceChecks() {
    }

    static int run(ServerLevel world) {
        int checks = test(world, "origin_grass_block", MaterialGroup.SOIL, true);
        for (String colour : new String[] { "white", "orange", "black" }) {
            checks += test(world, colour + "_sand", MaterialGroup.SAND_AND_GRAVEL, false);
            checks += test(world, colour + "_sandstone", MaterialGroup.STONE, true);
        }
        checks += test(world, "mossy_black_sand", MaterialGroup.SAND_AND_GRAVEL, false);
        SkysTerrainSmoother.logger.info("TERRAIN_BOP_NATIVE_SURFACES_PASS checks={}", checks);
        return checks;
    }

    private static int test(ServerLevel world, String name, MaterialGroup group, boolean finerShapes) {
        var id = ResourceLocation.fromNamespaceAndPath("biomesoplenty", name);
        var source = ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
        var catalogue = MaterialCatalogue.instance();
        var material = catalogue.source(source);
        RuntimeProbe.require(material != null && material.id.equals(id) && material.group == group,
                "The surface has the right material and config group: " + id);
        RuntimeProbe.require((material.step(0) != null) == finerShapes && (material.corner(0) != null) == finerShapes,
                "The surface offers the expected shapes: " + id);
        if (group == MaterialGroup.SAND_AND_GRAVEL) {
            RuntimeProbe.require(material.slab.getBlock() instanceof SandSlabBlock sand && sand.fullState().equals(source),
                    "The slab uses native falling and joining behaviour: " + id);
        } else if (group == MaterialGroup.STONE) {
            RuntimeProbe.require(material.slab.getBlock() instanceof SlabBlock
                    && ForgeRegistries.BLOCKS.getKey(material.slab.getBlock()).equals(id.withPath(name + "_slab")),
                    "Reuse BoP's native slab: " + id);
        }
        BlockPos floor = new BlockPos(5028, 210, 5028);
        var owner = world.getChunkAt(floor);
        for (int level = 1; level <= 3; level++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos column = floor.offset(x, 0, z);
                    world.setBlock(column.below(), Blocks.STONE.defaultBlockState(), 2);
                    world.setBlock(column, source, 2);
                    for (int y = 1; y <= 4; y++) {
                        ProbeWorld.clear(world, column.above(y));
                    }
                }
            }
            world.setBlock(floor.north().above(), source, 2);
            ProbeWorld.rebuildHeights(owner);
            SkysTerrainSmoother.engine.smooth(world, owner, level);
            boolean allowed = !material.grass || zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi.grassGenerationAllowed();
            RuntimeProbe.require(allowed ? catalogue.piece(world.getBlockState(floor.above())) == material
                            : world.isEmptyBlock(floor.above()),
                    "The slope uses its own material at level " + level + ": " + id);
        }
        return 6;
    }
}
