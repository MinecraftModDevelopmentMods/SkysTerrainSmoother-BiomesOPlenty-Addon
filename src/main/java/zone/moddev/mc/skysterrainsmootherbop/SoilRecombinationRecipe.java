package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/** Rebuilds matching soil pieces while accepting their saved placement orientations. */
public final class SoilRecombinationRecipe extends ShapedRecipes {
    private final SoilPieceBlock inputBlock;
    private final ItemStack ingredient;

    public SoilRecombinationRecipe(IBlockState input, ItemStack output) {
        super(SkysTerrainSmootherBop.ID + "/recombine/" + ((SoilPieceBlock) input.getBlock()).shape.name(), 1, 2, NonNullList.from(Ingredient.EMPTY,
                ingredients(input), ingredients(input)),
                output.copy());
        inputBlock = (SoilPieceBlock) input.getBlock();
        ingredient = SoilPieces.stack(input, 1);
    }

    private static Ingredient ingredients(IBlockState state) {
        SoilPieceBlock block = (SoilPieceBlock) state.getBlock();
        ItemStack canonical = SoilPieces.stack(state, 1);
        java.util.List<ItemStack> variants = new java.util.ArrayList<>();
        variants.add(canonical.copy());
        for (int metadata = 0; metadata < 16; metadata++) {
            if (metadata != canonical.getMetadata()
                    && block.canonical(block.getStateFromMeta(metadata)) == canonical.getMetadata()) {
                ItemStack variant = canonical.copy();
                variant.setItemDamage(metadata);
                variants.add(variant);
            }
        }
        // Keep saved orientations in the matching and auto-fill lists, but show the normal item.
        return new Ingredient(variants.toArray(new ItemStack[variants.size()])) {
            private final ItemStack[] preview = {canonical.copy()};

            @Override
            public ItemStack[] getMatchingStacks() {
                return preview;
            }
        };
    }

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        int firstSlot = -1;
        int secondSlot = -1;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack stack = grid.getStackInSlot(slot);
            if (stack.isEmpty()) {
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
        return !stack.isEmpty() && stack.getItem() == ingredient.getItem() && metadata >= 0 && metadata < 16
                && inputBlock.canonical(inputBlock.getStateFromMeta(metadata)) == ingredient.getItemDamage();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting grid) {
        return NonNullList.withSize(grid.getSizeInventory(), ItemStack.EMPTY);
    }
}
