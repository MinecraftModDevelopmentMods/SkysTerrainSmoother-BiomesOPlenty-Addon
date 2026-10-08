package zone.moddev.mc.skysterrainsmootherbop;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.block.GrassSpread;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** BOP decides the native grass/soil identity; this layer preserves partial geometry. */
public final class SoilLifecycle {
    private SoilLifecycle(){ }
    public static void repairSupport(World world,BlockPos pos,IBlockState state){
        SoilPieceBlock block=(SoilPieceBlock)state.getBlock();
        if(world.isRemote||!block.grass||!world.isBlockLoaded(pos.down()))return;
        IBlockState lower=world.getBlockState(pos.down()),dirt=NativeSoils.dirt(lower);
        if(dirt==null)dirt=GrassSlabsApi.dirtFor(lower);
        if(dirt!=null)world.setBlockState(pos.down(),dirt,2);
    }
    public static void tick(World world,BlockPos pos,IBlockState state,Random random){
        if(world.isRemote||!world.isAreaLoaded(pos,3))return;
        SoilPieceBlock block=(SoilPieceBlock)state.getBlock();
        if(block.grass){
            repairSupport(world,pos,state);
            if(!GrassSpread.canRemainGrass(world,pos)){
                world.setBlockState(pos,SoilPieces.matching(NativeSoils.dirt(block.full(state)),block.shape,block.orientation(state)),3);return;
            }
            if(!NativeSoils.spreads(block.material(state))||!GrassSpread.hasSpreadLight(world,pos))return;
            for(int attempt=0;attempt<4;attempt++){
                BlockPos target=pos.add(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
                if(!target.equals(pos.down()))grow(world,target,block.full(state));
            }
        }else if(block.material(state)<3){
            for(int attempt=0;attempt<4;attempt++){
                BlockPos source=pos.add(random.nextInt(3)-1,random.nextInt(5)-1,random.nextInt(3)-1);
                if(!world.isBlockLoaded(source)||!GrassSpread.canRemainGrass(world,source)||!GrassSpread.hasSpreadLight(world,source))continue;
                IBlockState full=source(world.getBlockState(source));if(full!=null)grow(world,pos,full);
            }
        }
    }
    private static IBlockState source(IBlockState state){
        if(state.getBlock()==NativeSoils.grassBlock){int index=NativeSoils.index(state,true);return NativeSoils.spreads(index)?state:null;}
        if(state.getBlock() instanceof SoilPieceBlock){SoilPieceBlock block=(SoilPieceBlock)state.getBlock();return block.grass&&NativeSoils.spreads(block.material(state))?block.full(state):null;}
        return state.getBlock()==Blocks.GRASS||GrassSlabsApi.isSpreadingGrassForm(state)?Blocks.GRASS.getDefaultState():null;
    }
    public static IBlockState spreadResult(IBlockState source,IBlockState target) {
        IBlockState fullSource=source(source);if(fullSource==null||fullSource.getBlock()!=NativeSoils.grassBlock)return null;
        if(target.getBlock() instanceof SoilPieceBlock) {
            SoilPieceBlock piece=(SoilPieceBlock)target.getBlock();if(piece.grass)return null;
            IBlockState result=NativeSoils.spread(fullSource,piece.full(target));
            return result==null?null:SoilPieces.matching(result,piece.shape,piece.orientation(target));
        }
        if(target.getBlock()==ModBlocks.DIRT_SLAB) {
            IBlockState result=NativeSoils.spread(fullSource,Blocks.DIRT.getDefaultState());
            return result==null?null:SoilPieces.matching(result,SoilPieces.Shape.SLAB,target.getValue(net.minecraft.block.BlockSlab.HALF)==net.minecraft.block.BlockSlab.EnumBlockHalf.TOP?0:1);
        }
        for(SoilPieces.Shape shape:new SoilPieces.Shape[]{SoilPieces.Shape.STEP,SoilPieces.Shape.CORNER})for(int o=0;o<8;o++) {
            IBlockState vanillaDirt=SoilPieces.matching(Blocks.DIRT.getDefaultState(),shape,o);
            if(target.equals(vanillaDirt)) {
                IBlockState result=NativeSoils.spread(fullSource,Blocks.DIRT.getDefaultState());return result==null?null:SoilPieces.matching(result,shape,o);
            }
        }
        return NativeSoils.spread(fullSource,target);
    }
    public static boolean grow(World world,BlockPos pos,IBlockState source){
        if(pos.getY()<0||pos.getY()>=256||!world.isBlockLoaded(pos)||!world.isBlockLoaded(pos.up()))return false;
        IBlockState cover=world.getBlockState(pos.up());
        if(cover.getBlock()==ModBlocks.TURF||GrassSlabsApi.isGrassForm(cover)||world.getLightFromNeighbors(pos.up())<4||cover.getLightOpacity(world,pos.up())>2)return false;
        IBlockState target=world.getBlockState(pos),full=target,result;
        SoilPieceBlock piece=target.getBlock() instanceof SoilPieceBlock?(SoilPieceBlock)target.getBlock():null;
        if(piece!=null){if(piece.grass)return false;full=piece.full(target);}
        else if(GrassSlabsApi.grassFor(target)!=null) {
            IBlockState converted=GrassSlabsApi.grassFor(target,source);return converted!=null&&world.setBlockState(pos,converted,3);
        }
        result=NativeSoils.spread(source,full);if(result==null)return false;
        if(piece!=null)result=SoilPieces.matching(result,piece.shape,piece.orientation(target));
        return result!=null&&world.setBlockState(pos,result,3);
    }
}
