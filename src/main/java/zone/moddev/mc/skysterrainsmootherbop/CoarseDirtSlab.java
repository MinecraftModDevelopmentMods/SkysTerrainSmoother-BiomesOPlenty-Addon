package zone.moddev.mc.skysterrainsmootherbop;

import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/** Retains the old coarse-soil slab without turning it into spreading dirt. */
public final class CoarseDirtSlab extends LegacySlabBlock {
    public static final String BLOCK_ID = SkysTerrainSmootherBop.ID + ":dirt_slab_00";
    // Forge 40 creates block holders only while the block registry is open.
    public static CoarseDirtSlab BLOCK;
    static Item item;

    CoarseDirtSlab() {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SnowyDirtBlock.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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
            LevelAccessor world, BlockPos position, BlockPos neighbourPosition) {
        return super.updateShape(state, facing, neighbour, world, position, neighbourPosition)
                .setValue(SnowyDirtBlock.SNOWY, snowy(world, position));
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos position, Random random) {
        if (!world.isClientSide) {
            BlockState updated = state.setValue(SnowyDirtBlock.SNOWY, snowy(world, position));
            if (updated != state) {
                world.setBlock(position, updated, 2 | 16);
            }
        }
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos position, Direction face,
            net.minecraftforge.common.IPlantable plant) {
        BlockState full = Blocks.COARSE_DIRT.defaultBlockState();
        return state.getValue(TYPE) == SlabType.TOP && face == Direction.UP
                && full.getBlock().canSustainPlant(full, world, position, face, plant);
    }

    private static boolean snowy(BlockGetter world, BlockPos position) {
        for (Direction direction : new Direction[] {
                Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            BlockPos neighbour = position.relative(direction);
            Block block;
            if (world instanceof ServerLevel) {
                LevelChunk chunk = ((ServerLevel) world).getChunkSource()
                        .getChunkNow(neighbour.getX() >> 4, neighbour.getZ() >> 4);
                if (chunk == null) {
                    continue;
                }
                block = chunk.getBlockState(neighbour).getBlock();
            } else {
                if (world instanceof LevelReader
                        && !((LevelReader) world).hasChunk(neighbour.getX() >> 4, neighbour.getZ() >> 4)) {
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
