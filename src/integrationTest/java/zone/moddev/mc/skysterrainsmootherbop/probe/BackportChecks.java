package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Biomes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.skysterrainsmoother.api.MaterialGroup;
import zone.moddev.mc.skysterrainsmoother.api.TerrainMaterial;
import zone.moddev.mc.skysterrainsmoother.internal.MaterialCatalogue;
import zone.moddev.mc.skysterrainsmootherbop.NativeSoils;
import zone.moddev.mc.skysterrainsmootherbop.SkysTerrainSmootherBop;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieceBlock;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieces;

/** Keeps the old palette exhaustive while exercising the new shared behaviour. */
final class BackportChecks {
    static int run(WorldServer world) throws Exception {
        int soils = 0;
        for (TerrainMaterial material : MaterialCatalogue.instance().materials()) {
            if (material.id.getNamespace().equals(SkysTerrainSmootherBop.ID)) {
                RuntimeProbe.require(material.group == MaterialGroup.SOIL, "native BOP soils follow the soil switch");
                soils++;
            } else if (material.id.getNamespace().equals("biomesoplenty")) {
                RuntimeProbe.require(material.group == MaterialGroup.STONE, "BOP rocks follow the stone switch");
            }
        }
        RuntimeProbe.require(soils == 14, "all eight grass and six dirt mappings remain available");
        Map<String, String> groups = new HashMap<>();
        for (IRecipe recipe : net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES.getValuesCollection()) {
            if (!recipe.getRegistryName().getNamespace().equals(SkysTerrainSmootherBop.ID)) {
                continue;
            }
            ItemStack output = recipe.getRecipeOutput();
            String identity = output.getItem().getRegistryName() + "/" + output.getMetadata() + "/" + output.getCount();
            String prior = groups.put(recipe.getGroup(), identity);
            RuntimeProbe.require(!recipe.getGroup().isEmpty() && (prior == null || prior.equals(identity)),
                    "a book button never cycles between different materials, shapes or yields");
        }

        BlockPos position = new BlockPos(3512, 220, 3512);
        byte[] biomes = world.getChunk(position).getBiomeArray();
        int index = (position.getZ() & 15) << 4 | position.getX() & 15;
        byte previous = biomes[index];
        int states = 0;
        try {
            biomes[index] = (byte) net.minecraft.world.biome.Biome.getIdForBiome(Biomes.PLAINS);
            for (SoilPieceBlock block : SoilPieces.BLOCKS) {
                for (int slot = 0; slot < block.count; slot++) {
                    for (int orientation = 0; orientation < block.shape.states; orientation++) {
                        IBlockState piece = block.getStateFromMeta(slot * block.shape.states + orientation);
                        IBlockState full = block.full(piece);
                        world.setBlockToAir(position.up());
                        world.setBlockState(position.down(), full, 2);
                        world.setBlockState(position, piece, 2);
                        world.setBlockState(position.north(), Blocks.STONE.getDefaultState(), 2);
                        world.setBlockState(position.north().up(), Blocks.SNOW_LAYER.getDefaultState(), 2);
                        RuntimeProbe.require(block.getActualState(piece, world, position).getValue(BlockGrass.SNOWY),
                                "BOP pieces see snow on a higher terrace: " + piece);
                        RuntimeProbe.require(block.getMetaFromState(world.getBlockState(position))
                                == block.getMetaFromState(piece), "snow appearance never changes palette or orientation");
                        world.setBlockToAir(position.north().up());
                        world.setBlockToAir(position.north());
                        RuntimeProbe.require(!block.getActualState(piece, world, position).getValue(BlockGrass.SNOWY),
                                "the calculated cap clears after the snow is removed");
                        states++;
                    }
                }
            }
        } finally {
            biomes[index] = previous;
            world.setBlockToAir(position);
            world.setBlockToAir(position.down());
            world.setBlockToAir(position.north());
            world.setBlockToAir(position.north().up());
        }
        java.lang.reflect.Field active = zone.moddev.mc.skysterrainsmoother.config.TerrainSmootherConfig.class
                .getDeclaredField("enabledGroups");
        active.setAccessible(true);
        int settings = active.getInt(null);
        int switched = 0;
        try {
            for (TerrainMaterial material : MaterialCatalogue.instance().materials()) {
                if (!material.id.getNamespace().equals(SkysTerrainSmootherBop.ID)
                        && !material.id.getNamespace().equals("biomesoplenty")
                        || material.grass && !zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi.grassGenerationAllowed()) {
                    continue;
                }
                IBlockState source = material.sources.iterator().next();
                slope(world, position, source);
                zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.engine.smooth(world, world.getChunk(position), 3);
                RuntimeProbe.require(MaterialCatalogue.instance().piece(world.getBlockState(position.up())) == material,
                        "enabled BOP family receives its matching piece");
                active.setInt(null, settings & ~(1 << material.group.ordinal()));
                slope(world, position, source);
                zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.engine.smooth(world, world.getChunk(position), 3);
                RuntimeProbe.require(world.isAirBlock(position.up()), "disabled BOP family is not placed");
                ProbeWorld.post(world,
                        new net.minecraftforge.event.terraingen.DecorateBiomeEvent.Post(world, new java.util.Random(1),
                                new BlockPos(position.getX() & ~15, 0, position.getZ() & ~15)));
                RuntimeProbe.require(world.isAirBlock(position.up()), "late decoration respects the same family switch");
                active.setInt(null, settings);
                switched++;
            }
        } finally {
            active.setInt(null, settings);
        }
        System.out.println("BOP_TERRAIN_BACKPORT_PASS soils=" + soils + " snow_states=" + states
                + " switched_materials=" + switched
                + " result_groups=" + groups.size());
        return soils + states * 3 + groups.size();
    }

    private static void slope(WorldServer world, BlockPos position, IBlockState source) {
        for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
            for (int offsetX = -2; offsetX <= 2; offsetX++) {
                BlockPos support = position.add(offsetX, 0, offsetZ);
                world.setBlockState(support, source, 2);
                for (int height = 1; height <= 4; height++) {
                    world.setBlockToAir(support.up(height));
                }
            }
        }
        world.setBlockState(position.north().up(), source, 2);
    }
}
