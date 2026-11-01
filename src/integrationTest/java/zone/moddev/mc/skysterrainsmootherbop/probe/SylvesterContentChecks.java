package zone.moddev.mc.skysterrainsmootherbop.probe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.Dynamic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.datafix.TypeReferences;
import net.minecraft.world.server.ServerWorld;
import zone.moddev.mc.skysbuildingpieces.catalogue.Catalogue;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysbuildingpieces.legacy.HistoricalFullBlocks;
import zone.moddev.mc.skysbuildingpieces.legacy.LegacyBridge;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Optional, ignored input from a read-only world census; never bundled in the test mod. */
final class SylvesterContentChecks {
    static int run(ServerWorld world) throws Exception {
        File input = new File("source-sylvester-sky-content.json");
        if (!input.isFile()) {
            return 0;
        }
        JsonObject census;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(input), StandardCharsets.UTF_8)) {
            census = new JsonParser().parse(reader).getAsJsonObject();
        }
        RuntimeProbe.require(census.get("source_unchanged").getAsBoolean(), "The census preserved every source file");
        long blockUnits = 0;
        for (JsonElement row : census.getAsJsonArray("blocks")) {
            JsonObject saved = row.getAsJsonObject();
            String id = saved.get("id").getAsString();
            int metadata = saved.get("meta").getAsInt();
            BlockState target = block(id, metadata);
            RuntimeProbe.require(target != null && !target.getBlock().hasTileEntity(target),
                    "Every placed Sky state in the actual world has a tile-free target: " + id + "/" + metadata);
            RuntimeProbe.require(!Pieces.stack(target, 1).isEmpty(), "The actual world's target has a usable drop");
            blockUnits += saved.get("count").getAsLong();
        }

        File output = new File("world/sylvester-sky-audit-items.dat");
        boolean reload = output.isFile();
        CompoundNBT prior = null;
        if (reload) {
            try (FileInputStream stream = new FileInputStream(output)) {
                prior = CompressedStreamTools.readCompressed(stream);
            }
        }
        ListNBT converted = new ListNBT();
        int itemIndex = 0;
        for (JsonElement row : census.getAsJsonArray("unique_stack_nbt")) {
            CompoundNBT original = JsonToNBT.getTagFromJson(row.getAsString());
            CompoundNBT source = reload ? prior.getList("Items", 10).getCompound(itemIndex).copy() : original.copy();
            CompoundNBT before = source.copy();
            LegacyBridge.prepareNbt(source);
            CompoundNBT fixed = (CompoundNBT) world.getServer().getDataFixer().update(TypeReferences.ITEM_STACK,
                    new Dynamic<>(NBTDynamicOps.INSTANCE, source), reload ? 2230 : 1343, 2230).getValue();
            LegacyBridge.finalizeNbt(fixed, 2230);
            ItemStack stack = ItemStack.read(fixed);
            RuntimeProbe.require(!stack.isEmpty() && stack.getCount() == (original.getByte("Count") & 255),
                    "Actual saved Sky stack keeps its count: " + original);
            for (String key : original.keySet()) {
                if (!key.equals("id") && !key.equals("Damage") && !key.equals("Count") && !key.equals("tag")) {
                    RuntimeProbe.require(original.get(key).equals(fixed.get(key)),
                            "Unrelated root item NBT survives: " + key);
                }
            }
            CompoundNBT originalTag = original.getCompound("tag");
            for (String key : originalTag.keySet()) {
                if (!key.equals("display") && !key.equals("ench") && !key.equals("StoredEnchantments")
                        && !key.equals("Items") && !key.equals("BasketOpen")) {
                    RuntimeProbe.require(originalTag.get(key).equals(fixed.getCompound("tag").get(key)),
                            "Unrelated item tag survives: " + key);
                }
            }
            if (reload) {
                RuntimeProbe.require(before.equals(fixed), "Already upgraded actual item data is unchanged on reload");
            }
            converted.add(fixed);
            itemIndex++;
        }
        CompoundNBT saved = new CompoundNBT();
        saved.put("Items", converted);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            CompressedStreamTools.writeCompressed(saved, stream);
        }
        RuntimeProbe.require(blockUnits == 2721943 && itemIndex == 237,
                "The complete read-only census was checked, not a selected sample");
        System.out.println("SYLVESTER_SKY_CONTENT_PASS blocks=" + blockUnits + " states="
                + census.getAsJsonArray("blocks").size() + " unique_stacks=" + itemIndex + " reload=" + reload);
        return census.getAsJsonArray("blocks").size() + itemIndex;
    }

    private static BlockState block(String id, int metadata) {
        zone.moddev.mc.skysterrainsmootherbop.LegacySoils.Palette soil =
                zone.moddev.mc.skysterrainsmootherbop.LegacySoils.PALETTES.get(id);
        if (soil != null) {
            return soil.resolve(metadata);
        }
        if (HistoricalFullBlocks.historical(id)) {
            return HistoricalFullBlocks.resolve(id, metadata);
        }
        for (Catalogue.Palette palette : Catalogue.INSTANCE.palettes) {
            if (palette.registryId().equals(id)) {
                return Pieces.state(palette.material(metadata), palette.shape, metadata % palette.shape.states);
            }
        }
        if (id.equals("skysgrassslabs:turf")) {
            return ModBlocks.TURF.getDefaultState();
        }
        if (id.equals("skysterrainsmoother:sand_slab")) {
            return zone.moddev.mc.skysterrainsmoother.content.SandContent.legacyState(metadata);
        }
        net.minecraft.block.Block block = id.equals("skysgrassslabs:grass_slab") ? ModBlocks.GRASS_SLAB
                : id.equals("skysgrassslabs:dirt_slab") ? ModBlocks.DIRT_SLAB
                : id.equals(ModBlocks.PATH_SLAB.getRegistryName().toString()) ? ModBlocks.PATH_SLAB : null;
        return block == null ? null : block.getDefaultState().with(SlabBlock.TYPE,
                (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM);
    }

    private SylvesterContentChecks() {
    }
}
