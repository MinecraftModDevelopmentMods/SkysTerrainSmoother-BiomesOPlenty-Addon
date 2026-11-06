package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import zone.moddev.mc.skysbuildingpieces.catalogue.Shape;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysbuildingpieces.legacy.HistoricalFullBlocks;
import zone.moddev.mc.skysbuildingpieces.legacy.LegacyBridge;
import zone.moddev.mc.skysterrainsmootherbop.LegacySoils;

/** Exercises every assigned old palette value against the actual registered target pieces. */
final class BopRecoveryChecks {
    static int run(ServerLevel world) {
        int checked = 0;
        int states = 0;
        for (String rock : new String[] { "crag_rock", "limestone", "shale", "siltstone" }) {
            String material = "biomesoplenty:" + rock;
            BlockState full = Pieces.nativeState(
                    zone.moddev.mc.skysbuildingpieces.catalogue.Catalogue.INSTANCE.materials.get(material), "full");
            boolean mineralogyOwnsRock = ForgeRegistries.BLOCKS.getKey(full.getBlock()).getNamespace().equals("mineralogy");
            RuntimeProbe.require(zone.moddev.mc.skysterrainsmoother.api.TerrainSmoothingApi
                            .hasMaterial(net.minecraft.resources.Identifier.parse(material)) != mineralogyOwnsRock,
                    "BOP rock mappings defer to Mineralogy without bypassing its configuration");
            checked++;
        }
        for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
            for (int metadata = 0; metadata < 16; metadata++) {
                if (!palette.valid(metadata)) {
                    final int invalid = metadata;
                    boolean rejected = false;
                    try {
                        HistoricalFullBlocks.resolve(palette.id, invalid);
                    } catch (LegacyBridge.RecoveryAbort expected) {
                        rejected = true;
                    }
                    RuntimeProbe.require(rejected, "Unassigned palette slot stops recovery");
                    checked++;
                    continue;
                }
                states++;
                BlockState target = palette.resolve(metadata);
                BlockState recovered = HistoricalFullBlocks.resolve(palette.id, metadata);
                RuntimeProbe.require(target.equals(recovered),
                        "The numeric bridge resolves the same material and orientation: "
                                + palette.id + "/" + metadata + " actual=" + recovered);
                RuntimeProbe.require(!target.hasBlockEntity() && target.getFluidState().isEmpty(),
                        "Recovered pieces are dry and have no block entity");
                RuntimeProbe.require(target.getBlock() == zone.moddev.mc.skysterrainsmootherbop.CoarseDirtSlab.BLOCK
                                || !ForgeRegistries.BLOCKS.getKey(target.getBlock()).getNamespace().equals("skysterrainsmootherbop"),
                        "A removed soil becomes an available playable piece");
                int orientation = metadata % palette.orientations;
                int lower = palette.shape == Shape.SLAB ? 15
                        : palette.shape == Shape.HORIZONTAL_STEP ? new int[] { 3, 6, 12, 9 }[orientation & 3]
                        : 1 << (orientation & 3);
                int expectedMask = (palette.shape == Shape.SLAB ? orientation == 0 : orientation < 4)
                        ? lower << 4 : lower;
                int actualMask = 0;
                for (int verticalHalf = 0; verticalHalf < 2; verticalHalf++) {
                    for (int corner = 0; corner < 4; corner++) {
                        double sampleX = corner == 1 || corner == 2 ? .75 : .25;
                        double sampleZ = corner >= 2 ? .75 : .25;
                        for (AABB box : target.getShape(world, new BlockPos(0, 250, 0)).toAabbs()) {
                            if (box.contains(sampleX, verticalHalf == 0 ? .25 : .75, sampleZ)) {
                                actualMask |= 1 << (verticalHalf * 4 + corner);
                                break;
                            }
                        }
                    }
                }
                RuntimeProbe.require(actualMask == expectedMask,
                        "Actual target shape preserves the old footprint and upper/lower half: " + palette.id + "/" + metadata);

                CompoundTag stack = new CompoundTag();
                stack.putString("id", palette.id);
                stack.putShort("Damage", (short) metadata);
                stack.putByte("Count", (byte) 23);
                stack.putByte("Slot", (byte) 7);
                CompoundTag custom = new CompoundTag();
                custom.putString("CustomMaterialNote", palette.id + "/" + metadata);
                stack.put("tag", custom.copy());
                CompoundTag root = new CompoundTag();
                ListTag nested = new ListTag();
                nested.add(stack);
                root.put("Items", nested);
                LegacySoils.recover(root);
                ItemStack expected = Pieces.stack(target, 23);
                ItemStack actual = ProbeItems.readPreComponentStack(world, stack);
                RuntimeProbe.require(actual.getItem() == expected.getItem() && actual.getCount() == 23,
                        "Nested inventory identity and count survive");
                RuntimeProbe.require(ProbeItems.custom(actual).equals(custom),
                        "Nested inventory custom data survives the native item conversion");
                RuntimeProbe.require(stack.getIntOr("Slot", -1) == 7 && stack.getCompoundOrEmpty("tag").equals(custom),
                        "Inventory slot and unrelated custom NBT survive");
                CompoundTag once = root.copy();
                LegacySoils.recover(root);
                RuntimeProbe.require(root.equals(once), "Item recovery is idempotent");

                CompoundTag named = new CompoundTag();
                named.putString("Name", palette.id);
                CompoundTag properties = new CompoundTag();
                properties.putString("meta", Integer.toString(metadata));
                properties.putString("snowy", "false");
                named.put("Properties", properties);
                named.putString("Unrelated", "kept");
                CompoundTag current = new CompoundTag();
                current.putString("id", palette.id);
                current.put("properties", properties.copy());
                current.putString("Unrelated", "kept");
                LegacySoils.recover(named);
                LegacySoils.recover(current);
                RuntimeProbe.require(ProbeItems.readFixtureState(world, named).equals(target),
                        "Named states preserve the exact target geometry");
                RuntimeProbe.require(named.getStringOr("Unrelated", "").equals("kept"), "Unrelated state data survives");
                RuntimeProbe.require(net.minecraft.nbt.NbtUtils.readBlockState(
                                world.holderLookup(net.minecraft.core.registries.Registries.BLOCK), current).equals(target)
                                && current.getStringOr("Unrelated", "").equals("kept"),
                        "Modern palette compounds preserve the same geometry and unrelated data");
                CompoundTag namedOnce = named.copy();
                LegacySoils.recover(named);
                RuntimeProbe.require(named.equals(namedOnce), "Current named coarse slabs are not converted twice");
                CompoundTag currentOnce = current.copy();
                LegacySoils.recover(current);
                RuntimeProbe.require(current.equals(currentOnce), "Modern coarse slabs are not mistaken for inventory stacks");
                checked += 10;
            }
        }
        RuntimeProbe.require(states == 306, "All 306 assigned old soil and path states are covered");
        CompoundTag root = new CompoundTag();
        ListTag items = new ListTag();
        for (int metadata : new int[] { 0, 16 }) {
            CompoundTag stack = new CompoundTag();
            stack.putString("id", "skysterrainsmootherbop:grass_slab_00");
            stack.putShort("Damage", (short) metadata);
            stack.putByte("Count", (byte) 2);
            items.add(stack);
        }
        root.put("Items", items);
        CompoundTag before = root.copy();
        boolean rejected = false;
        try {
            LegacySoils.recover(root);
        } catch (LegacyBridge.RecoveryAbort expected) {
            rejected = true;
        }
        RuntimeProbe.require(rejected && root.equals(before), "A malformed later stack leaves the whole tree untouched");
        for (int count : new int[] { -1, 0, 128 }) {
            CompoundTag invalid = new CompoundTag();
            invalid.putString("id", "skysterrainsmootherbop:grass_slab_00");
            invalid.putShort("Damage", (short) 0);
            invalid.putInt("Count", count);
            CompoundTag unchanged = invalid.copy();
            rejected = false;
            try {
                LegacySoils.recover(invalid);
            } catch (LegacyBridge.RecoveryAbort expected) {
                rejected = true;
            }
            RuntimeProbe.require(rejected && invalid.equals(unchanged),
                    "A count outside the saved-byte range stops without changing the stack");
        }
        int recipes = 0;
        for (var holder : world.getServer().getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            if (!holder.id().identifier().getNamespace().equals("skysterrainsmootherbop")) {
                continue;
            }
            var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(world);
            RuntimeProbe.require(!recipe.isSpecial() && !recipe.display().isEmpty(),
                    "Replacement recipes remain visible in the native recipe book");
            for (var display : recipe.display()) {
                RuntimeProbe.require(!display.result().resolveForFirstStack(context).isEmpty(),
                        "The recipe book shows the replacement item");
                var slots = switch (display) {
                    case net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped ->
                            shaped.ingredients();
                    case net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless ->
                            shapeless.ingredients();
                    default -> throw new AssertionError("Unexpected BOP recipe display: " + display);
                };
                RuntimeProbe.require(slots.stream().anyMatch(slot -> !slot.resolveForStacks(context).isEmpty()),
                        "Replacement recipes expose native book ingredients");
            }
            recipes++;
        }
        RuntimeProbe.require(recipes == 84, "All original recipe identities load with available ingredients");
        zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info("TERRAIN_BOP_RECOVERY_PASS states=" + states + " checks=" + checked);
        return checked + 2;
    }
}
