package zone.moddev.mc.skysterrainsmootherbop;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

/** Uses the installed mod's soil rules; no BOP implementation or assets are copied. */
public final class NativeSoils {
    public static final String[] GRASS_NAMES={"spectral_moss","overgrown_stone","loamy","sandy","silty","origin","overgrown_netherrack","daisy"};
    public static final String[] DIRT_NAMES={"loamy","sandy","silty","coarse_loamy","coarse_sandy","coarse_silty"};
    private static Method dirt,spread;
    public static Block grassBlock,dirtBlock;
    private NativeSoils() { }
    public static void initialize() {
        grassBlock=required("grass"); dirtBlock=required("dirt");
        try {
            Class<?> type=Class.forName("biomesoplenty.common.block.BlockBOPGrass");
            dirt=type.getMethod("getDirtBlockState",IBlockState.class);
            spread=type.getMethod("spreadsToGrass",IBlockState.class,IBlockState.class);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("The installed BOP grass API is incompatible",failure);}
    }
    private static Block required(String path) {
        Block block=Block.REGISTRY.getObject(new ResourceLocation("biomesoplenty",path));
        if(block==null||block==Blocks.AIR)throw new IllegalStateException("Missing BOP terrain block: "+path);
        return block;
    }
    public static IBlockState full(boolean grass,int material) { return grass?grassBlock.getStateFromMeta(material):dirtBlock.getStateFromMeta(material<3?material:material-3+8); }
    public static int index(IBlockState full,boolean grass) {
        if(full.getBlock()!=(grass?grassBlock:dirtBlock))return -1;
        int meta=full.getBlock().getMetaFromState(full);
        return grass?meta&7:(meta&7)<3?(meta&7)+((meta&8)==0?0:3):-1;
    }
    public static IBlockState dirt(IBlockState grass) {
        if(grass.getBlock()==Blocks.GRASS)return Blocks.DIRT.getDefaultState();
        if(grass.getBlock()!=grassBlock)return null;
        return invoke(dirt,grass);
    }
    public static IBlockState spread(IBlockState source,IBlockState target) {
        if(source.getBlock()==Blocks.GRASS) {
            if(target.equals(Blocks.DIRT.getDefaultState()))return Blocks.GRASS.getDefaultState();
            int index=index(target,false);return index>=0&&index<3?full(true,index+2):null;
        }
        return source.getBlock()==grassBlock?invoke(spread,source,target):null;
    }
    public static boolean spreads(int index) { return index!=0&&index!=1&&index!=6; }
    private static IBlockState invoke(Method method,IBlockState... arguments) {
        try{return (IBlockState)method.invoke(null,(Object[])arguments);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("BOP soil conversion failed",failure);}
    }
}
