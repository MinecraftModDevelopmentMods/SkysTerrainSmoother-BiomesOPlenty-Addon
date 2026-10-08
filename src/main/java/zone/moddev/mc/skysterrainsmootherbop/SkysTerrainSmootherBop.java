package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;

@Mod(modid=SkysTerrainSmootherBop.ID,name="Sky's Terrain Smoother - Biomes O Plenty",version=SkysTerrainSmootherBop.VERSION,
        acceptedMinecraftVersions="[1.10.2]",dependencies="required-after:skysterrainsmoother@[0.1.0.110021,0.2);required-after:skysgrassslabs@[1.1.0.110021,2);required-after:skysbuildingpieces@[0.3.0.110021,0.4);required-after:BiomesOPlenty@[5.0.0,6);after:skysbuildingpiecesbop")
public final class SkysTerrainSmootherBop {
    public static final String ID="skysterrainsmootherbop",VERSION="0.1.0.110021";
    @SidedProxy(clientSide="zone.moddev.mc.skysterrainsmootherbop.client.ClientProxy",serverSide="zone.moddev.mc.skysterrainsmootherbop.client.CommonProxy")
    public static zone.moddev.mc.skysterrainsmootherbop.client.CommonProxy proxy;
    @Mod.EventHandler public void pre(FMLPreInitializationEvent event){SoilPieces.register();proxy.preInit();}
    @Mod.EventHandler public void init(FMLInitializationEvent event){SoilPieces.initialize();proxy.init();}
}
