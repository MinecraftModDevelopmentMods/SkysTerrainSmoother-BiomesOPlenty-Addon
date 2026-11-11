package zone.moddev.mc.skysterrainsmootherbop;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.TriState;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Retains the old coarse-soil slab without turning it into spreading dirt. */
public final class CoarseDirtSlab extends SlabBlock {
    private static final MapCodec<CoarseDirtSlab> CODEC = simpleCodec(CoarseDirtSlab::new);

    public static final String BLOCK_ID = SkysTerrainSmootherBop.ID + ":dirt_slab_00";
    // Create the holder while the block registry is open.
    public static CoarseDirtSlab BLOCK;
    static Item item;

    CoarseDirtSlab() {
        this(Properties.of().mapColor(MapColor.DIRT).strength(.5f).sound(SoundType.GRAVEL)
                .noOcclusion().randomTicks());
    }

    private CoarseDirtSlab(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SnowyDirtBlock.SNOWY, false));
    }

    @Override
    public MapCodec<CoarseDirtSlab> codec() {
        return CODEC;
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility action,
            boolean simulate) {
        if (action != ItemAbilities.SHOVEL_FLATTEN || !context.getItemInHand().canPerformAction(action)
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
                .setValue(SnowyDirtBlock.SNOWY, snowy(world, position, facing, neighbour, neighbourPosition));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos position, RandomSource random) {
        tick(state, world, position, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos position, RandomSource random) {
        if (!world.isClientSide) {
            BlockState updated = state.setValue(SnowyDirtBlock.SNOWY, snowy(world, position));
            if (updated != state) {
                world.setBlock(position, updated, 2 | 16);
            }
        }
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter world, BlockPos position, Direction face,
            BlockState plant) {
        if (state.getValue(TYPE) != SlabType.TOP || state.getValue(WATERLOGGED) || face != Direction.UP) {
            return TriState.FALSE;
        }
        BlockState coarseDirt = Blocks.COARSE_DIRT.defaultBlockState();
        TriState support = coarseDirt.canSustainPlant(world, position, face, plant);
        if (!support.isDefault() || !(world instanceof LevelReader level)) {
            return support;
        }
        // Let the plant check the original soil without replacing the slab in the world.
        LevelReader soil = new PlantSoilView(level, position, coarseDirt);
        return plant.canSurvive(soil, position.above()) ? TriState.TRUE : TriState.FALSE;
    }

    private static boolean snowy(LevelAccessor world, BlockPos position) {
        return snowy(world, position, Direction.UP, world.getBlockState(position.above()), position.above());
    }

    private static boolean snowy(LevelAccessor world, BlockPos position, Direction side, BlockState neighbour,
            BlockPos neighbourPosition) {
        // Reuse dirt slabs' snow calculation without running their grass-spreading tick on coarse soil.
        BlockState dirt = ModBlocks.DIRT_SLAB.get().defaultBlockState();
        return dirt.updateShape(side, neighbour, world, position, neighbourPosition)
                .getValue(SnowyDirtBlock.SNOWY);
    }
}
