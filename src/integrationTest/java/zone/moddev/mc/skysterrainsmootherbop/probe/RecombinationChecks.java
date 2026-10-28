package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.skysterrainsmootherbop.SoilPieces;

/** Exercises all soil palettes and orientations through Minecraft's recipe manager. */
public final class RecombinationChecks {
    public static int run(WorldServer world) {
        int cases = 0;
        for (boolean grass : new boolean[] { false, true }) {
            for (int material = 0; material < (grass ? 8 : 6); material++) {
                cases += pair(world, grass, material, SoilPieces.Shape.CORNER, SoilPieces.Shape.STEP);
                cases += pair(world, grass, material, SoilPieces.Shape.STEP, SoilPieces.Shape.SLAB);
            }
        }
        System.out.println("BOP_TERRAIN_RECOMBINATION_PASS cases=" + cases + " materials=14");
        return cases;
    }

    private static int pair(WorldServer world, boolean grass, int material, SoilPieces.Shape inputShape,
            SoilPieces.Shape outputShape) {
        ItemStack expected = SoilPieces.stack(SoilPieces.state(grass, material, outputShape, 0), 1);
        int cases = 0;
        for (int size : new int[] { 2, 3 }) {
            for (int column = 0; column < size; column++) {
                for (int row = 0; row < size - 1; row++) {
                    for (int orientation = 0; orientation < inputShape.states; orientation++) {
                        IBlockState state = SoilPieces.state(grass, material, inputShape, orientation);
                        ItemStack upper = new ItemStack(state.getBlock(), 17, state.getBlock().getMetaFromState(state));
                        IBlockState otherOrientation = SoilPieces.state(grass, material, inputShape,
                                (orientation + 1) % inputShape.states);
                        ItemStack lower = new ItemStack(otherOrientation.getBlock(), 23,
                                otherOrientation.getBlock().getMetaFromState(otherOrientation));
                        InventoryCrafting grid = grid(size);
                        int upperSlot = column + row * size;
                        grid.setInventorySlotContents(upperSlot, upper);
                        grid.setInventorySlotContents(upperSlot + size, lower);
                        String label = grass + "/" + material + "/" + inputShape + "/" + orientation;

                        require(ItemStack.areItemStacksEqual(expected,
                                CraftingManager.getInstance().findMatchingRecipe(grid, world)), "vertical pair " + label);
                        for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
                            if (recipe.matches(grid, world)) {
                                ItemStack actual = recipe.getCraftingResult(grid);
                                require(ItemStack.areItemStacksEqual(expected, actual), "no conflicting recipe " + label);
                                actual.stackSize = 9;
                                require(ItemStack.areItemStacksEqual(expected, recipe.getCraftingResult(grid)),
                                        "independent one-item result");
                                for (ItemStack remainder : recipe.getRemainingItems(grid)) {
                                    require(remainder == null, "no duplicated input remainders");
                                }
                            }
                        }
                        require(upper.stackSize == 17 && lower.stackSize == 23, "lookup does not mutate inputs");
                        cases++;
                    }
                }
            }
        }

        InventoryCrafting invalid = grid(2);
        ItemStack ingredient = SoilPieces.stack(SoilPieces.state(grass, material, inputShape, 0), 1);
        invalid.setInventorySlotContents(0, ingredient);
        invalid.setInventorySlotContents(2,
                SoilPieces.stack(SoilPieces.state(grass, (material + 1) % (grass ? 8 : 6), inputShape, 0), 1));
        require(CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null, "mixed soils rejected");
        invalid.setInventorySlotContents(2,
                SoilPieces.stack(SoilPieces.state(!grass, 0, inputShape, 0), 1));
        require(CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null, "grass and dirt cannot mix");
        invalid.setInventorySlotContents(2, new ItemStack(ingredient.getItem(), 1, 16));
        require(CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null, "invalid metadata rejected");
        invalid.setInventorySlotContents(2, ingredient.copy());
        invalid.setInventorySlotContents(1, ingredient.copy());
        require(CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null, "extra ingredient rejected");
        invalid.setInventorySlotContents(2, null);
        if (inputShape == SoilPieces.Shape.CORNER) {
            require(CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null,
                    "horizontal pair does not claim a cutting recipe");
        }
        invalid.setInventorySlotContents(1, null);
        require(CraftingManager.getInstance().findMatchingRecipe(invalid, world) == null, "one piece is insufficient");
        return cases;
    }

    private static InventoryCrafting grid(int size) {
        return new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, size, size);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private RecombinationChecks() {
    }
}
