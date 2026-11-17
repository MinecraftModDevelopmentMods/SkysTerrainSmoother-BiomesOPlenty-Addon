package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import com.mojang.serialization.MapCodec;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Retains the old coarse-soil slab without turning it into spreading dirt. */
public final class CoarseDirtSlab extends SlabBlock {
    private static final MapCodec<CoarseDirtSlab> CODEC = simpleCodec(CoarseDirtSlab::new);
    public static final String BLOCK_ID = SkysTerrainSmootherBop.ID + ":dirt_slab_00";
    // Forge creates block holders only while the block registry is open.
    public static CoarseDirtSlab BLOCK;
    static Item item;

    CoarseDirtSlab() {
        this(Properties.of().mapColor(MapColor.DIRT).strength(.5f).sound(SoundType.GRAVEL)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.parse(BLOCK_ID)))
                .noOcclusion().randomTicks());
    }

    private CoarseDirtSlab(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.SNOWY, false));
    }

    @Override
    public MapCodec<CoarseDirtSlab> codec() {
        return CODEC;
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action,
            boolean simulate) {
        if (action != ToolActions.SHOVEL_FLATTEN || !context.getItemInHand().canPerformAction(action)
                || state.getValue(WATERLOGGED)) {
            return super.getToolModifiedState(state, context, action, simulate);
        }
        return state.getValue(TYPE) == SlabType.DOUBLE
                ? Blocks.DIRT_PATH.defaultBlockState()
                : ModBlocks.PATH_SLAB.get().defaultBlockState()
                        .setValue(TYPE, state.getValue(TYPE)).setValue(WATERLOGGED, false);
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
        builder.add(BlockStateProperties.SNOWY);
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
        return state.setValue(BlockStateProperties.SNOWY, snowy(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks,
            BlockPos position, Direction facing, BlockPos neighbourPosition, BlockState neighbour,
            RandomSource random) {
        return super.updateShape(state, world, ticks, position, facing, neighbourPosition, neighbour, random)
                .setValue(BlockStateProperties.SNOWY, snowy(world, position));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos position, RandomSource random) {
        tick(state, world, position, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos position, RandomSource random) {
        if (!world.isClientSide()) {
            BlockState updated = state.setValue(BlockStateProperties.SNOWY, snowy(world, position));
            if (updated != state) {
                world.setBlock(position, updated, 2 | 16);
            }
        }
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos position, Direction face,
            net.minecraftforge.common.IPlantable plant) {
        BlockState full = Blocks.COARSE_DIRT.defaultBlockState();
        return !state.getValue(WATERLOGGED) && state.getValue(TYPE) == SlabType.TOP && face == Direction.UP
                && full.getBlock().canSustainPlant(full, world, position, face, plant);
    }

    private static boolean snowy(BlockGetter world, BlockPos position) {
        if (readState(world, position.above()).is(net.minecraft.tags.BlockTags.SNOW)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = position.relative(direction);
            BlockState state = readState(world, neighbour);
            if (state.is(net.minecraft.tags.BlockTags.SNOW) || state.isCollisionShapeFullBlock(world, neighbour)
                    && readState(world, neighbour.above()).is(net.minecraft.tags.BlockTags.SNOW)) {
                return true;
            }
        }
        return world instanceof LevelReader reader
                && reader.getBiome(position).value().coldEnoughToSnow(position, reader.getSeaLevel())
                && reader.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        position.getX(), position.getZ()) <= position.getY() + 1
                && reader.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, position.above()) < 10;
    }

    private static BlockState readState(BlockGetter world, BlockPos position) {
        if (world instanceof LevelReader reader && !reader.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
            return Blocks.AIR.defaultBlockState();
        }
        return world.getBlockState(position);
    }
}
