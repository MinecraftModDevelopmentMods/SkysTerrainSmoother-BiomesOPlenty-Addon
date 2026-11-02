package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;

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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META, SnowyDirtBlock.SNOWY, LOWERED_SUPPORT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos position,
            CollisionContext context) {
        // Forge bakes every possible carrier state, including reserved slots.
        // Saved reserved values are rejected by the recovery bridge before loading.
        if (!palette.valid(state.getValue(META))) {
            return Shapes.empty();
        }
        BlockState target = palette.resolve(state.getValue(META));
        return target.getShape(world, position, context);
    }
}
