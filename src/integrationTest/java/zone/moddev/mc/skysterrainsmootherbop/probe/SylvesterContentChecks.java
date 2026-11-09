package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraftforge.registries.ForgeRegistries;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.server.level.ServerLevel;
import zone.moddev.mc.skysbuildingpieces.catalogue.Catalogue;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysbuildingpieces.legacy.HistoricalFullBlocks;
import zone.moddev.mc.skysbuildingpieces.legacy.LegacyBridge;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Optional, ignored input from a read-only world census; never bundled in the test mod. */
final class SylvesterContentChecks {
    static int run(ServerLevel world) throws Exception {
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
            RuntimeProbe.require(target != null && !target.hasBlockEntity(),
                    "Every placed Sky state in the actual world has a tile-free target: " + id + "/" + metadata);
            RuntimeProbe.require(!Pieces.stack(target, 1).isEmpty(), "The actual world's target has a usable drop");
            blockUnits += saved.get("count").getAsLong();
        }

        File output = new File("world/sylvester-sky-audit-items.dat");
        boolean reload = output.isFile();
        CompoundTag prior = null;
        if (reload) {
            try (FileInputStream stream = new FileInputStream(output)) {
                prior = NbtIo.readCompressed(stream, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            }
        }
        ListTag converted = new ListTag();
        int itemIndex = 0;
        int targetVersion = net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
        for (JsonElement row : census.getAsJsonArray("unique_stack_nbt")) {
            CompoundTag original = TagParser.parseCompoundFully(row.getAsString());
            CompoundTag source = reload ? prior.getList("Items").orElseThrow().getCompound(itemIndex).orElseThrow().copy() : original.copy();
            CompoundTag before = source.copy();
            LegacyBridge.prepareNbt(source);
            CompoundTag fixed = (CompoundTag) world.getServer().getFixerUpper().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, source), reload ? targetVersion : 1343, targetVersion).getValue();
            LegacyBridge.finalizeNbt(fixed, targetVersion);
            ItemStack stack = ProbeItems.read(world, fixed);
            RuntimeProbe.require(!stack.isEmpty() && stack.getCount() == (original.getIntOr("Count", 0) & 255),
                    "Actual saved Sky stack keeps its count: " + original);
            for (String key : original.keySet()) {
                if (!key.equals("id") && !key.equals("Damage") && !key.equals("Count") && !key.equals("tag")) {
                    RuntimeProbe.require(original.get(key).equals(fixed.get(key)),
                            "Unrelated root item NBT survives: " + key);
                }
            }
            CompoundTag originalTag = original.getCompoundOrEmpty("tag");
            for (String key : originalTag.keySet()) {
                if (!key.equals("display") && !key.equals("ench") && !key.equals("StoredEnchantments")
                        && !key.equals("Items") && !key.equals("BasketOpen")) {
                    RuntimeProbe.require(originalTag.get(key).equals(ProbeItems.custom(stack).get(key)),
                            "Unrelated item tag survives: " + key);
                }
            }
            if (reload) {
                RuntimeProbe.require(before.equals(fixed), "Already upgraded actual item data is unchanged on reload");
            }
            converted.add(fixed);
            itemIndex++;
        }
        CompoundTag saved = new CompoundTag();
        saved.put("Items", converted);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            NbtIo.writeCompressed(saved, stream);
        }
        RuntimeProbe.require(blockUnits == 2721943 && itemIndex == 237,
                "The complete read-only census was checked, not a selected sample");
        zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info("SYLVESTER_SKY_CONTENT_PASS blocks=" + blockUnits + " states="
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
            return ModBlocks.TURF.get().defaultBlockState();
        }
        if (id.equals("skysterrainsmoother:sand_slab")) {
            return zone.moddev.mc.skysterrainsmoother.content.SandContent.legacyState(metadata);
        }
        net.minecraft.world.level.block.Block block = id.equals("skysgrassslabs:grass_slab") ? ModBlocks.GRASS_SLAB.get()
                : id.equals("skysgrassslabs:dirt_slab") ? ModBlocks.DIRT_SLAB.get()
                : id.equals(ForgeRegistries.BLOCKS.getKey(ModBlocks.PATH_SLAB.get()).toString()) ? ModBlocks.PATH_SLAB.get() : null;
        return block == null ? null : block.defaultBlockState().setValue(SlabBlock.TYPE,
                (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM);
    }

    private SylvesterContentChecks() {
    }
}
