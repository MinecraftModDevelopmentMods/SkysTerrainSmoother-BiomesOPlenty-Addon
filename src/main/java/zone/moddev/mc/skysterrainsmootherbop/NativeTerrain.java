package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraftforge.registries.ForgeRegistries;
import java.util.Collections;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;
import zone.moddev.mc.skysbuildingpieces.catalogue.Catalogue;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysterrainsmoother.api.TerrainMaterial;
import zone.moddev.mc.skysterrainsmoother.api.TerrainSmoothingApi;

/** Ordinary BOP ground now uses vanilla states; only retired natural rocks need extra mappings. */
final class NativeTerrain {
    private NativeTerrain() {
    }

    static void register() {
        Direction[] directions = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
        ResourceLocation coarseId = new ResourceLocation("minecraft:coarse_dirt");
        if (!TerrainSmoothingApi.hasMaterial(coarseId)) {
            BlockState[] steps = new BlockState[4];
            BlockState[] corners = new BlockState[4];
            for (int direction = 0; direction < directions.length; direction++) {
                steps[direction] = BuildingPiecesApi.bottomPiece("minecraft:dirt_coarse",
                        BuildingPiecesApi.TerrainShape.STEP, directions[direction]);
                corners[direction] = BuildingPiecesApi.bottomPiece("minecraft:dirt_coarse",
                        BuildingPiecesApi.TerrainShape.CORNER, directions[direction]);
            }
            TerrainSmoothingApi.registerMaterial(new TerrainMaterial(coarseId,
                    Collections.singleton(net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState()),
                    CoarseDirtSlab.BLOCK.defaultBlockState(), steps, corners, false, null));
        }
        for (String rock : new String[] { "crag_rock", "limestone", "shale", "siltstone" }) {
            String material = "biomesoplenty:" + rock;
            BlockState full = Pieces.nativeState(Catalogue.INSTANCE.materials.get(material), "full");
            ResourceLocation resolved = ForgeRegistries.BLOCKS.getKey(full.getBlock());
            // The core owns Mineralogy, including its disabled or unavailable slab forms.
            if (resolved.getNamespace().equals("mineralogy") || TerrainSmoothingApi.hasMaterial(resolved)) {
                continue;
            }
            BlockState[] steps = new BlockState[4];
            BlockState[] corners = new BlockState[4];
            for (int direction = 0; direction < directions.length; direction++) {
                steps[direction] = BuildingPiecesApi.bottomPiece(material,
                        BuildingPiecesApi.TerrainShape.STEP, directions[direction]);
                corners[direction] = BuildingPiecesApi.bottomPiece(material,
                        BuildingPiecesApi.TerrainShape.CORNER, directions[direction]);
            }
            TerrainSmoothingApi.registerMaterial(new TerrainMaterial(new ResourceLocation(material),
                    Collections.singleton(full), BuildingPiecesApi.bottomPiece(material,
                            BuildingPiecesApi.TerrainShape.SLAB, Direction.NORTH), steps, corners, false, null));
        }
    }
}
