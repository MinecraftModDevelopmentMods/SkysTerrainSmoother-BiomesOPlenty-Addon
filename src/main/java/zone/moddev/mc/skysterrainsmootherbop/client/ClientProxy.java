package zone.moddev.mc.skysterrainsmootherbop.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.item.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.*;
import zone.moddev.mc.skysterrainsmootherbop.*;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @Override public void preInit(){
        for(SoilPieceBlock block:SoilPieces.BLOCKS)for(int slot=0;slot<block.count;slot++)ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),slot*block.shape.states,
                new ModelResourceLocation(block.getRegistryName(),"meta="+(slot*block.shape.states+(block.shape==SoilPieces.Shape.SLAB?1:4))+",snowy=false"));
    }
    @Override public void init(){
        Minecraft minecraft=Minecraft.getMinecraft();
        IBlockColor nativeColour=(state,world,pos,tint)->{
            SoilPieceBlock block=(SoilPieceBlock)state.getBlock();
            return minecraft.getBlockColors().colorMultiplier(block.full(state),world,pos,tint);
        };
        for(SoilPieceBlock block:SoilPieces.BLOCKS)if(block.grass){
            minecraft.getBlockColors().registerBlockColorHandler(nativeColour,block);
            minecraft.getItemColors().registerItemColorHandler((stack,tint)->minecraft.getBlockColors().colorMultiplier(block.full(block.getStateFromMeta(stack.getItemDamage())),null,null,tint),Item.getItemFromBlock(block));
        }
    }
}
