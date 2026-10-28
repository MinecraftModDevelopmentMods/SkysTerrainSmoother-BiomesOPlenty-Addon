package zone.moddev.mc.skysterrainsmootherbop;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

/** Native soil paths keep their footprint and lower only the exposed top. */
public final class SoilPathBlock extends Block {
    public static final PropertyBool LOWERED_SUPPORT = PropertyBool.create("lowered_support");
    private static final Material PATH_MATERIAL = new Material(MapColor.DIRT) {
        @Override
        public boolean isSolid() {
            // Native paths must not treat a partial path piece as solid cover.
            return false;
        }
    };

    public final SoilPieces.Shape shape;
    public final int start;
    public final int count;

    public SoilPathBlock(SoilPieces.Shape shape, int start, int count) {
        super(PATH_MATERIAL);
        this.shape = shape;
        this.start = start;
        this.count = count;
        String name = "path_" + shape.name().toLowerCase(Locale.ROOT) + "_"
                + String.format(Locale.ROOT, "%02d", start / (16 / shape.states));
        setRegistryName(SkysTerrainSmootherBop.ID, name);
        setUnlocalizedName(SkysTerrainSmootherBop.ID + "." + name);
        setDefaultState(blockState.getBaseState().withProperty(SoilPieceBlock.META, 0)
                .withProperty(LOWERED_SUPPORT, false));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setHardness(.65F);
        setSoundType(SoundType.PLANT);
        setHarvestLevel("shovel", 0);
        setLightOpacity(0);
        useNeighborBrightness = true;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SoilPieceBlock.META, LOWERED_SUPPORT);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(SoilPieceBlock.META, metadata & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SoilPieceBlock.META);
    }

    public int material(IBlockState state) {
        int slot = getMetaFromState(state) / shape.states;
        if (slot >= count) {
            throw new IllegalArgumentException("Unassigned path slot");
        }
        return start + slot;
    }

    public int orientation(IBlockState state) {
        return getMetaFromState(state) % shape.states;
    }

    public int canonical(IBlockState state) {
        return getMetaFromState(state) / shape.states * shape.states;
    }

    public IBlockState full(IBlockState state) {
        return NativeSoils.path(material(state));
    }

    public IBlockState dirt(IBlockState state) {
        return SoilPieces.state(false, material(state), shape, orientation(state));
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos position, EnumFacing side) {
        return false;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos position, EnumFacing side) {
        return true;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos position) {
        return state.withProperty(LOWERED_SUPPORT, SoilPaths.isFullPath(world.getBlockState(position.down())));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos position) {
        AxisAlignedBB shapeBounds = SoilPieceBlock.bounds(shape, orientation(state));
        double bottom = shapeBounds.minY;
        // A full path underneath is one pixel shorter. Extend the foot of the
        // piece so the two surfaces still meet.
        if (bottom == 0 && SoilPaths.isFullPath(world.getBlockState(position.down()))) {
            bottom -= 1.0 / 16.0;
        }
        return new AxisAlignedBB(shapeBounds.minX, bottom, shapeBounds.minZ,
                shapeBounds.maxX, shapeBounds.maxY - 1.0 / 16.0, shapeBounds.maxZ);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos position) {
        return getBoundingBox(state, world, position);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos position,
            EnumFacing side, IPlantable plant) {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos position, IBlockState state, int fortune) {
        return Collections.singletonList(SoilPieces.stack(dirt(state), 1));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Item.getItemFromBlock(dirt(state).getBlock());
    }

    @Override
    public int damageDropped(IBlockState state) {
        IBlockState dirt = dirt(state);
        return ((SoilPieceBlock) dirt.getBlock()).canonical(dirt);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos position, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> items) {
        for (int slot = 0; slot < count; slot++) {
            items.add(new ItemStack(item, 1, slot * shape.states));
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos position, IBlockState state) {
        SoilPaths.checkCover(world, position, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos position, Block changed) {
        SoilPaths.checkCover(world, position, state);
    }
}
