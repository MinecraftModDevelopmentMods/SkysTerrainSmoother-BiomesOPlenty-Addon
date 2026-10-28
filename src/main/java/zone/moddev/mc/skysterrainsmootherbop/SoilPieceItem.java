package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public final class SoilPieceItem extends ItemBlock {
    private final SoilPieceBlock piece;
    private final SoilPathBlock path;
    private final SoilPieces.Shape shape;

    public SoilPieceItem(SoilPieceBlock block) {
        super(block);
        piece = block;
        path = null;
        shape = block.shape;
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    public SoilPieceItem(SoilPathBlock block) {
        super(block);
        piece = null;
        path = block;
        shape = block.shape;
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    private int material(IBlockState state) {
        return path == null ? piece.material(state) : path.material(state);
    }

    private IBlockState full(IBlockState state) {
        return path == null ? piece.full(state) : path.full(state);
    }

    private IBlockState state(int material, SoilPieces.Shape shape, int orientation) {
        return path == null ? SoilPieces.state(piece.grass, material, shape, orientation)
                : SoilPaths.state(material, shape, orientation);
    }

    private boolean matches(IBlockState other, int material) {
        if (path != null) {
            return other.getBlock() instanceof SoilPathBlock
                    && ((SoilPathBlock) other.getBlock()).material(other) == material;
        }
        return other.getBlock() instanceof SoilPieceBlock
                && ((SoilPieceBlock) other.getBlock()).grass == piece.grass
                && ((SoilPieceBlock) other.getBlock()).material(other) == material;
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player,
            ItemStack stack) {
        return player.canPlayerEdit(pos, side, stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        IBlockState full = full(block.getStateFromMeta(stack.getItemDamage()));
        String name = new ItemStack(full.getBlock(), 1, full.getBlock().getMetaFromState(full)).getDisplayName();

        return I18n.translateToLocalFormatted(
                "pieces.skysterrainsmootherbop." + shape.name().toLowerCase(java.util.Locale.ROOT), name);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing face, float hitX, float hitY, float hitZ) {
        if (stack == null || stack.stackSize <= 0) {
            return EnumActionResult.FAIL;
        }

        int material = material(block.getStateFromMeta(stack.getItemDamage()));
        IBlockState existingState = world.getBlockState(pos);

        if (!existingState.getBlock().isReplaceable(world, pos)) {
            if (combine(stack, player, world, pos, existingState, material, face, hitX, hitY, hitZ)) {
                return EnumActionResult.SUCCESS;
            }

            pos = pos.offset(face);
            existingState = world.getBlockState(pos);
            hitX -= face.getFrontOffsetX();
            hitY -= face.getFrontOffsetY();
            hitZ -= face.getFrontOffsetZ();
        }
        if (!player.canPlayerEdit(pos, face, stack)) {
            return EnumActionResult.FAIL;
        }
        if (!existingState.getBlock().isReplaceable(world, pos)) {
            return combine(stack, player, world, pos, existingState, material, face, hitX, hitY, hitZ)
                    ? EnumActionResult.SUCCESS
                    : EnumActionResult.FAIL;
        }

        boolean isTopHalf = face == EnumFacing.DOWN || face.getAxis() != EnumFacing.Axis.Y && hitY > .5;
        int orientation = shape == SoilPieces.Shape.SLAB ? (isTopHalf ? 0 : 1)
                : (shape == SoilPieces.Shape.STEP ? edge(hitX, hitZ) : corner(hitX, hitZ)) + (isTopHalf ? 0 : 4);
        IBlockState target = state(material, shape, orientation);

        if (!world.canBlockBePlaced(block, pos, true, face, player, stack)
                || !world.checkNoEntityCollision(target.getCollisionBoundingBox(world, pos).offset(pos))) {
            return EnumActionResult.FAIL;
        }
        if (!world.setBlockState(pos, target, 3)) {
            return EnumActionResult.FAIL;
        }

        finish(stack, player, world, pos);
        return EnumActionResult.SUCCESS;
    }

    private boolean combine(ItemStack stack, EntityPlayer player, World world, BlockPos pos, IBlockState existingState,
            int material, EnumFacing face, float hitX, float hitY, float hitZ) {
        if (!matches(existingState, material) || !player.canPlayerEdit(pos, face, stack)) {
            return false;
        }

        hitX += face.getFrontOffsetX() * .001f;
        hitY += face.getFrontOffsetY() * .001f;
        hitZ += face.getFrontOffsetZ() * .001f;

        boolean isTopHalf = hitY >= .5;
        int orientation = shape == SoilPieces.Shape.SLAB ? (isTopHalf ? 0 : 1)
                : (shape == SoilPieces.Shape.STEP ? edge(hitX, hitZ) : corner(hitX, hitZ)) + (isTopHalf ? 0 : 4);
        int added = SoilPieceBlock.mask(shape, orientation);
        int before = existingState.getBlock() instanceof SoilPieceBlock
                ? ((SoilPieceBlock) existingState.getBlock()).mask(existingState)
                : SoilPieceBlock.mask(((SoilPathBlock) existingState.getBlock()).shape,
                        ((SoilPathBlock) existingState.getBlock()).orientation(existingState));

        if ((added & before) != 0) {
            return false;
        }

        int union = added | before;
        IBlockState target = null;

        if (union == 255) {
            target = path == null ? NativeSoils.full(piece.grass, material) : NativeSoils.path(material);
        } else {
            for (SoilPieces.Shape shape : SoilPieces.Shape.values()) {
                for (int candidateOrientation = 0; candidateOrientation < shape.states; candidateOrientation++) {
                    if (SoilPieceBlock.mask(shape, candidateOrientation) == union) {
                        target = state(material, shape, candidateOrientation);
                    }
                }
            }
        }
        if (target == null || !world.checkNoEntityCollision(target.getCollisionBoundingBox(world, pos).offset(pos))
                || !world.setBlockState(pos, target, 3)) {
            return false;
        }

        finish(stack, player, world, pos);
        return true;
    }

    private void finish(ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
        world.playSound(player, pos, block.getSoundType().getPlaceSound(), SoundCategory.BLOCKS, 1, .8f);

        if (!player.capabilities.isCreativeMode) {
            --stack.stackSize;
        }
    }

    private static int corner(float hitX, float hitZ) {
        return hitZ < .5 ? (hitX < .5 ? 0 : 1) : (hitX < .5 ? 3 : 2);
    }

    private static int edge(float hitX, float hitZ) {
        float[] distance = { hitZ, 1 - hitX, 1 - hitZ, hitX };
        int best = 0;

        for (int edgeIndex = 1; edgeIndex < 4; edgeIndex++) {
            if (distance[edgeIndex] < distance[best]) {
                best = edgeIndex;
            }
        }

        return best;
    }
}
