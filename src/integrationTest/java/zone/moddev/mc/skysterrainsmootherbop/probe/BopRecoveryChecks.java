package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import zone.moddev.mc.skysbuildingpieces.catalogue.Shape;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysbuildingpieces.legacy.HistoricalFullBlocks;
import zone.moddev.mc.skysbuildingpieces.legacy.LegacyBridge;
import zone.moddev.mc.skysterrainsmootherbop.LegacySoils;

/** Exercises every assigned old palette value against the actual registered target pieces. */
final class BopRecoveryChecks {
    static int run(ServerWorld world) {
        int checked = 0;
        int states = 0;
        for (String rock : new String[] { "crag_rock", "limestone", "shale", "siltstone" }) {
            String material = "biomesoplenty:" + rock;
            BlockState full = Pieces.nativeState(
                    zone.moddev.mc.skysbuildingpieces.catalogue.Catalogue.INSTANCE.materials.get(material), "full");
            boolean mineralogyOwnsRock = full.getBlock().getRegistryName().getNamespace().equals("mineralogy");
            RuntimeProbe.require(zone.moddev.mc.skysterrainsmoother.api.TerrainSmoothingApi
                            .hasMaterial(new net.minecraft.util.ResourceLocation(material)) != mineralogyOwnsRock,
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
                RuntimeProbe.require(HistoricalFullBlocks.resolve(palette.id, metadata).equals(target),
                        "The numeric bridge resolves the same material and orientation");
                RuntimeProbe.require(!target.getBlock().hasTileEntity(target) && target.getFluidState().isEmpty(),
                        "Recovered pieces are dry and have no block entity");
                RuntimeProbe.require(target.getBlock() == zone.moddev.mc.skysterrainsmootherbop.CoarseDirtSlab.BLOCK
                                || !target.getBlock().getRegistryName().getNamespace().equals("skysterrainsmootherbop"),
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
                        for (AxisAlignedBB box : target.getShape(world, new BlockPos(0, 250, 0)).toAabbs()) {
                            if (box.contains(sampleX, verticalHalf == 0 ? .25 : .75, sampleZ)) {
                                actualMask |= 1 << (verticalHalf * 4 + corner);
                                break;
                            }
                        }
                    }
                }
                RuntimeProbe.require(actualMask == expectedMask,
                        "Actual target shape preserves the old footprint and upper/lower half: " + palette.id + "/" + metadata);

                CompoundNBT stack = new CompoundNBT();
                stack.putString("id", palette.id);
                stack.putShort("Damage", (short) metadata);
                stack.putByte("Count", (byte) 23);
                stack.putByte("Slot", (byte) 7);
                CompoundNBT custom = new CompoundNBT();
                custom.putString("CustomMaterialNote", palette.id + "/" + metadata);
                stack.put("tag", custom.copy());
                CompoundNBT root = new CompoundNBT();
                ListNBT nested = new ListNBT();
                nested.add(stack);
                root.put("Items", nested);
                LegacySoils.recover(root);
                ItemStack expected = Pieces.stack(target, 23);
                ItemStack actual = ItemStack.of(stack);
                RuntimeProbe.require(actual.getItem() == expected.getItem() && actual.getCount() == 23,
                        "Nested inventory identity and count survive");
                RuntimeProbe.require(stack.getByte("Slot") == 7 && stack.getCompound("tag").equals(custom),
                        "Inventory slot and unrelated custom NBT survive");
                CompoundNBT once = root.copy();
                LegacySoils.recover(root);
                RuntimeProbe.require(root.equals(once), "Item recovery is idempotent");

                CompoundNBT named = new CompoundNBT();
                named.putString("Name", palette.id);
                CompoundNBT properties = new CompoundNBT();
                properties.putString("meta", Integer.toString(metadata));
                properties.putString("snowy", "false");
                named.put("Properties", properties);
                named.putString("Unrelated", "kept");
                LegacySoils.recover(named);
                RuntimeProbe.require(NBTUtil.readBlockState(named).equals(target),
                        "Named states preserve the exact target geometry");
                RuntimeProbe.require(named.getString("Unrelated").equals("kept"), "Unrelated state data survives");
                CompoundNBT namedOnce = named.copy();
                LegacySoils.recover(named);
                RuntimeProbe.require(named.equals(namedOnce), "Current named coarse slabs are not converted twice");
                checked += 8;
            }
        }
        RuntimeProbe.require(states == 306, "All 306 assigned old soil and path states are covered");
        CompoundNBT root = new CompoundNBT();
        ListNBT items = new ListNBT();
        for (int metadata : new int[] { 0, 16 }) {
            CompoundNBT stack = new CompoundNBT();
            stack.putString("id", "skysterrainsmootherbop:grass_slab_00");
            stack.putShort("Damage", (short) metadata);
            stack.putByte("Count", (byte) 2);
            items.add(stack);
        }
        root.put("Items", items);
        CompoundNBT before = root.copy();
        boolean rejected = false;
        try {
            LegacySoils.recover(root);
        } catch (LegacyBridge.RecoveryAbort expected) {
            rejected = true;
        }
        RuntimeProbe.require(rejected && root.equals(before), "A malformed later stack leaves the whole tree untouched");
        for (int count : new int[] { -1, 0, 128 }) {
            CompoundNBT invalid = new CompoundNBT();
            invalid.putString("id", "skysterrainsmootherbop:grass_slab_00");
            invalid.putShort("Damage", (short) 0);
            invalid.putInt("Count", count);
            CompoundNBT unchanged = invalid.copy();
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
        for (net.minecraft.item.crafting.IRecipe<?> recipe : world.getRecipeManager().getRecipes()) {
            if (!recipe.getId().getNamespace().equals("skysterrainsmootherbop")) {
                continue;
            }
            RuntimeProbe.require(!recipe.isSpecial() && !recipe.getResultItem().isEmpty(),
                    "Replacement recipes remain visible in the native recipe book");
            RuntimeProbe.require(recipe.getIngredients().stream()
                    .anyMatch(ingredient -> ingredient != net.minecraft.item.crafting.Ingredient.EMPTY),
                    "Replacement recipes expose native book ingredients");
            recipes++;
        }
        RuntimeProbe.require(recipes == 84, "All original recipe identities load with available ingredients");
        System.out.println("TERRAIN_BOP_RECOVERY_PASS states=" + states + " checks=" + checked);
        return checked + 2;
    }
}
