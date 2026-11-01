package zone.moddev.mc.skysterrainsmootherbop;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/** Retains the old coarse-soil slab without turning it into spreading dirt. */
public final class CoarseDirtSlab extends LegacySlabBlock {
    public static final String BLOCK_ID = SkysTerrainSmootherBop.ID + ":dirt_slab_00";
    public static final CoarseDirtSlab BLOCK = new CoarseDirtSlab();
    static Item item;

    private CoarseDirtSlab() {
        super(Material.DIRT, SoundType.GRAVEL, .5f, true);
        setRegistryName(BLOCK_ID);
        registerDefaultState(defaultBlockState().setValue(SnowyDirtBlock.SNOWY, false));
    }

    @Override
    public Item asItem() {
        // Registry reloads can otherwise select the old palette's hidden recovery item.
        // Placement, picking and drops must keep using the playable coarse-dirt slab.
        return item == null ? super.asItem() : item;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SnowyDirtBlock.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        if (state.getValue(TYPE) == SlabType.DOUBLE) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        return state.setValue(SnowyDirtBlock.SNOWY, snowy(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState neighbour,
            IWorld world, BlockPos position, BlockPos neighbourPosition) {
        return super.updateShape(state, facing, neighbour, world, position, neighbourPosition)
                .setValue(SnowyDirtBlock.SNOWY, snowy(world, position));
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos position, Random random) {
        if (!world.isClientSide) {
            BlockState updated = state.setValue(SnowyDirtBlock.SNOWY, snowy(world, position));
            if (updated != state) {
                world.setBlock(position, updated, 2 | 16);
            }
        }
    }

    @Override
    public boolean canSustainPlant(BlockState state, IBlockReader world, BlockPos position, Direction face,
            net.minecraftforge.common.IPlantable plant) {
        BlockState full = Blocks.COARSE_DIRT.defaultBlockState();
        return state.getValue(TYPE) == SlabType.TOP && face == Direction.UP
                && full.getBlock().canSustainPlant(full, world, position, face, plant);
    }

    private static boolean snowy(IBlockReader world, BlockPos position) {
        for (Direction direction : new Direction[] {
                Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            BlockPos neighbour = position.relative(direction);
            Block block;
            if (world instanceof ServerWorld) {
                Chunk chunk = ((ServerWorld) world).getChunkSource()
                        .getChunkNow(neighbour.getX() >> 4, neighbour.getZ() >> 4);
                if (chunk == null) {
                    continue;
                }
                block = chunk.getBlockState(neighbour).getBlock();
            } else {
                if (world instanceof IWorldReader
                        && !((IWorldReader) world).hasChunk(neighbour.getX() >> 4, neighbour.getZ() >> 4)) {
                    continue;
                }
                block = world.getBlockState(neighbour).getBlock();
            }
            if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
                return true;
            }
        }
        return false;
    }
}
