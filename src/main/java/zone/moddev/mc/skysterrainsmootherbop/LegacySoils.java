package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
import zone.moddev.mc.skysbuildingpieces.catalogue.Shape;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysbuildingpieces.legacy.HistoricalFullBlocks;
import zone.moddev.mc.skysbuildingpieces.legacy.HistoricalItems;
import zone.moddev.mc.skysbuildingpieces.legacy.LegacyBridge;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Explicit replacements for the soil families removed from BOP after 1.12. */
public final class LegacySoils {
    public static final String MODULE = "terrain_bop_soils";
    public static final Map<String, Palette> PALETTES = createPalettes();
    private static final String[] GRASS_TARGETS = {
            "minecraft:mossy_cobblestone", "minecraft:mossy_cobblestone",
            "minecraft:grass", "minecraft:grass", "minecraft:grass",
            "minecraft:grass", "minecraft:netherrack", "minecraft:grass"
    };

    private LegacySoils() {
    }

    private static Map<String, Palette> createPalettes() {
        Map<String, Palette> result = new LinkedHashMap<>();
        for (String family : new String[] { "dirt", "grass", "path" }) {
            int materials = family.equals("grass") ? 8 : family.equals("dirt") ? 6 : 3;
            for (Shape shape : new Shape[] { Shape.SLAB, Shape.HORIZONTAL_STEP, Shape.CORNER }) {
                int orientations = shape == Shape.SLAB ? 2 : 8;
                int capacity = 16 / orientations;
                String shapeName = shape == Shape.HORIZONTAL_STEP ? "step" : shape.name().toLowerCase(Locale.ROOT);
                for (int start = 0; start < materials; start += capacity) {
                    String id = SkysTerrainSmootherBop.ID + ":" + family + "_" + shapeName + "_"
                            + String.format(Locale.ROOT, "%02d", start / capacity);
                    result.put(id, new Palette(id, family, shape, start,
                            Math.min(capacity, materials - start), orientations));
                }
            }
        }
        if (result.size() != 21) {
            throw new IllegalStateException("The permanent BOP terrain palettes changed");
        }
        return Collections.unmodifiableMap(result);
    }

    public static void register() {
        for (Palette palette : PALETTES.values()) {
            HistoricalFullBlocks.registerStates(palette.id, palette.id, MODULE,
                    metadata -> palette.valid(metadata) ? palette.resolve(metadata) : null);
        }
        HistoricalItems.register(MODULE, LegacySoils::contains, LegacySoils::recover);
    }

    private static boolean contains(CompoundTag root) {
        List<Change> changes = new ArrayList<>();
        collect(root, changes, 0);
        return !changes.isEmpty();
    }

    /** Resolve the entire tree first. A bad entry must never leave half an inventory converted. */
    public static void recover(CompoundTag root) {
        List<Change> changes = new ArrayList<>();
        collect(root, changes, 0);
        for (Change change : changes) {
            if (change.item) {
                ItemStack target = Pieces.stack(change.state, 1);
                if (target.isEmpty()) {
                    throw abort("The recovered soil piece has no inventory item");
                }
            }
        }
        for (Change change : changes) {
            if (change.item) {
                ItemStack target = Pieces.stack(change.state, 1);
                change.saved.putString("id", BuiltInRegistries.ITEM.getKey(target.getItem()).toString());
                change.saved.remove("Damage");
            } else {
                CompoundTag target = NbtUtils.writeBlockState(change.state);
                change.saved.putString("Name", target.getString("Name"));
                change.saved.put("Properties", target.getCompound("Properties"));
            }
        }
    }

    private static void collect(Tag value, List<Change> changes, int depth) {
        if (depth > 64) {
            throw abort("Saved BOP terrain nesting exceeds 64 levels");
        }
        if (value instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) value;
            Palette item = PALETTES.get(compound.getString("id"));
            Palette block = PALETTES.get(compound.getString("Name"));
            if (item != null) {
                if (!integer(compound, "Damage") || !integer(compound, "Count")
                        || compound.getLong("Count") < 1 || compound.getLong("Count") > 127
                        || compound.getLong("Damage") < 0 || compound.getLong("Damage") > 15
                        || !item.valid(compound.getInt("Damage"))) {
                    throw abort("Invalid saved BOP soil stack: " + item.id);
                }
                changes.add(new Change(compound, item.resolve(compound.getInt("Damage")), true));
            }
            if (block != null) {
                BlockState target = namedState(block, compound.getCompound("Properties"));
                if (target != null) {
                    changes.add(new Change(compound, target, false));
                }
            }
            for (String key : compound.getAllKeys()) {
                collect(compound.get(key), changes, depth + 1);
            }
        } else if (value instanceof ListTag) {
            for (Tag child : (ListTag) value) {
                collect(child, changes, depth + 1);
            }
        }
    }

    private static BlockState namedState(Palette palette, CompoundTag properties) {
        if (palette.id.equals(CoarseDirtSlab.BLOCK_ID) && properties.contains("type")) {
            if (properties.contains("meta")) {
                throw abort("Conflicting old and current coarse dirt slab properties");
            }
            String type = properties.getString("type");
            if (type.equals("double")) {
                return net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState();
            }
            if (!type.equals("top") && !type.equals("bottom")) {
                throw abort("Invalid saved coarse dirt slab half: " + type);
            }
            return null;
        }
        int metadata;
        try {
            metadata = Integer.parseInt(properties.getString("meta"));
        } catch (NumberFormatException failure) {
            throw abort("Missing or invalid saved BOP soil orientation: " + palette.id);
        }
        if (!palette.valid(metadata)) {
            throw abort("Unassigned saved BOP soil slot: " + palette.id + " / " + metadata);
        }
        return palette.resolve(metadata);
    }

    private static boolean integer(CompoundTag compound, String key) {
        Tag value = compound.get(key);
        return value != null && value.getId() >= 1 && value.getId() <= 4;
    }

    private static LegacyBridge.RecoveryAbort abort(String detail) {
        return new LegacyBridge.RecoveryAbort(detail + ". Restore the world backup and required Sky mods.", null);
    }

    private static final class Change {
        final CompoundTag saved;
        final BlockState state;
        final boolean item;

        Change(CompoundTag saved, BlockState state, boolean item) {
            this.saved = saved;
            this.state = state;
            this.item = item;
        }
    }

    public static final class Palette {
        public final String id;
        public final String family;
        public final Shape shape;
        public final int start;
        public final int count;
        public final int orientations;
        Block holder;

        Palette(String id, String family, Shape shape, int start, int count, int orientations) {
            this.id = id;
            this.family = family;
            this.shape = shape;
            this.start = start;
            this.count = count;
            this.orientations = orientations;
        }

        public boolean valid(int metadata) {
            return metadata >= 0 && metadata < count * orientations;
        }

        public String material(int metadata) {
            if (!valid(metadata)) {
                throw abort("Unassigned saved BOP soil slot: " + id + " / " + metadata);
            }
            return family.equals("grass") ? GRASS_TARGETS[start + metadata / orientations]
                    : family.equals("path") ? "minecraft:grass_path"
                    : start + metadata / orientations >= 3 ? "minecraft:dirt_coarse" : "minecraft:dirt";
        }

        public BlockState resolve(int metadata) {
            String material = material(metadata);
            int orientation = metadata % orientations;
            BlockState target;
            if (shape == Shape.SLAB && material.equals("minecraft:dirt_coarse")) {
                target = CoarseDirtSlab.BLOCK.defaultBlockState().setValue(SlabBlock.TYPE,
                        orientation == 0 ? SlabType.TOP : SlabType.BOTTOM);
            } else if (shape == Shape.SLAB && (material.equals("minecraft:grass")
                    || material.equals("minecraft:dirt") || material.equals("minecraft:grass_path"))) {
                Block block = material.equals("minecraft:grass") ? ModBlocks.GRASS_SLAB.get()
                        : material.equals("minecraft:dirt") ? ModBlocks.DIRT_SLAB.get() : ModBlocks.PATH_SLAB.get();
                target = block.defaultBlockState().setValue(SlabBlock.TYPE,
                        orientation == 0 ? SlabType.TOP : SlabType.BOTTOM);
            } else {
                target = Pieces.state(material, shape, orientation);
            }
            if (target == null || target.hasBlockEntity()) {
                throw abort("No safe replacement for " + id + " / " + metadata);
            }
            return target;
        }
    }
}
