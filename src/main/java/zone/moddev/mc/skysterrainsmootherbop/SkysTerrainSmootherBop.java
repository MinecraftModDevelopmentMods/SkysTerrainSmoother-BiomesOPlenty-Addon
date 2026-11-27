package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import zone.moddev.mc.skysterrainsmoother.content.CoarseDirtSlab;

/** Keeps old BOP terrain pieces recoverable and adds the remaining rock mappings. */
@Mod(SkysTerrainSmootherBop.ID)
public final class SkysTerrainSmootherBop {
    public static final String ID = "skysterrainsmootherbop";
    public static final String VERSION = "0.1.4.121011";

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
                        palette.holder = CoarseDirtSlab.registeredBlock();
                        continue;
                    }
                    palette.holder = new LegacySoilBlock(palette);
                    registry.register(ResourceLocation.parse(palette.id), palette.holder);
                }
            });

            event.register(ForgeRegistries.Keys.ITEMS, registry -> {
                for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
                    if (!palette.id.equals(CoarseDirtSlab.BLOCK_ID)) {
                        registry.register(ResourceLocation.parse(palette.id), new BlockItem(palette.holder, new Item.Properties()));
                    }
                }
            });
        }
    }
}
