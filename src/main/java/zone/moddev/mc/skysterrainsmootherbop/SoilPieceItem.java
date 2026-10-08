package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public final class SoilPieceItem extends ItemBlock {
    private final SoilPieceBlock piece;
    public SoilPieceItem(SoilPieceBlock block){super(block);piece=block;setHasSubtypes(true);setMaxDamage(0);}
    @Override public int getMetadata(int damage){return damage;}
    @Override public boolean canPlaceBlockOnSide(World world,BlockPos pos,EnumFacing side,EntityPlayer player,ItemStack stack){return player.canPlayerEdit(pos,side,stack);}
    @Override public String getItemStackDisplayName(ItemStack stack){
        IBlockState full=piece.full(piece.getStateFromMeta(stack.getItemDamage()));
        String name=new ItemStack(full.getBlock(),1,full.getBlock().getMetaFromState(full)).getDisplayName();
        return I18n.translateToLocalFormatted("pieces.skysterrainsmootherbop."+piece.shape.name().toLowerCase(java.util.Locale.ROOT),name);
    }
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing face,float x,float y,float z){
        if(stack==null||stack.stackSize<=0)return EnumActionResult.FAIL;
        int material=piece.material(piece.getStateFromMeta(stack.getItemDamage()));
        IBlockState old=world.getBlockState(pos);
        if(!old.getBlock().isReplaceable(world,pos)){
            if(combine(stack,player,world,pos,old,material,face,x,y,z))return EnumActionResult.SUCCESS;
            pos=pos.offset(face);old=world.getBlockState(pos);
            x-=face.getFrontOffsetX();y-=face.getFrontOffsetY();z-=face.getFrontOffsetZ();
        }
        if(!player.canPlayerEdit(pos,face,stack))return EnumActionResult.FAIL;
        if(!old.getBlock().isReplaceable(world,pos))return combine(stack,player,world,pos,old,material,face,x,y,z)?EnumActionResult.SUCCESS:EnumActionResult.FAIL;
        boolean top=face==EnumFacing.DOWN||face.getAxis()!=EnumFacing.Axis.Y&&y>.5;
        int orientation=piece.shape==SoilPieces.Shape.SLAB?(top?0:1):(piece.shape==SoilPieces.Shape.STEP?edge(x,z):corner(x,z))+(top?0:4);
        IBlockState target=SoilPieces.state(piece.grass,material,piece.shape,orientation);
        if(!world.canBlockBePlaced(piece,pos,true,face,player,stack)||!world.checkNoEntityCollision(target.getCollisionBoundingBox(world,pos).offset(pos)))return EnumActionResult.FAIL;
        if(!world.setBlockState(pos,target,3))return EnumActionResult.FAIL;finish(stack,player,world,pos);return EnumActionResult.SUCCESS;
    }
    private boolean combine(ItemStack stack,EntityPlayer player,World world,BlockPos pos,IBlockState old,int material,EnumFacing face,float x,float y,float z){
        if(!(old.getBlock() instanceof SoilPieceBlock)||!player.canPlayerEdit(pos,face,stack))return false;
        SoilPieceBlock other=(SoilPieceBlock)old.getBlock();if(other.grass!=piece.grass||other.material(old)!=material)return false;
        x+=face.getFrontOffsetX()*.001f;y+=face.getFrontOffsetY()*.001f;z+=face.getFrontOffsetZ()*.001f;
        boolean top=y>=.5;int orientation=piece.shape==SoilPieces.Shape.SLAB?(top?0:1):(piece.shape==SoilPieces.Shape.STEP?edge(x,z):corner(x,z))+(top?0:4);
        int added=SoilPieceBlock.mask(piece.shape,orientation),before=other.mask(old);if((added&before)!=0)return false;
        int union=added|before;IBlockState target=null;
        if(union==255)target=NativeSoils.full(piece.grass,material);
        else for(SoilPieces.Shape shape:SoilPieces.Shape.values())for(int o=0;o<shape.states;o++)if(SoilPieceBlock.mask(shape,o)==union)target=SoilPieces.state(piece.grass,material,shape,o);
        if(target==null||!world.checkNoEntityCollision(target.getCollisionBoundingBox(world,pos).offset(pos))||!world.setBlockState(pos,target,3))return false;
        finish(stack,player,world,pos);return true;
    }
    private void finish(ItemStack stack,EntityPlayer player,World world,BlockPos pos){world.playSound(player,pos,piece.getSoundType().getPlaceSound(),SoundCategory.BLOCKS,1,.8f);if(!player.capabilities.isCreativeMode)--stack.stackSize;}
    private static int corner(float x,float z){return z<.5?(x<.5?0:1):(x<.5?3:2);}
    private static int edge(float x,float z){float[] distance={z,1-x,1-z,x};int best=0;for(int i=1;i<4;i++)if(distance[i]<distance[best])best=i;return best;}
}
