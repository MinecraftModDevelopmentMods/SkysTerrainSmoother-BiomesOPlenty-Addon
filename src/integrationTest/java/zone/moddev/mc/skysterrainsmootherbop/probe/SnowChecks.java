package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.block.BlockGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieceBlock;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieces;

/** Every fixed soil slot keeps its orientation while its snow cap follows the surroundings. */
final class SnowChecks {
    static int run(WorldServer world) {
        BlockPos position = new BlockPos(3912, 220, 3912);
        byte[] biomes = world.getChunkFromBlockCoords(position).getBiomeArray();
        int index = (position.getZ() & 15) << 4 | position.getX() & 15;
        byte originalBiome = biomes[index];
        int states = 0;
        try {
            for (SoilPieceBlock block : SoilPieces.BLOCKS) {
                for (int slot = 0; slot < block.count; slot++) {
                    for (int orientation = 0; orientation < block.shape.states; orientation++) {
                        IBlockState piece = block.getStateFromMeta(slot * block.shape.states + orientation);
                        world.setBlockState(position.down(), block.full(piece), 2);
                        world.setBlockState(position, piece, 2);
                        world.setBlockState(position.up(3), Blocks.GLASS.getDefaultState(), 2);
                        for (EnumFacing direction : EnumFacing.HORIZONTALS) {
                            BlockPos higher = position.offset(direction);
                            world.setBlockState(higher, Blocks.STONE.getDefaultState(), 2);
                            world.setBlockState(higher.up(), Blocks.SNOW_LAYER.getDefaultState(), 2);
                            require(snowy(block, piece, world, position), "higher snowy terrace " + direction);
                            world.setBlockToAir(higher.up());
                            world.setBlockToAir(higher);
                            require(!snowy(block, piece, world, position), "removed snow clears the cap");
                        }
                        biomes[index] = (byte) Biome.getIdForBiome(Biomes.ICE_PLAINS);
                        world.setBlockToAir(position.up(3));
                        require(snowy(block, piece, world, position), "exposed cold soil gets a snow cap");
                        ChunkCache cache = new ChunkCache(world, position.add(-2, -2, -2), position.add(2, 4, 2), 0);
                        require(block.getActualState(piece, cache, position).getValue(BlockGrass.SNOWY),
                                "the render cache uses the correct world");
                        IBlockState support = world.getBlockState(position.down());
                        if (support.getBlock() instanceof BlockGrass) {
                            boolean touchesGround = (block.mask(piece) & 15) != 0;
                            require(support.getBlock().getActualState(support, world, position.down())
                                    .getValue(BlockGrass.SNOWY) == touchesGround,
                                    "native BOP grass follows a touching snowy piece");
                        }
                        world.setBlockState(position.up(3), Blocks.GLASS.getDefaultState(), 2);
                        require(!snowy(block, piece, world, position), "glass roof blocks weather snow");
                        biomes[index] = (byte) Biome.getIdForBiome(Biomes.PLAINS);
                        world.setBlockToAir(position.up(3));
                        require(!snowy(block, piece, world, position), "warm exposed soil remains green");
                        require(world.getBlockState(position).equals(piece), "saved metadata is unchanged");
                        states++;
                    }
                }
            }
        } finally {
            biomes[index] = originalBiome;
            world.setBlockToAir(position.up(3));
            world.setBlockToAir(position);
            world.setBlockToAir(position.down());
        }
        System.out.println("BOP_TERRAIN_SNOW_PASS states=" + states + " terraces=4 cache=true roof=true warm=true metadata=true");
        return states * 13;
    }

    private static boolean snowy(SoilPieceBlock block, IBlockState state, WorldServer world, BlockPos position) {
        return block.getActualState(state, world, position).getValue(BlockGrass.SNOWY);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private SnowChecks() {
    }
}
