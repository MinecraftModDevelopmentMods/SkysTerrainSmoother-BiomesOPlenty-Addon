package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

/** Hidden registry carriers; the early loader replaces their saved states. */
final class LegacySoilBlock extends Block {
    static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);
    static final BooleanProperty LOWERED_SUPPORT = BooleanProperty.create("lowered_support");
    private final LegacySoils.Palette palette;

    LegacySoilBlock(LegacySoils.Palette palette) {
        super(Block.Properties.of(Material.DIRT).strength(.5f));
        this.palette = palette;
        palette.holder = this;
        setRegistryName(palette.id);
        registerDefaultState(stateDefinition.any().setValue(META, 0)
                .setValue(SnowyDirtBlock.SNOWY, false).setValue(LOWERED_SUPPORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(META, SnowyDirtBlock.SNOWY, LOWERED_SUPPORT);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos position,
            ISelectionContext context) {
        // Forge bakes every possible carrier state, including reserved slots.
        // Saved reserved values are rejected by the recovery bridge before loading.
        if (!palette.valid(state.getValue(META))) {
            return VoxelShapes.empty();
        }
        BlockState target = palette.resolve(state.getValue(META));
        return target.getShape(world, position, context);
    }
}
