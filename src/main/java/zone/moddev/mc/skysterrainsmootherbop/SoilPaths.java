package zone.moddev.mc.skysterrainsmootherbop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Matches each soil piece to a path with the same material and orientation. */
public final class SoilPaths {
    public static final List<SoilPathBlock> BLOCKS = new ArrayList<>();

    public static void create() {
        for (SoilPieces.Shape shape : SoilPieces.Shape.values()) {
            int capacity = 16 / shape.states;
            for (int start = 0; start < 3; start += capacity) {
                BLOCKS.add(new SoilPathBlock(shape, start, Math.min(capacity, 3 - start)));
            }
        }
    }

    public static IBlockState state(int material, SoilPieces.Shape shape, int orientation) {
        for (SoilPathBlock block : BLOCKS) {
            if (block.shape == shape && material >= block.start && material < block.start + block.count) {
                return block.getStateFromMeta((material - block.start) * shape.states + orientation);
            }
        }
        throw new IllegalArgumentException("Unassigned BOP path material or shape");
    }

    public static IBlockState fromFull(IBlockState source, SoilPieces.Shape shape, int orientation) {
        IBlockState dirt = NativeSoils.dirt(source);
        if (dirt == null) {
            dirt = source;
        }
        int material = NativeSoils.index(dirt, false);
        if (material >= 0 && material < 3) {
            return state(material, shape, orientation);
        }
        if (dirt.getBlock() != Blocks.DIRT || dirt.getBlock().getMetaFromState(dirt) != 0) {
            return null;
        }
        // Origin and daisy grass use vanilla dirt and the existing Sky path pieces.
        if (shape == SoilPieces.Shape.SLAB) {
            return ModBlocks.PATH_SLAB.getStateFromMeta(orientation);
        }
        EnumFacing direction = new EnumFacing[] {
                EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST }[orientation & 3];
        IBlockState bottom = BuildingPiecesApi.bottomPiece("minecraft:grass_path",
                shape == SoilPieces.Shape.STEP ? BuildingPiecesApi.TerrainShape.STEP
                        : BuildingPiecesApi.TerrainShape.CORNER, direction);
        return bottom == null || orientation >= 4 ? bottom
                : bottom.getBlock().getStateFromMeta(bottom.getBlock().getMetaFromState(bottom) - 4);
    }

    public static boolean isFullPath(IBlockState state) {
        return state.getBlock() == NativeSoils.pathBlock || state.getBlock() == Blocks.GRASS_PATH;
    }

    private static IBlockState supportPath(IBlockState grass) {
        if (grass.getBlock() == Blocks.GRASS) {
            return Blocks.GRASS_PATH.getDefaultState();
        }
        if (grass.getBlock() != NativeSoils.grassBlock) {
            return null;
        }
        IBlockState dirt = NativeSoils.dirt(grass);
        int material = NativeSoils.index(dirt, false);
        if (material >= 0 && material < 3) {
            return NativeSoils.path(material);
        }
        return dirt.getBlock() == Blocks.DIRT && dirt.getBlock().getMetaFromState(dirt) == 0
                ? Blocks.GRASS_PATH.getDefaultState() : null;
    }

    private static IBlockState target(World world, BlockPos position, EntityPlayer player,
            ItemStack shovel, EnumFacing face) {
        if (shovel == null || shovel.stackSize <= 0 || face == null || face == EnumFacing.DOWN
                || !world.isAirBlock(position.up()) || !player.canPlayerEdit(position, face, shovel)) {
            return null;
        }
        Set<String> tools = shovel.getItem().getToolClasses(shovel);
        if (tools == null || !tools.contains("shovel")) {
            return null;
        }
        IBlockState source = world.getBlockState(position);
        if (!(source.getBlock() instanceof SoilPieceBlock)) {
            return null;
        }
        SoilPieceBlock block = (SoilPieceBlock) source.getBlock();
        return fromFull(block.full(source), block.shape, block.orientation(source));
    }

    @SubscribeEvent
    public void allowShovelUse(PlayerInteractEvent.RightClickBlock event) {
        if (target(event.getWorld(), event.getPos(), event.getEntityPlayer(),
                event.getItemStack(), event.getFace()) != null) {
            // Let block activation handle the shovel once, including when sneaking
            // or using the off hand.
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }

    public static boolean activate(World world, BlockPos position, EntityPlayer player,
            ItemStack shovel, EnumFacing face) {
        IBlockState path = target(world, position, player, shovel, face);
        if (path == null) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        SoilPieceBlock source = (SoilPieceBlock) world.getBlockState(position).getBlock();
        if (!world.setBlockState(position, path, 11)) {
            return false;
        }
        BlockPos support = position.down();
        if (source.grass && source.shape != SoilPieces.Shape.SLAB && world.isBlockLoaded(support)
                && player.canPlayerEdit(support, EnumFacing.UP, shovel)) {
            IBlockState lowerPath = supportPath(world.getBlockState(support));
            if (lowerPath != null) {
                world.setBlockState(support, lowerPath, 3);
            }
        }
        world.playSound(null, position, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1, 1);
        if (!player.capabilities.isCreativeMode) {
            shovel.damageItem(1, player);
        }
        return true;
    }

    static void checkCover(World world, BlockPos position, IBlockState state) {
        if (!world.isRemote && world.getBlockState(position.up()).getMaterial().isSolid()) {
            SoilPathBlock block = (SoilPathBlock) state.getBlock();
            world.setBlockState(position, block.dirt(state), 3);
        }
    }
}
