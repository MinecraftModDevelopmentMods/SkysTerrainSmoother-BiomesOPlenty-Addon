package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.io.*;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.item.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.skysterrainsmootherbop.*;
import zone.moddev.mc.skysterrainsmoother.content.SandContent;

public final class ClientProbe extends ProbeProxy {
    private boolean ran, integrated;

    @Override
    public void init() {
        if (org.lwjgl.opengl.Display.isCreated()) {
            org.lwjgl.opengl.Display.setLocation(-10000, -10000);
        }
        if (org.lwjgl.input.Mouse.isCreated()) {
            org.lwjgl.input.Mouse.setGrabbed(false);
        }
        Minecraft.getMinecraft().getSession().setProperties(new com.mojang.authlib.properties.PropertyMap());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (ran) {
            if (integrated && ClientStatus.complete) {
                System.out.println("BOP_TERRAIN_CLIENT_WORLD_PASS");
                minecraft.shutdown();
                integrated = false;
            }

            return;
        }
        if (minecraft.currentScreen == null) {
            return;
        }

        ran = true;

        net.minecraftforge.fml.client.IModGuiFactory factory = net.minecraftforge.fml.client.FMLClientHandler.instance()
                .getGuiFactoryFor(net.minecraftforge.fml.common.Loader.instance().getIndexedModList()
                        .get(SkysTerrainSmootherBop.ID));
        RuntimeProbe.require(factory != null && factory.hasConfigGui(), "the BOP Config button is available");
        net.minecraft.client.gui.GuiScreen screen = factory.createConfigGui(minecraft.currentScreen);
        net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(minecraft);
        screen.setWorldAndResolution(minecraft, resolution.getScaledWidth(), resolution.getScaledHeight());
        net.minecraftforge.fml.client.config.GuiConfig settings = (net.minecraftforge.fml.client.config.GuiConfig) screen;
        RuntimeProbe.require(settings.configElements.size() == 4 && settings.allRequireMcRestart,
                "the BOP button opens the shared settings and material pages");
        minecraft.getFramebuffer().bindFramebuffer(true);
        minecraft.entityRenderer.setupOverlayRendering();
        screen.drawScreen(0, 0, 0);
        screen.onGuiClosed();
        System.out.println("BOP_TERRAIN_CONFIG_PASS shared_pages=true restart_required=true");

        int models = 0;
        int items = 0;

        for (IBlockState state : SandContent.slab.getBlockState().getValidStates()) {
            model(minecraft, state);
            models++;
        }
        for (SoilPieceBlock block : SoilPieces.BLOCKS) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (state.getValue(SoilPieceBlock.META) / block.shape.states >= block.count) {
                    continue;
                }

                model(minecraft, state);
                models++;

                int actual = minecraft.getBlockColors().colorMultiplier(state, null, null, 0);
                int nativeColour = minecraft.getBlockColors().colorMultiplier(block.full(state), null, null, 0);

                if (block.grass) {
                    RuntimeProbe.require(actual == nativeColour, "native BOP biome/item tint " + state);
                }
            }
            for (int slot = 0; slot < block.count; slot++) {
                ItemStack stack = new ItemStack(block, 1, slot * block.shape.states);
                IBakedModel item = minecraft.getRenderItem().getItemModelMesher().getItemModel(stack);

                RuntimeProbe.require(item != minecraft.getBlockRendererDispatcher().getBlockModelShapes()
                        .getModelManager().getMissingModel(), "inventory model " + stack);
                RuntimeProbe.require(
                        !stack.getDisplayName().contains("pieces.") && !stack.getDisplayName().contains("tile."),
                        "localized item display name");
                items++;
            }
        }

        for (SoilPathBlock block : SoilPaths.BLOCKS) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                if (state.getValue(SoilPieceBlock.META) / block.shape.states >= block.count) {
                    continue;
                }
                model(minecraft, state);
                RuntimeProbe.require(minecraft.getBlockColors().colorMultiplier(state, null, null, 0) == -1,
                        "BOP path surfaces remain untinted");
                models++;
            }
            for (int slot = 0; slot < block.count; slot++) {
                ItemStack stack = new ItemStack(block, 1, slot * block.shape.states);
                RuntimeProbe.require(minecraft.getRenderItem().getItemModelMesher().getItemModel(stack)
                        != minecraft.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel(),
                        "BOP path item model");
                RuntimeProbe.require(!stack.getDisplayName().contains("pieces.")
                        && !stack.getDisplayName().contains("tile."), "BOP path item name");
                items++;
            }
        }
        System.out.println("BOP_TERRAIN_CLIENT_PASS models=" + models + " items=" + items);
        MineralogyProbe.run();

        try (Writer writer = new FileWriter(new File(minecraft.gameDir, "bop-terrain-client-pass.txt"))) {
            writer.write("BOP_TERRAIN_CLIENT_PASS\n");
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        if (System.getProperty("skysterrainsmootherbop.integrationPhase", "").startsWith("client-")) {
            integrated = true;
            minecraft.launchIntegratedServer("terrain-smoother-world", "BOP Terrain Test",
                    new WorldSettings(1272993827L, GameType.CREATIVE, true, false, WorldType.byName("BIOMESOP")));
        } else {
            minecraft.shutdown();
        }
    }

    private static void model(Minecraft minecraft, IBlockState state) {
        IBakedModel model = minecraft.getBlockRendererDispatcher().getModelForState(state);

        RuntimeProbe.require(model != minecraft.getBlockRendererDispatcher().getBlockModelShapes().getModelManager()
                .getMissingModel(), "missing block model " + state);

        for (EnumFacing face : new EnumFacing[] { null, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH,
                EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST }) {
            for (BakedQuad quad : model.getQuads(state, face, 0)) {
                RuntimeProbe.require(!quad.getSprite().getIconName().contains("missingno"), "missing texture " + state);

                if (state.getPropertyKeys().contains(BlockGrass.SNOWY) && state.getValue(BlockGrass.SNOWY)) {
                    RuntimeProbe.require(!quad.hasTintIndex(), "snow is never grass tinted");
                }
            }
        }
    }
}
