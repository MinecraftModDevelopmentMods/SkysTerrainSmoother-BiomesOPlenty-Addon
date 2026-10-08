package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class SoilRecipes {
    private SoilRecipes(){ }
    public static void register(){
        for(boolean grass:new boolean[]{false,true})for(int material=0;material<(grass?8:6);material++){
            net.minecraft.block.state.IBlockState full=NativeSoils.full(grass,material);
            ItemStack block=new ItemStack(full.getBlock(),1,full.getBlock().getMetaFromState(full));
            ItemStack slab=SoilPieces.stack(SoilPieces.state(grass,material,SoilPieces.Shape.SLAB,1),1);
            ItemStack step=SoilPieces.stack(SoilPieces.state(grass,material,SoilPieces.Shape.STEP,4),1);
            ItemStack corners=SoilPieces.stack(SoilPieces.state(grass,material,SoilPieces.Shape.CORNER,4),4);
            ItemStack slabs=slab.copy();slabs.stackSize=6;GameRegistry.addShapedRecipe(slabs,"XXX",'X',block);
            GameRegistry.addShapedRecipe(block,"XX",'X',slab);
            ItemStack steps=step.copy();steps.stackSize=4;GameRegistry.addShapedRecipe(steps,"X "," X",'X',slab);
            GameRegistry.addShapedRecipe(corners,"XX",'X',step);
        }
    }
}
