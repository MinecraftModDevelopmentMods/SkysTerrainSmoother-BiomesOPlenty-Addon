package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;

/** Keeps old BOP terrain pieces recoverable and adds the remaining rock mappings. */
@Mod(SkysTerrainSmootherBop.ID)
public final class SkysTerrainSmootherBop {
    public static final String ID = "skysterrainsmootherbop";
    public static final String VERSION = "0.1.0.120061";

    public SkysTerrainSmootherBop(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        NativeTerrain.register();
    }

    @Mod.EventBusSubscriber(modid = ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registries {
        private Registries() {
        }

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.BLOCKS, registry -> {
                // The content catalogue is ready by the time Forge opens its registries.
                LegacySoils.register();
                for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                    if (palette.id.equals(CoarseDirtSlab.BLOCK_ID)) {
                        CoarseDirtSlab.BLOCK = new CoarseDirtSlab();
                        palette.holder = CoarseDirtSlab.BLOCK;
                    } else {
                        palette.holder = new LegacySoilBlock(palette);
                    }
                    registry.register(new ResourceLocation(palette.id), palette.holder);
                }
            });

            event.register(ForgeRegistries.Keys.ITEMS, registry -> {
                for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                    registry.register(new ResourceLocation(palette.id),
                            new BlockItem(palette.holder, new Item.Properties()));
                }

                BlockItem coarse = new BlockItem(CoarseDirtSlab.BLOCK, new Item.Properties()) {
                    @Override
                    public Component getName(ItemStack stack) {
                        return Component.translatable("pieces.skysterrainsmootherbop.slab",
                                Component.translatable(Blocks.COARSE_DIRT.getDescriptionId()));
                    }
                };
                registry.register(new ResourceLocation(ID, "coarse_dirt_slab"), coarse);
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
