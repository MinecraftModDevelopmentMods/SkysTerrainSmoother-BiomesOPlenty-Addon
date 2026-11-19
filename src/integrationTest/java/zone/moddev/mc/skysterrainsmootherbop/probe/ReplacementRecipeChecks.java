package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** Retired soil recipes can disappear only when every cut and rebuilding pattern remains available. */
final class ReplacementRecipeChecks {
    static int run(ServerLevel world) {
        RecipeUniquenessChecks.run(world);
        chain(world, "minecraft:coarse_dirt", "skysterrainsmootherbop:coarse_dirt_slab",
                "dirt_coarse_horizontal_step", "dirt_coarse_corner");
        chain(world, "minecraft:dirt", "skysgrassslabs:dirt_slab", "dirt_horizontal_step", "dirt_corner");
        chain(world, "minecraft:grass_block", "skysgrassslabs:grass_slab", "grass_step", "grass_corner");
        chain(world, "minecraft:netherrack", "skysbuildingpieces:netherrack_slab",
                "netherrack_horizontal_step", "netherrack_corner");
        chain(world, "minecraft:mossy_cobblestone", "minecraft:mossy_cobblestone_slab",
                "mossy_cobblestone_horizontal_step", "mossy_cobblestone_corner");
        zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info(
                "TERRAIN_BOP_REPLACEMENT_RECIPES_PASS patterns=30 unique_matches=true");
        return 30;
    }

    private static void chain(ServerLevel world, String fullId, String slabId, String stepPath, String cornerPath) {
        ItemStack full = item(fullId);
        ItemStack slab = item(slabId);
        ItemStack step = item("skysbuildingpieces:" + stepPath);
        ItemStack corner = item("skysbuildingpieces:" + cornerPath);
        craft(world, 3, 1, List.of(full, full, full), slab.copyWithCount(6));
        craft(world, 2, 2, List.of(slab, ItemStack.EMPTY, ItemStack.EMPTY, slab), step.copyWithCount(4));
        craft(world, 2, 1, List.of(step, step), corner.copyWithCount(4));
        craft(world, 1, 2, List.of(corner, corner), step);
        craft(world, 1, 2, List.of(step, step), slab);
        craft(world, 2, 1, List.of(slab, slab), full);
    }

    private static ItemStack item(String identity) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(identity));
        RuntimeProbe.require(item != null && item != net.minecraft.world.item.Items.AIR, "Recipe item is missing: " + identity);
        return new ItemStack(item);
    }

    private static void craft(ServerLevel world, int width, int height, List<ItemStack> ingredients, ItemStack expected) {
        CraftingInput input = CraftingInput.of(width, height, ingredients);
        var matches = world.getServer().getRecipeManager().getRecipes().stream().filter(holder -> holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe && recipe.matches(input, world)).toList();
        RuntimeProbe.require(matches.size() == 1,
                "Each replacement pattern has one matching recipe: " + ingredients + " matches=" + matches);
        RuntimeProbe.require(ItemStack.matches(expected, ((net.minecraft.world.item.crafting.CraftingRecipe) matches.getFirst().value()).assemble(input)),
                "Replacement crafting keeps its result and quantity: " + ingredients + " expected=" + expected);
    }

    private ReplacementRecipeChecks() {
    }
}
