package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.util.Random;
import java.io.File;
import java.io.IOException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.skysterrainsmootherbop.SoilPathBlock;
import zone.moddev.mc.skysterrainsmootherbop.SoilPaths;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieceBlock;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieces;

/** Covers every BOP soil slot and orientation through Minecraft's real snow rule. */
final class SnowSettlementChecks {
    static void savedWorld(WorldServer world) throws IOException {
        File marker = new File(world.getSaveHandler().getWorldDirectory(), "snow-support-repaired.pass");
        boolean reload = marker.isFile();
        int repaired = 0;
        int supported = 0;
        int snowCaps = 0;
        for (int blockX = -1276; blockX < -1258; blockX++) {
            for (int blockZ = -3044; blockZ < -3026; blockZ++) {
                world.getChunk(new BlockPos(blockX, 132, blockZ));
                for (int blockY = 132; blockY < 144; blockY++) {
                    BlockPos snowPosition = new BlockPos(blockX, blockY, blockZ);
                    IBlockState snow = world.getBlockState(snowPosition);
                    IBlockState support = world.getBlockState(snowPosition.down());
                    Block block = support.getBlock();
                    if (snow.getBlock() != Blocks.SNOW_LAYER
                            || !(block instanceof SoilPieceBlock || block instanceof SoilPathBlock
                                    || block instanceof zone.moddev.mc.skysbuildingpieces.content.PieceBlock)) {
                        continue;
                    }
                    if (Blocks.SNOW_LAYER.canPlaceBlockAt(world, snowPosition)) {
                        supported++;
                        continue;
                    }
                    require(!reload, "unsupported snow returned after reload: " + snowPosition);
                    block.neighborChanged(support, world, snowPosition.down(), Blocks.SNOW_LAYER, snowPosition);
                    require(world.isAirBlock(snowPosition), "ordinary update clears the saved floating layer");
                    require(world.getBlockState(snowPosition.down()).equals(support),
                            "saved piece identity and metadata are unchanged");
                    if (block instanceof SoilPieceBlock && block.getActualState(support, world, snowPosition.down())
                            .getValue(BlockGrass.SNOWY)) {
                        snowCaps++;
                    }
                    repaired++;
                }
            }
        }
        require(reload || repaired > 0, "the disposable copy reproduces the reported snow");
        require(reload || snowCaps > 0, "calculated snow caps remain after the floating layers are removed");
        if (!reload) {
            java.nio.file.Files.write(marker.toPath(), ("repaired=" + repaired + "\n").getBytes("UTF-8"));
        }
        System.out.println("BOP_SAVED_SNOW_REPAIR_PASS reload=" + reload + " repaired=" + repaired
                + " supported=" + supported + " snow_caps=" + snowCaps + " piece_metadata=unchanged");
    }

    static int run(WorldServer world) {
        BlockPos position = new BlockPos(1460, 220, 1460);
        for (int chunkX = -1; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) {
                world.getChunk((position.getX() >> 4) + chunkX, (position.getZ() >> 4) + chunkZ);
            }
        }
        world.setBlockState(position.down(), Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(position, ((Block) SoilPieces.BLOCKS.get(0)).getStateFromMeta(1), 2);
        require(!Blocks.SNOW_LAYER.canPlaceBlockAt(world, position.up()),
                "snow must not hover over a bottom BOP slab");
        int cases = 0;
        for (SoilPieceBlock block : SoilPieces.BLOCKS) {
            Block nativeMethods = block;
            for (int slot = 0; slot < block.count; slot++) {
                for (int orientation = 0; orientation < block.shape.states; orientation++) {
                    IBlockState state = nativeMethods.getStateFromMeta(slot * block.shape.states + orientation);
                    world.setBlockToAir(position.up());
                    world.setBlockState(position, state, 2);
                    boolean completeTop = block.shape == SoilPieces.Shape.SLAB && orientation == 0;
                    require(Blocks.SNOW_LAYER.canPlaceBlockAt(world, position.up()) == completeTop,
                            "snow needs a complete BOP top: " + state);
                    for (EnumFacing face : EnumFacing.values()) {
                        BlockFaceShape expected = nativeMethods.isSideSolid(state, world, position, face)
                                ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
                        require(state.getBlockFaceShape(world, position, face) == expected,
                                "BOP face geometry: " + state + "/" + face);
                    }
                    for (int layers : new int[] { 1, 8 }) {
                        world.setBlockState(position.up(),
                                Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, layers), 2);
                        nativeMethods.neighborChanged(state, world, position, Blocks.SNOW_LAYER, position.up());
                        require((world.getBlockState(position.up()).getBlock() == Blocks.SNOW_LAYER) == completeTop,
                                "saved snow repair: " + state);
                        require(world.getBlockState(position).equals(state), "BOP metadata survives snow repair");
                        world.setBlockToAir(position.up());
                    }
                    world.setBlockState(position.up(), Blocks.SNOW_LAYER.getDefaultState(), 2);
                    nativeMethods.updateTick(world, position, state, new Random(4815162342L));
                    require((world.getBlockState(position.up()).getBlock() == Blocks.SNOW_LAYER) == completeTop,
                            "soil ticks clear existing floating snow: " + state);
                    world.setBlockToAir(position.up());
                    cases++;
                }
            }
        }
        for (SoilPathBlock block : SoilPaths.BLOCKS) {
            Block nativeMethods = block;
            for (int slot = 0; slot < block.count; slot++) {
                for (int orientation = 0; orientation < block.shape.states; orientation++) {
                    IBlockState state = nativeMethods.getStateFromMeta(slot * block.shape.states + orientation);
                    world.setBlockState(position, state, 2);
                    require(!Blocks.SNOW_LAYER.canPlaceBlockAt(world, position.up()), "lowered paths reject snow");
                    world.setBlockState(position.up(), Blocks.SNOW_LAYER.getDefaultState(), 2);
                    nativeMethods.neighborChanged(state, world, position, Blocks.SNOW_LAYER, position.up());
                    require(world.isAirBlock(position.up()), "path updates clear unsupported snow");
                    cases++;
                }
            }
        }
        world.setBlockToAir(position);
        world.setBlockToAir(position.down());
        System.out.println("BOP_SNOW_SETTLEMENT_PASS states=" + cases
                + " native_rule=true metadata=true saved_snow=true");
        return cases;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private SnowSettlementChecks() {
    }
}
