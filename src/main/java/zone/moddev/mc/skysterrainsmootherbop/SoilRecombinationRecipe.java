package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.world.World;

/** Rebuilds matching soil pieces while accepting their saved placement orientations. */
public final class SoilRecombinationRecipe extends ShapedRecipes {
    private final SoilPieceBlock inputBlock;
    private final ItemStack ingredient;

    public SoilRecombinationRecipe(IBlockState input, ItemStack output) {
        super(1, 2, new ItemStack[] { SoilPieces.stack(input, 1), SoilPieces.stack(input, 1) }, output.copy());
        inputBlock = (SoilPieceBlock) input.getBlock();
        ingredient = SoilPieces.stack(input, 1);
    }

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        int firstSlot = -1;
        int secondSlot = -1;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack stack = grid.getStackInSlot(slot);
            if (stack == null) {
                continue;
            }
            if (!sameMaterial(stack) || secondSlot != -1) {
                return false;
            }
            if (firstSlot == -1) {
                firstSlot = slot;
            } else {
                secondSlot = slot;
            }
        }

        return firstSlot >= 0 && secondSlot == firstSlot + grid.getWidth();
    }

    private boolean sameMaterial(ItemStack stack) {
        int metadata = stack.getItemDamage();
        return stack.stackSize > 0 && stack.getItem() == ingredient.getItem() && metadata >= 0 && metadata < 16
                && inputBlock.canonical(inputBlock.getStateFromMeta(metadata)) == ingredient.getItemDamage();
    }

    @Override
    public ItemStack[] getRemainingItems(InventoryCrafting grid) {
        return new ItemStack[grid.getSizeInventory()];
    }
}
