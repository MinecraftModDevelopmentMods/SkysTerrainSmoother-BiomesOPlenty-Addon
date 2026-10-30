package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

public final class SoilRecipes {
    private SoilRecipes() {
    }

    public static void register(IForgeRegistry<IRecipe> registry) {
        net.minecraftforge.oredict.RecipeSorter.register("skysterrainsmootherbop:soil_recombination",
                SoilRecombinationRecipe.class, net.minecraftforge.oredict.RecipeSorter.Category.SHAPED,
                "after:minecraft:shaped");

        for (boolean grass : new boolean[] { false, true }) {
            for (int material = 0; material < (grass ? 8 : 6); material++) {
                net.minecraft.block.state.IBlockState full = NativeSoils.full(grass, material);
                ItemStack block = new ItemStack(full.getBlock(), 1, full.getBlock().getMetaFromState(full));
                ItemStack slab = SoilPieces.stack(SoilPieces.state(grass, material, SoilPieces.Shape.SLAB, 1), 1);
                ItemStack step = SoilPieces.stack(SoilPieces.state(grass, material, SoilPieces.Shape.STEP, 4), 1);
                ItemStack corners = SoilPieces.stack(SoilPieces.state(grass, material, SoilPieces.Shape.CORNER, 4), 4);
                String materialName = (grass ? "grass/" : "dirt/")
                        + (grass ? NativeSoils.GRASS_NAMES[material] : NativeSoils.DIRT_NAMES[material]);
                ItemStack slabs = slab.copy();

                slabs.setCount(6);
                register(registry, materialName + "/slab", slabs, block, "XXX");
                register(registry, materialName + "/full_block", block, slab, "XX");

                ItemStack steps = step.copy();

                steps.setCount(4);
                register(registry, materialName + "/step", steps, slab, "X ", " X");
                register(registry, materialName + "/corner", corners, step, "XX");
                // Vertical pairs leave the side-by-side cutting recipes available.
                registry.register(new SoilRecombinationRecipe(
                        SoilPieces.state(grass, material, SoilPieces.Shape.CORNER, 0), step)
                        .setRegistryName(SkysTerrainSmootherBop.ID, materialName + "/step_from_corners"));
                registry.register(new SoilRecombinationRecipe(
                        SoilPieces.state(grass, material, SoilPieces.Shape.STEP, 0), slab)
                        .setRegistryName(SkysTerrainSmootherBop.ID, materialName + "/slab_from_steps"));
            }
        }
    }

    private static void register(IForgeRegistry<IRecipe> registry, String name, ItemStack output, ItemStack input,
            String... rows) {
        int width = rows[0].length();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * rows.length, Ingredient.EMPTY);

        for (int blockY = 0; blockY < rows.length; blockY++) {
            for (int blockX = 0; blockX < width; blockX++) {
                if (rows[blockY].charAt(blockX) == 'X') {
                    ingredients.set(blockY * width + blockX, Ingredient.fromStacks(input));
                }
            }
        }

        registry.register(new ShapedRecipes(SkysTerrainSmootherBop.ID + "/" + name.substring(name.lastIndexOf('/') + 1), width, rows.length, ingredients, output)
                .setRegistryName(new ResourceLocation(SkysTerrainSmootherBop.ID, name)));
    }
}
