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
        super(Block.Properties.create(Material.EARTH).hardnessAndResistance(.5f));
        this.palette = palette;
        palette.holder = this;
        setRegistryName(palette.id);
        setDefaultState(stateContainer.getBaseState().with(META, 0)
                .with(SnowyDirtBlock.SNOWY, false).with(LOWERED_SUPPORT, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(META, SnowyDirtBlock.SNOWY, LOWERED_SUPPORT);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos position,
            ISelectionContext context) {
        // Forge bakes every possible carrier state, including reserved slots.
        // Saved reserved values are rejected by the recovery bridge before loading.
        if (!palette.valid(state.get(META))) {
            return VoxelShapes.empty();
        }
        BlockState target = palette.resolve(state.get(META));
        return target.getShape(world, position, context);
    }
}
