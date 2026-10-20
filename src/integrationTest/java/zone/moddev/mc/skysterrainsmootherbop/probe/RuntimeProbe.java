package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.io.*;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.block.GrassSpread;
import zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother;
import zone.moddev.mc.skysterrainsmoother.api.TerrainMaterial;
import zone.moddev.mc.skysterrainsmoother.internal.*;
import zone.moddev.mc.skysterrainsmootherbop.*;

/** Test-only mod; no probe classes are exposed to ordinary launches or release jars. */
@Mod(
        modid = "bopterrainprobe",
        name = "BOP Terrain Runtime Probe",
        version = "1",
        dependencies = "required-after:skysterrainsmootherbop")
public final class RuntimeProbe {
    @SidedProxy(
            clientSide = "zone.moddev.mc.skysterrainsmootherbop.probe.ClientProbe",
            serverSide = "zone.moddev.mc.skysterrainsmootherbop.probe.ProbeProxy")
    public static ProbeProxy proxy;
    private MinecraftServer server;
    private boolean ran;

    @Mod.EventHandler
    public void pre(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        proxy.init();
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {
        server = FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (server == null || ran || event.phase != TickEvent.Phase.END) {
            return;
        }

        ran = true;

        try {
            WorldServer world = server.getWorld(0);

            world.getGameRules().setOrCreateGameRule("randomTickSpeed", "0");
            world.setWorldTime(6000);

            int checks = content(world) + placement(world) + recipes(world) + footprints(world) + nativeGrass(world)
                    + persistence(world) + generationOrder(world);
            int generated = generation(world);
            File marker = new File(world.getSaveHandler().getWorldDirectory(),
                    "skysterrainsmootherbop-integration.properties");
            Properties values = new Properties();

            if (marker.isFile()) {
                try (InputStream input = new FileInputStream(marker)) {
                    values.load(input);
                }
            }

            values.setProperty(phase() + "_complete", "true");
            values.setProperty("gameplay_checks", Integer.toString(checks));
            values.setProperty("generation_checks", Integer.toString(generated));

            try (OutputStream output = new FileOutputStream(marker)) {
                values.store(output, "BOP terrain qualification");
            }

            System.out.println("BOP_TERRAIN_RUNTIME_PASS phase=" + phase() + " level=" + SkysTerrainSmoother.level
                    + " checks=" + checks + " generated=" + generated);
            ClientStatus.complete = true;
        } catch (Throwable failure) {
            failure.printStackTrace();
            throw new IllegalStateException("BOP terrain probe failed", failure);
        }
        if (!System.getProperty("skysterrainsmootherbop.integrationPhase", "fresh").startsWith("client-")) {
            server.initiateShutdown();
        }
    }

    private static String phase() {
        return System.getProperty("skysterrainsmootherbop.integrationPhase", "fresh").replace("client-", "");
    }

    private static BlockPos position(WorldServer world, int blockX, int blockY, int blockZ) {
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);

        world.getChunk(pos);
        world.getChunk(pos.add(8, 0, 8));
        world.getChunk(pos.add(-8, 0, -8));
        return pos;
    }

    private static int content(WorldServer world) {
        BlockPos position = position(world, 80, 220, 80);
        int checks = 0;
        int states = 0;

        for (SoilPieceBlock block : SoilPieces.BLOCKS) {
            for (int slot = 0; slot < block.count; slot++) {
                for (int orientation = 0; orientation < block.shape.states; orientation++) {
                    IBlockState state = block.getStateFromMeta(slot * block.shape.states + orientation);

                    require(block.getMetaFromState(state) == slot * block.shape.states + orientation,
                            "fixed metadata round trip");
                    world.setBlockToAir(position.up());
                    world.setBlockToAir(position.north());
                    world.setBlockState(position.down(), Blocks.STONE.getDefaultState(), 2);
                    world.setBlockState(position, state, 2);

                    AxisAlignedBB boundingBox = state.getBoundingBox(world, position);
                    double volume = (boundingBox.maxX - boundingBox.minX) * (boundingBox.maxY - boundingBox.minY)
                            * (boundingBox.maxZ - boundingBox.minZ);

                    require(volume == (block.shape == SoilPieces.Shape.SLAB ? .5
                            : block.shape == SoilPieces.Shape.STEP ? .25 : .125), "exact partial volume");

                    for (EnumFacing face : EnumFacing.values()) {
                        require(block.shouldSideBeRendered(state, world, position, face),
                                "internal exposed face remains visible");
                    }

                    world.setBlockState(position.north(), Blocks.SNOW.getDefaultState(), 2);

                    IBlockState actual = block.getActualState(state, world, position);

                    require(actual.getValue(BlockGrass.SNOWY)
                            && block.getMetaFromState(actual) == block.getMetaFromState(state),
                            "snow is nonpersistent");
                    world.setBlockToAir(position.north());
                    require(!block.getActualState(state, world, position).getValue(BlockGrass.SNOWY),
                            "visual cap leaves with snow");

                    boolean complete = block.shape == SoilPieces.Shape.SLAB && orientation == 0;

                    for (Block plant : new Block[] { Blocks.YELLOW_FLOWER, Blocks.TALLGRASS }) {
                        IBlockState full = block.full(state);
                        boolean nativeSupport = full.getBlock().canSustainPlant(full,
                                new net.minecraft.world.ChunkCache(world, position.add(-2, -2, -2),
                                        position.add(2, 2, 2), 0) {
                                    @Override
                                    public IBlockState getBlockState(BlockPos targetPosition) {
                                        return targetPosition.equals(position) ? full
                                                : super.getBlockState(targetPosition);
                                    }
                                }, position, EnumFacing.UP, (IPlantable) plant);

                        require(block.canSustainPlant(state, world, position, EnumFacing.UP,
                                (IPlantable) plant) == (complete && nativeSupport),
                                "plant support uses native properties and a complete upper face");
                        checks++;
                    }

                    ItemStack expected = SoilPieces.stack(block.grass
                            ? SoilPieces.matching(NativeSoils.dirt(block.full(state)), block.shape, orientation)
                            : state, 1);

                    require(ItemStack.areItemStacksEqual(block.getDrops(world, position, state, 0).get(0), expected),
                            "native dirt drop preserves material and geometry");

                    if (block.grass) {
                        IBlockState full = block.full(state);

                        world.setBlockState(position.down(), full, 2);
                        block.neighborChanged(state, world, position, full.getBlock(), position.down());
                        require(world.getBlockState(position.down()).equals(NativeSoils.dirt(full)),
                                "native grass support dirtified");
                        world.setBlockState(position.up(), Blocks.STONE.getDefaultState(), 2);
                        world.setLightFor(EnumSkyBlock.SKY, position.up(), 0);
                        world.setLightFor(EnumSkyBlock.BLOCK, position.up(), 0);
                        block.updateTick(world, position, state, new Random(1));
                        require(world.getBlockState(position)
                                .equals(SoilPieces.matching(NativeSoils.dirt(full), block.shape, orientation)),
                                "covered decay keeps shape and orientation");
                        world.setBlockToAir(position.up());
                        world.setBlockState(position, state, 2);

                        if (block.canGrow(world, position, state, false)) {
                            block.grow(world, new Random(1), position, state);
                            require(world.getBlockState(position).equals(state),
                                    "bonemeal does not replace its partial support");
                        }
                    }

                    checks += block.grass ? 15 : 13;
                    states++;
                }
            }
        }

        require(states == 252 && SoilPieces.BLOCKS.size() == 16, "measured palette budget");
        System.out.println("BOP_TERRAIN_CONTENT_PASS states=" + states + " blocks=" + SoilPieces.BLOCKS.size());
        return checks;
    }

    private static InventoryCrafting grid(int size) {
        return new InventoryCrafting(new Container() {
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, size, size);
    }

    private static int placement(WorldServer world) {
        BlockPos position = position(world, 160, 220, 160);
        int checks = 0;
        FakePlayer player = FakePlayerFactory.get(world,
                new GameProfile(UUID.fromString("fd91e970-f44c-4b48-95ce-825342755b00"), "SoilPieceProbe"));

        player.setPosition(164, 220, 164);
        player.capabilities.isCreativeMode = false;

        for (boolean grass : new boolean[] { false, true }) {
            for (int m = 0; m < (grass ? 8 : 6); m++) {
                for (SoilPieces.Shape shape : SoilPieces.Shape.values()) {
                    world.setBlockState(position.down(), Blocks.STONE.getDefaultState(), 2);
                    world.setBlockToAir(position);
                    world.setBlockToAir(position.up());

                    ItemStack stack = SoilPieces
                            .stack(SoilPieces.state(grass, m, shape, shape == SoilPieces.Shape.SLAB ? 1 : 4), 8);

                    player.setHeldItem(EnumHand.MAIN_HAND, stack);
                    require(stack.onItemUse(player, world, position.down(), EnumHand.MAIN_HAND, EnumFacing.UP, .1f, 1,
                            .1f) == EnumActionResult.SUCCESS, "real lower piece placement");
                    require(world.getBlockState(position)
                            .equals(SoilPieces.state(grass, m, shape, shape == SoilPieces.Shape.SLAB ? 1 : 4)),
                            "placement uses clicked footprint");
                    require(stack.getCount() == 7, "placement consumes one item");
                    checks += 3;

                    if (shape == SoilPieces.Shape.SLAB) {
                        require(stack.onItemUse(player, world, position, EnumHand.MAIN_HAND, EnumFacing.UP, .1f, .5f,
                                .1f) == EnumActionResult.SUCCESS, "complementary slab combines");
                        require(world.getBlockState(position).equals(NativeSoils.full(grass, m))
                                && stack.getCount() == 6, "combining preserves native full identity and count");
                        checks += 2;
                    } else {
                        // Add the adjacent quarter/eighth into the same lower half.
                        EnumFacing face = EnumFacing.SOUTH;
                        float hitX = .5f;
                        float hitZ = .5f;

                        if (shape == SoilPieces.Shape.CORNER) {
                            face = EnumFacing.EAST;
                            hitX = .5f;
                            hitZ = .1f;
                        }

                        require(stack.onItemUse(player, world, position, EnumHand.MAIN_HAND, face, hitX, .25f,
                                hitZ) == EnumActionResult.SUCCESS, "complementary fine piece combines");

                        IBlockState expected = SoilPieces.state(grass, m,
                                shape == SoilPieces.Shape.STEP ? SoilPieces.Shape.SLAB : SoilPieces.Shape.STEP,
                                shape == SoilPieces.Shape.STEP ? 1 : 4);

                        require(world.getBlockState(position).equals(expected) && stack.getCount() == 6,
                                "fine combination retains native material");
                        checks += 2;
                    }
                }
            }
        }

        return checks;
    }

    private static void recipe(WorldServer world, ItemStack input, String[] rows, ItemStack expected) {
        InventoryCrafting grid = grid(3);

        for (int blockY = 0; blockY < rows.length; blockY++) {
            for (int blockX = 0; blockX < rows[blockY].length(); blockX++) {
                if (rows[blockY].charAt(blockX) == 'X') {
                    grid.setInventorySlotContents(blockX + blockY * 3, input.copy());
                }
            }
        }

        require(ItemStack.areItemStacksEqual(expected, CraftingManager.findMatchingResult(grid, world)),
                "crafting manager result " + input + " " + Arrays.toString(rows));

        for (IRecipe candidate : net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES.getValuesCollection()) {
            if (candidate.matches(grid, world)) {
                require(ItemStack.areItemStacksEqual(expected, candidate.getCraftingResult(grid)),
                        "no conflicting installed recipe " + candidate.getClass().getName());
            }
        }
    }

    private static int recipes(WorldServer world) {
        int checks = 0;

        for (boolean grass : new boolean[] { false, true }) {
            for (int material = 0; material < (grass ? 8 : 6); material++) {
                IBlockState full = NativeSoils.full(grass, material);
                ItemStack nativeStack = new ItemStack(full.getBlock(), 1, full.getBlock().getMetaFromState(full));
                ItemStack slab = SoilPieces.stack(SoilPieces.state(grass, material, SoilPieces.Shape.SLAB, 1), 1);
                ItemStack step = SoilPieces.stack(SoilPieces.state(grass, material, SoilPieces.Shape.STEP, 4), 1);

                ItemStack result = slab.copy();

                result.setCount(6);
                recipe(world, nativeStack, new String[] { "XXX" }, result);
                recipe(world, slab, new String[] { "XX" }, nativeStack);
                result = step.copy();
                result.setCount(4);
                recipe(world, slab, new String[] { "X ", " X" }, result);
                recipe(world, step, new String[] { "XX" },
                        SoilPieces.stack(SoilPieces.state(grass, material, SoilPieces.Shape.CORNER, 4), 4));
                checks += 4;
            }
        }

        System.out.println("BOP_TERRAIN_RECIPE_PASS recipes=" + checks);
        return checks;
    }

    private static int footprints(WorldServer world) {
        BlockPos position = position(world, 100, 220, 100);
        Chunk owner = world.getChunk(position);
        int checks = 0;

        for (boolean grass : new boolean[] { false, true }) {
            for (int material = 0; material < (grass ? 8 : 6); material++) {
                for (int level = 1; level <= 3; level++) {
                    for (int cardinal = 0; cardinal < 16; cardinal++) {
                        for (int blockZ = -2; blockZ <= 2; blockZ++) {
                            for (int blockX = -2; blockX <= 2; blockX++) {
                                for (int blockY = 220; blockY <= 223; blockY++) {
                                    world.setBlockToAir(position.add(blockX, blockY - 220, blockZ));
                                }
                            }
                        }

                        IBlockState full = NativeSoils.full(grass, material);

                        world.setBlockState(position, full, 2);

                        int[] offsetsX = { 0, 1, 0, -1 };
                        int[] offsetsZ = { -1, 0, 1, 0 };

                        for (int directionIndex = 0; directionIndex < 4; directionIndex++) {
                            if ((cardinal & (1 << directionIndex)) != 0) {
                                world.setBlockState(position.add(offsetsX[directionIndex], 1, offsetsZ[directionIndex]),
                                        Blocks.STONE.getDefaultState(), 2);
                            }
                        }
                        if (level == 3) {
                            world.setBlockState(position.north().west().up(), Blocks.STONE.getDefaultState(), 2);
                        }

                        TerrainMaterial mapping = MaterialCatalogue.instance().source(full);
                        IBlockState expected = MaterialCatalogue.instance().result(mapping,
                                Footprint.mask(cardinal, level == 3 ? 1 : 0, level), level);

                        SkysTerrainSmoother.engine.smooth(world, owner, level);
                        require(world.getBlockState(position.up())
                                .equals(expected == null ? Blocks.AIR.getDefaultState() : expected),
                                "BOP footprint preserves variant " + grass + "/" + material + "/" + level + "/"
                                        + cardinal);

                        if (grass && expected != null) {
                            require(world.getBlockState(position).equals(NativeSoils.dirt(full)),
                                    "generated grass keeps native soil");
                        }

                        checks++;
                    }
                }
            }
        }

        return checks;
    }

    private static int nativeGrass(WorldServer world) {
        BlockPos position = position(world, 140, 220, 140);
        int checks = 0;

        for (int source = 0; source < 8; source++) {
            for (int soil = 0; soil < 6; soil++) {
                IBlockState dirt = SoilPieces.state(false, soil, SoilPieces.Shape.CORNER, 6);
                IBlockState full = NativeSoils.full(true, source);
                IBlockState nativeResult = NativeSoils.spread(full, NativeSoils.full(false, soil));

                world.setBlockToAir(position.up());
                world.setBlockState(position, dirt, 2);
                world.setLightFor(EnumSkyBlock.SKY, position.up(), 15);

                boolean changed = SoilLifecycle.grow(world, position, full);

                require(changed == (nativeResult != null), "native special/coarse spread policy");

                if (nativeResult != null) {
                    require(world.getBlockState(position)
                            .equals(SoilPieces.matching(nativeResult, SoilPieces.Shape.CORNER, 6)),
                            "spread keeps native grass family and orientation");
                }

                world.setBlockState(position, dirt, 2);
                world.setBlockState(position.up(), SoilPieces.state(true, 2, SoilPieces.Shape.SLAB, 1), 2);
                require(!SoilLifecycle.grow(world, position, full) && !GrassSpread.growTarget(world, position),
                        "covered BOP target rejected by both paths");
                checks += 3;
            }
        }
        for (int source : new int[] { 5, 7 }) {
            for (SoilPieces.Shape shape : SoilPieces.Shape.values()) {
                IBlockState full = NativeSoils.full(true, source);
                int orientation = shape == SoilPieces.Shape.SLAB ? 1 : 6;
                IBlockState dirt = SoilPieces.matching(Blocks.DIRT.getDefaultState(), shape, orientation);

                require(GrassSlabsApi.grassFor(dirt, full).equals(SoilPieces.state(true, source, shape, orientation)),
                        "shared spread preserves Origin/Daisy family and orientation");
                world.setBlockToAir(position.up());
                world.setBlockState(position, dirt, 2);
                world.setLightFor(EnumSkyBlock.SKY, position.up(), 15);
                require(SoilLifecycle.grow(world, position, full)
                        && world.getBlockState(position).equals(SoilPieces.state(true, source, shape, orientation)),
                        "real cross-mod dirt growth keeps BOP identity");
                checks += 2;
            }
        }

        world.setBlockState(position.down(), NativeSoils.full(true, 2), 2);
        world.setBlockState(position, zone.moddev.mc.skysgrassslabs.init.ModBlocks.GRASS_SLAB.getStateFromMeta(1), 3);
        require(world.getBlockState(position.down()).equals(NativeSoils.full(false, 0)),
                "Sky grass slab dirtifies BOP support through the optional rule");
        checks++;

        for (int special : new int[] { 0, 1, 6 }) {
            require(!GrassSlabsApi.isSpreadingGrassForm(NativeSoils.full(true, special)),
                    "nonspreading native special surface stays nonspreading");
        }

        checks += 3;
        return checks;
    }

    private static int persistence(WorldServer world) {
        int index = 0;

        for (SoilPieceBlock block : SoilPieces.BLOCKS) {
            for (int slot = 0; slot < block.count; slot++) {
                for (int orientation = 0; orientation < block.shape.states; orientation++) {
                    BlockPos pos = position(world, 2000 + index, 100, 2000);
                    IBlockState state = block.getStateFromMeta(slot * block.shape.states + orientation);

                    index++;

                    if (phase().equals("reload")) {
                        require(world.getBlockState(pos).equals(state), "saved soil identity and metadata");
                    } else {
                        world.setBlockState(pos, state, 2);
                    }
                }
            }
        }

        BlockPos pos = position(world, 2000, 105, 2000);

        if (!phase().equals("reload")) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);

            ItemStack stack = SoilPieces.stack(SoilPieces.state(true, 7, SoilPieces.Shape.SLAB, 1), 37);
            NBTTagCompound tag = new NBTTagCompound();

            tag.setString("custom_label", "BOP terrain fixture");
            stack.setTagCompound(tag);
            ((TileEntityChest) world.getTileEntity(pos)).setInventorySlotContents(0, stack);
        } else {
            ItemStack stack = ((TileEntityChest) world.getTileEntity(pos)).getStackInSlot(0);

            require(stack.getCount() == 37
                    && stack.getTagCompound().getString("custom_label").equals("BOP terrain fixture"),
                    "saved counts and item NBT");
        }

        return index + 1;
    }

    private static int generation(WorldServer world) {
        FakePlayer player = FakePlayerFactory.get(world,
                new GameProfile(UUID.fromString("6ba324bf-9731-4e8b-98cd-885e882bd3e1"), "BopTerrainProbe"));

        world.getPlayerChunkMap().setPlayerViewRadius(4);

        int found = 0;

        for (int attempt = 0; attempt < 10; attempt++) {
            int chunkX = 400 + attempt * 160 + (phase().equals("reload") ? 2000 : 0);
            int chunkZ = 400 + attempt * 40;

            player.setPosition(chunkX * 16 + 8, 100, chunkZ * 16 + 8);
            world.getPlayerChunkMap().addPlayer(player);

            try {
                for (int tick = 0; tick < 100; tick++) {
                    world.getPlayerChunkMap().tick();
                    world.getChunkProvider().tick();
                }
                for (int blockZ = chunkZ - 4; blockZ <= chunkZ + 4; blockZ++) {
                    for (int blockX = chunkX - 4; blockX <= chunkX + 4; blockX++) {
                        Chunk chunk = world.getChunkProvider().getLoadedChunk(blockX, blockZ);

                        if (chunk == null) {
                            continue;
                        }
                        for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
                            if (section != null) {
                                for (int blockY = 0; blockY < 16; blockY++) {
                                    for (int localZ = 0; localZ < 16; localZ++) {
                                        for (int localX = 0; localX < 16; localX++) {
                                            IBlockState state = section.get(localX, blockY, localZ);

                                            if (!(state.getBlock() instanceof SoilPieceBlock)) {
                                                continue;
                                            }

                                            TerrainMaterial material = MaterialCatalogue.instance().piece(state);
                                            BlockPos pos = new BlockPos((blockX << 4) + localX,
                                                    section.getYLocation() + blockY, (blockZ << 4) + localZ);
                                            IBlockState support = chunk.getBlockState(pos.down());

                                            require(material.sources.contains(support)
                                                    || support.equals(material.supportAfterPlacement),
                                                    "generated BOP piece retains correct support");
                                            found++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                world.getPlayerChunkMap().removePlayer(player);
            }
            if (found > 0) {
                break;
            }
        }

        require(found > 0,
                "normal player-tracked BOP generation produced no eligible pieces in bounded candidate regions");
        return found;
    }

    private static int generationOrder(WorldServer world) throws Exception {
        for (int level = 1; level <= 3; level++) {
            byte[][] hashes = new byte[2][];

            for (int order = 0; order < 2; order++) {
                int origin = order == 0 ? 128 : 256;
                List<Chunk> chunks = new ArrayList<>();

                for (int blockZ = 0; blockZ < 9; blockZ++) {
                    for (int blockX = 0; blockX < 9; blockX++) {
                        chunks.add(world.getChunk(origin + blockX, 128 + blockZ));
                    }
                }
                for (int blockZ = 0; blockZ < 144; blockZ++) {
                    for (int blockX = 0; blockX < 144; blockX++) {
                        int height = 200 + Math.floorMod(blockX * 31 + blockZ * 17 + (blockX >> 3) * (blockZ >> 3), 3);
                        int material = Math.floorMod(blockX * 7 + blockZ * 11, 14);

                        Chunk chunk = chunks.get((blockX >> 4) + (blockZ >> 4) * 9);
                        ExtendedBlockStorage section = chunk.getBlockStorageArray()[12];

                        if (section == null) {
                            chunk.getBlockStorageArray()[12] = section = new ExtendedBlockStorage(192, true);
                        }
                        for (int blockY = 200; blockY < 205; blockY++) {
                            section.set(blockX & 15, blockY & 15, blockZ & 15, Blocks.AIR.getDefaultState());
                        }

                        section.set(blockX & 15, height & 15, blockZ & 15,
                                NativeSoils.full(material < 8, material < 8 ? material : material - 8));
                        chunk.getHeightMap()[(blockX & 15) | ((blockZ & 15) << 4)] = height + 1;
                    }
                }
                if (order == 1) {
                    Collections.reverse(chunks);
                }
                for (Chunk chunk : chunks) {
                    SkysTerrainSmoother.engine.smooth(world, chunk, level);
                }

                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");

                for (int blockZ = 0; blockZ < 144; blockZ++) {
                    for (int blockX = 0; blockX < 144; blockX++) {
                        for (int blockY = 200; blockY < 205; blockY++) {
                            IBlockState state = world
                                    .getBlockState(new BlockPos((origin << 4) + blockX, blockY, (128 << 4) + blockZ));

                            digest.update((state.getBlock().getRegistryName() + "/"
                                    + state.getBlock().getMetaFromState(state) + "\n").getBytes("UTF-8"));
                        }
                    }
                }

                hashes[order] = digest.digest();
            }

            require(Arrays.equals(hashes[0], hashes[1]), "mixed BOP 9x9 generation order differs at level " + level);

            StringBuilder hexDigest = new StringBuilder();

            for (byte value : hashes[0]) {
                hexDigest.append(String.format("%02X", value & 255));
            }

            System.out.println("BOP_TERRAIN_GENERATION_ORDER_PASS level=" + level + " chunks=81 sha256=" + hexDigest);
        }

        return 486;
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
