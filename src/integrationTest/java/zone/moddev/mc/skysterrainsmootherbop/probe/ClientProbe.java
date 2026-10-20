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

        System.out.println("BOP_TERRAIN_CLIENT_PASS models=" + models + " items=" + items);

        try (Writer writer = new FileWriter(new File(minecraft.mcDataDir, "bop-terrain-client-pass.txt"))) {
            writer.write("BOP_TERRAIN_CLIENT_PASS\n");
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        if (System.getProperty("skysterrainsmootherbop.integrationPhase", "").startsWith("client-")) {
            integrated = true;
            minecraft.launchIntegratedServer("terrain-smoother-world", "BOP Terrain Test", new WorldSettings(
                    1272993827L, GameType.CREATIVE, true, false, WorldType.parseWorldType("BIOMESOP")));
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
