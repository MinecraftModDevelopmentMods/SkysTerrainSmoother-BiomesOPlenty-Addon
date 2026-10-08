package zone.moddev.mc.skysterrainsmootherbop;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.IPlantable;

/** Fixed, tile-free horizontal soil palettes. Snow is derived, never saved. */
public final class SoilPieceBlock extends Block implements IGrowable {
    public static final PropertyInteger META=PropertyInteger.create("meta",0,15);
    public final boolean grass;
    public final SoilPieces.Shape shape;
    public final int start,count;
    public SoilPieceBlock(boolean grass,SoilPieces.Shape shape,int start,int count) {
        super(grass?Material.GRASS:Material.GROUND);this.grass=grass;this.shape=shape;this.start=start;this.count=count;
        String id=(grass?"grass_":"dirt_")+shape.name().toLowerCase(Locale.ROOT)+"_"+String.format(Locale.ROOT,"%02d",start/(16/shape.states));
        setRegistryName(SkysTerrainSmootherBop.ID,id);setUnlocalizedName(SkysTerrainSmootherBop.ID+"."+id);
        setDefaultState(blockState.getBaseState().withProperty(META,0).withProperty(BlockGrass.SNOWY,false));
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);setTickRandomly(true);setLightOpacity(0);useNeighborBrightness=true;
        setHardness(grass?.6f:.5f);setSoundType(grass?SoundType.PLANT:SoundType.GROUND);setHarvestLevel("shovel",0);
    }
    @Override protected BlockStateContainer createBlockState(){return new BlockStateContainer(this,META,BlockGrass.SNOWY);}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(META,meta&15);}
    @Override public int getMetaFromState(IBlockState state){return state.getValue(META);}
    public int material(IBlockState state){int slot=getMetaFromState(state)/shape.states;if(slot>=count)throw new IllegalArgumentException("Unassigned soil slot");return start+slot;}
    public int orientation(IBlockState state){return getMetaFromState(state)%shape.states;}
    public int canonical(IBlockState state){return getMetaFromState(state)/shape.states*shape.states;}
    public IBlockState full(IBlockState state){return NativeSoils.full(grass,material(state));}
    public int mask(IBlockState state){return mask(shape,orientation(state));}
    public static int mask(SoilPieces.Shape shape,int orientation){
        if(shape==SoilPieces.Shape.SLAB)return orientation==0?240:15;
        int lower=shape==SoilPieces.Shape.STEP?new int[]{3,6,12,9}[orientation&3]:1<<(orientation&3);
        return orientation<4?lower<<4:lower;
    }
    @Override public boolean isOpaqueCube(IBlockState state){return false;}
    @Override public boolean isFullCube(IBlockState state){return false;}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side){return shape==SoilPieces.Shape.SLAB&&side==(orientation(state)==0?EnumFacing.UP:EnumFacing.DOWN);}
    @Override public boolean doesSideBlockRendering(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side){return isSideSolid(state,world,pos,side);}
    @Override public boolean shouldSideBeRendered(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side){return true;}
    @Override public BlockRenderLayer getBlockLayer(){return grass?BlockRenderLayer.CUTOUT_MIPPED:BlockRenderLayer.SOLID;}
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos){
        int orientation=orientation(state),direction=orientation&3;double minX=0,minZ=0,maxX=1,maxZ=1;
        boolean top=shape==SoilPieces.Shape.SLAB?orientation==0:orientation<4;
        if(shape==SoilPieces.Shape.STEP){if(direction==0)maxZ=.5;if(direction==1)minX=.5;if(direction==2)minZ=.5;if(direction==3)maxX=.5;}
        if(shape==SoilPieces.Shape.CORNER){minX=direction==1||direction==2?.5:0;minZ=direction>=2?.5:0;maxX=minX+.5;maxZ=minZ+.5;}
        return new AxisAlignedBB(minX,top?.5:0,minZ,maxX,top?1:.5,maxZ);
    }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,World world,BlockPos pos){return getBoundingBox(state,world,pos);}
    @Override public IBlockState getActualState(IBlockState state,IBlockAccess world,BlockPos pos){
        boolean snow=false;for(EnumFacing face:new EnumFacing[]{EnumFacing.UP,EnumFacing.NORTH,EnumFacing.EAST,EnumFacing.SOUTH,EnumFacing.WEST}){
            Block block=world.getBlockState(pos.offset(face)).getBlock();snow|=block==Blocks.SNOW||block==Blocks.SNOW_LAYER;
        }
        return state.withProperty(BlockGrass.SNOWY,snow);
    }
    @Override public void getSubBlocks(Item item,CreativeTabs tab,List<ItemStack> items){for(int slot=0;slot<count;slot++)items.add(new ItemStack(item,1,slot*shape.states));}
    @Override public int damageDropped(IBlockState state){return canonical(state);}
    @Override public List<ItemStack> getDrops(IBlockAccess world,BlockPos pos,IBlockState state,int fortune){
        IBlockState drop=grass?SoilPieces.matching(NativeSoils.dirt(full(state)),shape,orientation(state)):state;
        ItemStack stack=SoilPieces.stack(drop,1);return stack==null?Collections.emptyList():Collections.singletonList(stack);
    }
    @Override protected boolean canSilkHarvest(){return true;}
    @Override protected ItemStack getSilkTouchDrop(IBlockState state){return SoilPieces.stack(state,1);}
    @Override public Item getItemDropped(IBlockState state,Random random,int fortune){return getDrops(null,BlockPos.ORIGIN,state,fortune).get(0).getItem();}
    @Override public void onBlockAdded(World world,BlockPos pos,IBlockState state){SoilLifecycle.repairSupport(world,pos,state);}
    @Override public void neighborChanged(IBlockState state,World world,BlockPos pos,Block changed){SoilLifecycle.repairSupport(world,pos,state);}
    @Override public void updateTick(World world,BlockPos pos,IBlockState state,Random random){SoilLifecycle.tick(world,pos,state,random);}
    @Override public boolean canSustainPlant(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing face,IPlantable plant){return face==EnumFacing.UP&&shape==SoilPieces.Shape.SLAB&&orientation(state)==0&&full(state).getBlock().canSustainPlant(full(state),new NativeBlockView(world,pos,full(state)),pos,face,plant);}
    @Override public boolean canGrow(World world,BlockPos pos,IBlockState state,boolean client){IBlockState full=full(state);return grass&&shape==SoilPieces.Shape.SLAB&&orientation(state)==0&&full.getBlock() instanceof IGrowable&&((IGrowable)full.getBlock()).canGrow(world,pos,full,client);}
    @Override public boolean canUseBonemeal(World world,Random random,BlockPos pos,IBlockState state){return canGrow(world,pos,state,world.isRemote);}
    @Override public void grow(World world,Random random,BlockPos pos,IBlockState state){if(canGrow(world,pos,state,world.isRemote))((IGrowable)full(state).getBlock()).grow(world,random,pos,full(state));}
}
