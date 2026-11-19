package zone.moddev.mc.skysterrainsmootherbop;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;

/** Keeps old BOP terrain pieces recoverable and adds the remaining rock mappings. */
@Mod(SkysTerrainSmootherBop.ID)
public final class SkysTerrainSmootherBop {
    public static final String ID = "skysterrainsmootherbop";
    public static final String VERSION = "0.1.3.2602002";

    public SkysTerrainSmootherBop(IEventBus modBus) {
        modBus.addListener(this::setup);
        modBus.addListener(RegistryEvents::register);
        modBus.addListener(RegistryEvents::creativeTab);
    }

    private void setup(FMLCommonSetupEvent event) {
        NativeTerrain.register();
    }

    public static final class RegistryEvents {
        private RegistryEvents() {
        }

        public static void register(RegisterEvent event) {
            event.register(Registries.BLOCK, registry -> {
                // The content catalogue is ready by the time NeoForge opens its registries.
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

            event.register(Registries.ITEM, registry -> {
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
