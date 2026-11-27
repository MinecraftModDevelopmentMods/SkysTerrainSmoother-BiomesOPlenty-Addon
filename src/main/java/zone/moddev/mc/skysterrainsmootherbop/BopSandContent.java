package zone.moddev.mc.skysterrainsmootherbop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import zone.moddev.mc.skysterrainsmoother.api.MaterialGroup;
import zone.moddev.mc.skysterrainsmoother.api.TerrainMaterial;
import zone.moddev.mc.skysterrainsmoother.api.TerrainSmoothingApi;
import zone.moddev.mc.skysterrainsmoother.content.SandSlabBlock;
import zone.moddev.mc.skysterrainsmoother.content.SandSlabItem;

/** Sand slabs fall and rejoin just like the matching full sand block. */
@Mod.EventBusSubscriber(modid = SkysTerrainSmootherBop.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BopSandContent {
    private static final ResourceLocation MOSSY_SLAB = ResourceLocation.fromNamespaceAndPath(SkysTerrainSmootherBop.ID,
            "mossy_black_sand_slab");
    private static final List<String> SANDS = List.of("white_sand", "orange_sand", "black_sand", "mossy_black_sand");
    private static final Map<ResourceLocation, SandSlabBlock> SLABS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();

    private BopSandContent() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, registry -> {
            for (String name : SANDS) {
                ResourceLocation source = ResourceLocation.fromNamespaceAndPath("biomesoplenty", name);
                Block sand = ForgeRegistries.BLOCKS.getValue(source);
                if (sand == null || sand == Blocks.AIR) {
                    throw new IllegalStateException("Missing BoP sand: " + source);
                }
                SandSlabBlock slab = new SandSlabBlock(sand);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SkysTerrainSmootherBop.ID, name + "_slab");
                registry.register(id, slab);
                SLABS.put(id, slab);
            }
        });
        event.register(ForgeRegistries.Keys.ITEMS, registry -> SLABS.forEach((id, slab) -> {
            Item item = new SandSlabItem(slab);
            registry.register(id, item);
            ITEMS.put(id, item);
        }));
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            ITEMS.values().forEach(event::accept);
        }
    }

    static void registerTerrain() {
        for (SandSlabBlock slab : SLABS.values()) {
            var full = slab.fullState();
            TerrainSmoothingApi.registerMaterial(new TerrainMaterial(ForgeRegistries.BLOCKS.getKey(full.getBlock()),
                    Set.of(full), slab.defaultBlockState(), null, null, false, null, MaterialGroup.SAND_AND_GRAVEL));
        }
    }

    @Mod.EventBusSubscriber(modid = SkysTerrainSmootherBop.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Colours {
        private Colours() {
        }

        @SubscribeEvent
        public static void registerBlocks(RegisterColorHandlersEvent.Block event) {
            var mossy = SLABS.get(MOSSY_SLAB);
            event.register((state, world, pos, tint) -> event.getBlockColors().getColor(mossy.fullState(), world, pos, tint), mossy);
        }

        @SubscribeEvent
        public static void registerItems(RegisterColorHandlersEvent.Item event) {
            var nativeItem = SLABS.get(MOSSY_SLAB).fullState().getBlock().asItem().getDefaultInstance();
            event.register((stack, tint) -> event.getItemColors().getColor(nativeItem, tint), ITEMS.get(MOSSY_SLAB));
        }
    }
}
