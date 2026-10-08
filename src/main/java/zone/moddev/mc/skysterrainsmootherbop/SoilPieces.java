package zone.moddev.mc.skysterrainsmootherbop;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.Loader;
import zone.moddev.mc.skysbuildingpieces.api.BuildingPiecesApi;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysterrainsmoother.api.*;

/** Fixed palette slots: grass 0..7; dirt 0..5. Later additions must append IDs. */
public final class SoilPieces {
    public static final List<SoilPieceBlock> BLOCKS=new ArrayList<>();
    public enum Shape { SLAB(2),STEP(8),CORNER(8); public final int states; Shape(int states){this.states=states;} }
    private SoilPieces() { }
    public static void register() {
        NativeSoils.initialize();
        for(boolean grass:new boolean[]{false,true})for(Shape shape:Shape.values()) {
            int materials=grass?8:6,capacity=16/shape.states;
            for(int start=0;start<materials;start+=capacity) {
                SoilPieceBlock block=new SoilPieceBlock(grass,shape,start,Math.min(capacity,materials-start));
                GameRegistry.register(block);GameRegistry.register(new SoilPieceItem(block).setRegistryName(block.getRegistryName()));BLOCKS.add(block);
            }
        }
        if(BLOCKS.size()!=16)throw new IllegalStateException("BOP terrain block budget changed");
    }
    public static IBlockState state(boolean grass,int material,Shape shape,int orientation) {
        for(SoilPieceBlock block:BLOCKS)if(block.grass==grass&&block.shape==shape&&material>=block.start&&material<block.start+block.count)
            return block.getStateFromMeta((material-block.start)*shape.states+orientation);
        throw new IllegalArgumentException("Unassigned terrain palette slot");
    }
    public static IBlockState matching(IBlockState full,Shape shape,int orientation) {
        int grass=NativeSoils.index(full,true),dirt=NativeSoils.index(full,false);
        if(grass>=0)return state(true,grass,shape,orientation);
        if(dirt>=0)return state(false,dirt,shape,orientation);
        String material=full.getBlock().getRegistryName().toString();
        if(shape==Shape.SLAB&&full.getBlock()==net.minecraft.init.Blocks.DIRT)
            return ModBlocks.DIRT_SLAB.getStateFromMeta(orientation);
        if(shape==Shape.SLAB&&full.getBlock()==net.minecraft.init.Blocks.GRASS)
            return ModBlocks.GRASS_SLAB.getStateFromMeta(orientation);
        EnumFacing facing=new EnumFacing[]{EnumFacing.NORTH,EnumFacing.EAST,EnumFacing.SOUTH,EnumFacing.WEST}[orientation&3];
        IBlockState bottom=BuildingPiecesApi.bottomPiece(material,shape==Shape.SLAB?BuildingPiecesApi.TerrainShape.SLAB:shape==Shape.STEP?BuildingPiecesApi.TerrainShape.STEP:BuildingPiecesApi.TerrainShape.CORNER,facing);
        if(bottom==null)return null;
        if(shape==Shape.SLAB&&orientation==0) {
            if(bottom.getBlock() instanceof BlockSlab)return bottom.withProperty(BlockSlab.HALF,BlockSlab.EnumBlockHalf.TOP);
            return bottom.getBlock().getStateFromMeta(bottom.getBlock().getMetaFromState(bottom)-1);
        }
        if(shape!=Shape.SLAB&&orientation<4)return bottom.getBlock().getStateFromMeta(bottom.getBlock().getMetaFromState(bottom)-4);
        return bottom;
    }
    public static void initialize() {
        for(int material=0;material<8;material++) {
            IBlockState grass=NativeSoils.full(true,material),dirt=NativeSoils.dirt(grass);
            GrassSlabsApi.registerGrassSupport(grass,dirt);
            if(material>=2&&material<=4)GrassSlabsApi.registerGrassForm(dirt,grass);
            else GrassSlabsApi.registerGrassVariant(dirt,grass,NativeSoils.spreads(material));
        }
        for(SoilPieceBlock block:BLOCKS)if(block.grass)for(int slot=0;slot<block.count;slot++)for(int orientation=0;orientation<block.shape.states;orientation++) {
            int material=block.start+slot;IBlockState grass=state(true,material,block.shape,orientation);
            IBlockState dirt=matching(NativeSoils.dirt(NativeSoils.full(true,material)),block.shape,orientation);
            if(dirt==null)throw new IllegalStateException("No matching dirt geometry for "+grass);
            if(material>=2&&material<=4)GrassSlabsApi.registerGrassForm(dirt,grass);
            else GrassSlabsApi.registerGrassVariant(dirt,grass,NativeSoils.spreads(material));
        }
        for(boolean grass:new boolean[]{false,true})for(int material=0;material<(grass?8:6);material++) {
            IBlockState full=NativeSoils.full(grass,material),support=grass?NativeSoils.dirt(full):null;
            IBlockState[] steps=new IBlockState[4],corners=new IBlockState[4];
            for(int d=0;d<4;d++){steps[d]=state(grass,material,Shape.STEP,d+4);corners[d]=state(grass,material,Shape.CORNER,d+4);}
            TerrainSmoothingApi.registerMaterial(new TerrainMaterial(new ResourceLocation(SkysTerrainSmootherBop.ID,(grass?"grass_":"dirt_")+(grass?NativeSoils.GRASS_NAMES[material]:NativeSoils.DIRT_NAMES[material])),
                    Collections.singleton(full),state(grass,material,Shape.SLAB,1),steps,corners,grass,support));
        }
        if(Loader.isModLoaded("skysbuildingpiecesbop"))for(String name:new String[]{"crag_rock","limestone","shale","siltstone"}) {
            String id="biomesoplenty:"+name;
            IBlockState slab=BuildingPiecesApi.bottomPiece(id,BuildingPiecesApi.TerrainShape.SLAB,EnumFacing.NORTH);
            if(slab==null)continue;
            Block full=Block.REGISTRY.getObject(new ResourceLocation("biomesoplenty",name.equals("crag_rock")?name:"stone"));if(full==null||full==net.minecraft.init.Blocks.AIR)continue;
            int meta=name.equals("siltstone")?1:name.equals("shale")?2:0;
            IBlockState[] steps=new IBlockState[4],corners=new IBlockState[4];
            for(int d=0;d<4;d++) {EnumFacing facing=new EnumFacing[]{EnumFacing.NORTH,EnumFacing.EAST,EnumFacing.SOUTH,EnumFacing.WEST}[d];steps[d]=BuildingPiecesApi.bottomPiece(id,BuildingPiecesApi.TerrainShape.STEP,facing);corners[d]=BuildingPiecesApi.bottomPiece(id,BuildingPiecesApi.TerrainShape.CORNER,facing);}
            TerrainSmoothingApi.registerMaterial(new TerrainMaterial(new ResourceLocation(id),Collections.singleton(full.getStateFromMeta(meta)),slab,steps,corners,false,null));
        }
        GrassSlabsApi.registerSpreadRule(SkysTerrainSmootherBop.ID,SoilLifecycle::spreadResult);
        SoilRecipes.register();
    }
    public static ItemStack stack(IBlockState state,int count) {
        if(state==null)return null;Block block=state.getBlock();int meta=block instanceof SoilPieceBlock?((SoilPieceBlock)block).canonical(state):
                block==ModBlocks.DIRT_SLAB||block==ModBlocks.GRASS_SLAB?0:block.damageDropped(state);
        return new ItemStack(Item.getItemFromBlock(block),count,meta);
    }
}
