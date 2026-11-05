package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
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
    public static final String VERSION = "0.1.0.121111";

    public SkysTerrainSmootherBop(FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();
        FMLCommonSetupEvent.getBus(modBus).addListener(this::setup);
        RegisterEvent.getBus(modBus).addListener(RegistryEvents::register);
        BuildCreativeModeTabContentsEvent.getBus(modBus).addListener(RegistryEvents::creativeTab);
    }

    private void setup(FMLCommonSetupEvent event) {
        NativeTerrain.register();
    }

    public static final class RegistryEvents {
        private RegistryEvents() {
        }

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
                    registry.register(Identifier.parse(palette.id), palette.holder);
                }
            });

            event.register(ForgeRegistries.Keys.ITEMS, registry -> {
                for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                    registry.register(Identifier.parse(palette.id),
                            new BlockItem(palette.holder, new Item.Properties()
                                    .setId(ResourceKey.create(Registries.ITEM, Identifier.parse(palette.id)))));
                }

                Identifier coarseId = Identifier.fromNamespaceAndPath(ID, "coarse_dirt_slab");
                BlockItem coarse = new BlockItem(CoarseDirtSlab.BLOCK,
                        new Item.Properties().setId(ResourceKey.create(Registries.ITEM, coarseId))) {
                    @Override
                    public Component getName(ItemStack stack) {
                        return Component.translatable("pieces.skysterrainsmootherbop.slab",
                                Component.translatable(Blocks.COARSE_DIRT.getDescriptionId()));
                    }
                };
                registry.register(coarseId, coarse);
                CoarseDirtSlab.item = coarse;
                BuildingPiecesApi.registerStateItems(CoarseDirtSlab.BLOCK, state -> coarse);
            });
        }

        public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
            if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
                event.accept(CoarseDirtSlab.item);
            }
        }
    }
}
