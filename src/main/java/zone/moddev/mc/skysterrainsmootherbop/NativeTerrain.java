package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraftforge.registries.ForgeRegistries;
import java.util.Collections;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;
import zone.moddev.mc.skysbuildingpieces.catalogue.Catalogue;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysterrainsmoother.api.TerrainMaterial;
import zone.moddev.mc.skysterrainsmoother.api.MaterialGroup;
import zone.moddev.mc.skysterrainsmoother.api.TerrainSmoothingApi;

/** Match BoP's natural ground and the surviving replacements for its older rocks. */
final class NativeTerrain {
    private static final Direction[] DIRECTIONS = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    private NativeTerrain() {
    }

    static void register() {
        BopSandContent.registerTerrain();
        addNative("origin_grass_block", MaterialGroup.SOIL, Blocks.DIRT.defaultBlockState());
        for (String colour : new String[] { "white", "orange", "black" }) {
            addNative(colour + "_sandstone", MaterialGroup.STONE, null);
        }
        for (String rock : new String[] { "crag_rock", "limestone", "shale", "siltstone" }) {
            String material = "biomesoplenty:" + rock;
            BlockState full = Pieces.nativeState(Catalogue.INSTANCE.materials.get(material), "full");
            ResourceLocation resolved = ForgeRegistries.BLOCKS.getKey(full.getBlock());
            // The core owns Mineralogy, including its disabled or unavailable slab forms.
            if (resolved.getNamespace().equals("mineralogy") || TerrainSmoothingApi.hasMaterial(resolved)) {
                continue;
            }
            add(material, full, MaterialGroup.STONE, null);
        }
    }

    private static void addNative(String name, MaterialGroup group, BlockState supportAfterPlacement) {
        String material = "biomesoplenty:" + name;
        add(material, Pieces.nativeState(Catalogue.INSTANCE.materials.get(material), "full"), group, supportAfterPlacement);
    }

    private static void add(String material, BlockState full, MaterialGroup group, BlockState supportAfterPlacement) {
        BlockState[] steps = new BlockState[4];
        BlockState[] corners = new BlockState[4];
        for (int direction = 0; direction < DIRECTIONS.length; direction++) {
            steps[direction] = BuildingPiecesApi.bottomPiece(material, BuildingPiecesApi.TerrainShape.STEP, DIRECTIONS[direction]);
            corners[direction] = BuildingPiecesApi.bottomPiece(material, BuildingPiecesApi.TerrainShape.CORNER, DIRECTIONS[direction]);
        }
        TerrainSmoothingApi.registerMaterial(new TerrainMaterial(ResourceLocation.parse(material), Collections.singleton(full),
                BuildingPiecesApi.bottomPiece(material, BuildingPiecesApi.TerrainShape.SLAB, Direction.NORTH),
                steps, corners, supportAfterPlacement != null, supportAfterPlacement, group));
    }
}
