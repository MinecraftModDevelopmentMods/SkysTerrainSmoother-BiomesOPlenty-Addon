package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Keeps old BOP terrain pieces recoverable and adds the remaining rock mappings. */
@Mod(SkysTerrainSmootherBop.ID)
public final class SkysTerrainSmootherBop {
    public static final String ID = "skysterrainsmootherbop";
    public static final String VERSION = "0.1.0.116051";

    public SkysTerrainSmootherBop() {
        LegacySoils.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        NativeTerrain.register();
    }

    @Mod.EventBusSubscriber(modid = ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registries {
        private Registries() {
        }

        @SubscribeEvent
        public static void blocks(RegistryEvent.Register<Block> event) {
            for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                if (palette.id.equals(CoarseDirtSlab.BLOCK_ID)) {
                    palette.holder = CoarseDirtSlab.BLOCK;
                    event.getRegistry().register(palette.holder);
                } else {
                    event.getRegistry().register(new LegacySoilBlock(palette));
                }
            }
        }

        @SubscribeEvent
        public static void items(RegistryEvent.Register<Item> event) {
            for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                event.getRegistry().register(new BlockItem(palette.holder, new Item.Properties())
                        .setRegistryName(palette.holder.getRegistryName()));
            }
            BlockItem coarse = new BlockItem(CoarseDirtSlab.BLOCK,
                    new Item.Properties().tab(net.minecraft.item.ItemGroup.TAB_BUILDING_BLOCKS)) {
                @Override
                public net.minecraft.util.text.ITextComponent getName(net.minecraft.item.ItemStack stack) {
                    return new net.minecraft.util.text.TranslationTextComponent("pieces.skysterrainsmootherbop.slab",
                            new net.minecraft.util.text.TranslationTextComponent(
                                    net.minecraft.block.Blocks.COARSE_DIRT.getDescriptionId()));
                }
            };
            coarse.setRegistryName(ID, "coarse_dirt_slab");
            event.getRegistry().register(coarse);
            CoarseDirtSlab.item = coarse;
            zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi.registerStateItems(CoarseDirtSlab.BLOCK,
                    state -> coarse);
        }
    }
}
