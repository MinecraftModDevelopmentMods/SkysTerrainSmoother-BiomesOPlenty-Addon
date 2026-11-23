package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

/** Exercises the four retained recipes against the complete live recipe manager. */
final class CoarseRecipeChecks {
    static void run(ServerLevel world) {
        String[] inputs = {"minecraft:coarse_dirt", "skysterrainsmootherbop:coarse_dirt_slab",
                "skysterrainsmootherbop:coarse_dirt_slab", "skysbuildingpieces:dirt_coarse_horizontal_step"};
        String[] outputs = {"skysterrainsmootherbop:coarse_dirt_slab", "minecraft:coarse_dirt",
                "skysbuildingpieces:dirt_coarse_horizontal_step", "skysterrainsmootherbop:coarse_dirt_slab"};
        int[][] slots = {{0, 1, 2}, {0, 1}, {0, 3}, {0, 2}};
        int[] counts = {6, 1, 4, 1};
        for (int index = 0; index < inputs.length; index++) {
            var grid = RecombinationChecks.grid(index == 0 ? 3 : 2);
            Item input = ForgeRegistries.ITEMS.getValue(new ResourceLocation(inputs[index]));
            for (int slot : slots[index]) grid.setItem(slot, new ItemStack(input));
            var matches = world.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, grid, world);
            RuntimeProbe.require(matches.size() == 1, "Coarse recipe has one distinct match: " + inputs[index]);
            ItemStack result = matches.get(0).assemble(grid, world.registryAccess());
            RuntimeProbe.require(ForgeRegistries.ITEMS.getKey(result.getItem()).toString().equals(outputs[index])
                    && result.getCount() == counts[index], "Coarse cutting and combining preserves material volume");
        }
        Map<String, ResourceLocation> groups = new HashMap<>();
        for (var recipe : world.getRecipeManager().getRecipes()) {
            if (!recipe.getId().getNamespace().startsWith("skys") || recipe.getGroup().isEmpty()) continue;
            ResourceLocation output = ForgeRegistries.ITEMS.getKey(recipe.getResultItem(world.registryAccess()).getItem());
            ResourceLocation previous = groups.putIfAbsent(recipe.getGroup(), output);
            RuntimeProbe.require(previous == null || previous.equals(output),
                    "Different recipe-book outputs have separate groups: " + recipe.getId());
        }
        zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info(
                "TERRAIN_BOP_COARSE_RECIPES_PASS recipes=4 unique_matches=true groups=" + groups.size());
    }

    private CoarseRecipeChecks() {}
}
