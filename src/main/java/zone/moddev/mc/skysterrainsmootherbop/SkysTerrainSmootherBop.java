package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;

/** Keeps old BOP terrain pieces recoverable and adds the remaining rock mappings. */
@Mod(SkysTerrainSmootherBop.ID)
public final class SkysTerrainSmootherBop {
    public static final String ID = "skysterrainsmootherbop";
    public static final String VERSION = "0.1.1.121012";

    public SkysTerrainSmootherBop(IEventBus modBus) {
        modBus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        NativeTerrain.register();
    }

    @EventBusSubscriber(modid = ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registries {
        private Registries() {
        }

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(net.minecraft.core.registries.Registries.BLOCK, registry -> {
                // The content catalogue is ready by the time NeoForge opens its registries.
                LegacySoils.register();
                for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                    if (palette.id.equals(CoarseDirtSlab.BLOCK_ID)) {
                        CoarseDirtSlab.BLOCK = new CoarseDirtSlab();
                        palette.holder = CoarseDirtSlab.BLOCK;
                    } else {
                        palette.holder = new LegacySoilBlock(palette);
                    }
                    registry.register(ResourceLocation.parse(palette.id), palette.holder);
                }
            });

            event.register(net.minecraft.core.registries.Registries.ITEM, registry -> {
                for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                    registry.register(ResourceLocation.parse(palette.id),
                            new BlockItem(palette.holder, new Item.Properties()));
                }

                BlockItem coarse = new BlockItem(CoarseDirtSlab.BLOCK, new Item.Properties()) {
                    @Override
                    public Component getName(ItemStack stack) {
                        return Component.translatable("pieces.skysterrainsmootherbop.slab",
                                Component.translatable(Blocks.COARSE_DIRT.getDescriptionId()));
                    }
                };
                registry.register(ResourceLocation.fromNamespaceAndPath(ID, "coarse_dirt_slab"), coarse);
                CoarseDirtSlab.item = coarse;
                BuildingPiecesApi.registerStateItems(CoarseDirtSlab.BLOCK, state -> coarse);
            });
        }

        @SubscribeEvent
        public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
            if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
                event.accept(CoarseDirtSlab.item);
            }
        }
    }
}
