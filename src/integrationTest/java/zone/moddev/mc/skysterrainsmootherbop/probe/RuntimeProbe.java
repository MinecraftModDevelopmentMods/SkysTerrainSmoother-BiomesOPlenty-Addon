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
@Mod(modid="bopterrainprobe",name="BOP Terrain Runtime Probe",version="1",dependencies="required-after:skysterrainsmootherbop")
public final class RuntimeProbe {
    @SidedProxy(clientSide="zone.moddev.mc.skysterrainsmootherbop.probe.ClientProbe",serverSide="zone.moddev.mc.skysterrainsmootherbop.probe.ProbeProxy")
    public static ProbeProxy proxy;
    private MinecraftServer server;
    private boolean ran;
    @Mod.EventHandler public void pre(FMLPreInitializationEvent event){MinecraftForge.EVENT_BUS.register(this);proxy.init();}
    @Mod.EventHandler public void started(FMLServerStartedEvent event){server=FMLCommonHandler.instance().getMinecraftServerInstance();}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(server==null||ran||event.phase!=TickEvent.Phase.END)return;ran=true;
        try {
            WorldServer world=server.worldServerForDimension(0);world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");world.setWorldTime(6000);
            int checks=content(world)+placement(world)+recipes(world)+footprints(world)+nativeGrass(world)+persistence(world)+generationOrder(world);
            int generated=generation(world);
            File marker=new File(world.getSaveHandler().getWorldDirectory(),"skysterrainsmootherbop-integration.properties");Properties values=new Properties();
            if(marker.isFile())try(InputStream in=new FileInputStream(marker)){values.load(in);}
            values.setProperty(phase()+"_complete","true");values.setProperty("gameplay_checks",Integer.toString(checks));values.setProperty("generation_checks",Integer.toString(generated));
            try(OutputStream out=new FileOutputStream(marker)){values.store(out,"BOP terrain qualification");}
            System.out.println("BOP_TERRAIN_RUNTIME_PASS phase="+phase()+" level="+SkysTerrainSmoother.level+" checks="+checks+" generated="+generated);
            ClientStatus.complete=true;
        }catch(Throwable failure){failure.printStackTrace();throw new IllegalStateException("BOP terrain probe failed",failure);}
        if(!System.getProperty("skysterrainsmootherbop.integrationPhase","fresh").startsWith("client-"))server.initiateShutdown();
    }
    private static String phase(){return System.getProperty("skysterrainsmootherbop.integrationPhase","fresh").replace("client-","");}
    private static BlockPos position(WorldServer world,int x,int y,int z){BlockPos pos=new BlockPos(x,y,z);world.getChunkFromBlockCoords(pos);world.getChunkFromBlockCoords(pos.add(8,0,8));world.getChunkFromBlockCoords(pos.add(-8,0,-8));return pos;}
    private static int content(WorldServer world){
        BlockPos p=position(world,80,220,80);int checks=0,states=0;
        for(SoilPieceBlock block:SoilPieces.BLOCKS)for(int slot=0;slot<block.count;slot++)for(int o=0;o<block.shape.states;o++){
            IBlockState state=block.getStateFromMeta(slot*block.shape.states+o);
            require(block.getMetaFromState(state)==slot*block.shape.states+o,"fixed metadata round trip");
            world.setBlockToAir(p.up());world.setBlockToAir(p.north());world.setBlockState(p.down(),Blocks.STONE.getDefaultState(),2);world.setBlockState(p,state,2);
            AxisAlignedBB box=state.getBoundingBox(world,p);double volume=(box.maxX-box.minX)*(box.maxY-box.minY)*(box.maxZ-box.minZ);
            require(volume==(block.shape==SoilPieces.Shape.SLAB?.5:block.shape==SoilPieces.Shape.STEP?.25:.125),"exact partial volume");
            for(EnumFacing face:EnumFacing.values())require(block.shouldSideBeRendered(state,world,p,face),"internal exposed face remains visible");
            world.setBlockState(p.north(),Blocks.SNOW.getDefaultState(),2);
            IBlockState actual=block.getActualState(state,world,p);require(actual.getValue(BlockGrass.SNOWY)&&block.getMetaFromState(actual)==block.getMetaFromState(state),"snow is nonpersistent");
            world.setBlockToAir(p.north());require(!block.getActualState(state,world,p).getValue(BlockGrass.SNOWY),"visual cap leaves with snow");
            boolean complete=block.shape==SoilPieces.Shape.SLAB&&o==0;
            for(Block plant:new Block[]{Blocks.YELLOW_FLOWER,Blocks.TALLGRASS}){
                IBlockState full=block.full(state);boolean nativeSupport=full.getBlock().canSustainPlant(full,new net.minecraft.world.ChunkCache(world,p.add(-2,-2,-2),p.add(2,2,2),0){@Override public IBlockState getBlockState(BlockPos at){return at.equals(p)?full:super.getBlockState(at);}},p,EnumFacing.UP,(IPlantable)plant);
                require(block.canSustainPlant(state,world,p,EnumFacing.UP,(IPlantable)plant)==(complete&&nativeSupport),"plant support uses native properties and a complete upper face");checks++;
            }
            ItemStack expected=SoilPieces.stack(block.grass?SoilPieces.matching(NativeSoils.dirt(block.full(state)),block.shape,o):state,1);
            require(ItemStack.areItemStacksEqual(block.getDrops(world,p,state,0).get(0),expected),"native dirt drop preserves material and geometry");
            if(block.grass){
                IBlockState full=block.full(state);world.setBlockState(p.down(),full,2);block.neighborChanged(state,world,p,full.getBlock());
                require(world.getBlockState(p.down()).equals(NativeSoils.dirt(full)),"native grass support dirtified");
                world.setBlockState(p.up(),Blocks.STONE.getDefaultState(),2);world.setLightFor(EnumSkyBlock.SKY,p.up(),0);world.setLightFor(EnumSkyBlock.BLOCK,p.up(),0);
                block.updateTick(world,p,state,new Random(1));require(world.getBlockState(p).equals(SoilPieces.matching(NativeSoils.dirt(full),block.shape,o)),"covered decay keeps shape and orientation");
                world.setBlockToAir(p.up());world.setBlockState(p,state,2);
                if(block.canGrow(world,p,state,false)){block.grow(world,new Random(1),p,state);require(world.getBlockState(p).equals(state),"bonemeal does not replace its partial support");}
            }
            checks+=block.grass?15:13;states++;
        }
        require(states==252&&SoilPieces.BLOCKS.size()==16,"measured palette budget");
        System.out.println("BOP_TERRAIN_CONTENT_PASS states="+states+" blocks="+SoilPieces.BLOCKS.size());return checks;
    }
    private static InventoryCrafting grid(int size){return new InventoryCrafting(new Container(){public boolean canInteractWith(EntityPlayer player){return true;}},size,size);}
    private static int placement(WorldServer world){
        BlockPos p=position(world,160,220,160);int checks=0;
        FakePlayer player=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("fd91e970-f44c-4b48-95ce-825342755b00"),"SoilPieceProbe"));player.setPosition(164,220,164);player.capabilities.isCreativeMode=false;
        for(boolean grass:new boolean[]{false,true})for(int m=0;m<(grass?8:6);m++)for(SoilPieces.Shape shape:SoilPieces.Shape.values()){
            world.setBlockState(p.down(),Blocks.STONE.getDefaultState(),2);world.setBlockToAir(p);world.setBlockToAir(p.up());
            ItemStack stack=SoilPieces.stack(SoilPieces.state(grass,m,shape,shape==SoilPieces.Shape.SLAB?1:4),8);
            require(stack.onItemUse(player,world,p.down(),EnumHand.MAIN_HAND,EnumFacing.UP,.1f,1,.1f)==EnumActionResult.SUCCESS,"real lower piece placement");
            require(world.getBlockState(p).equals(SoilPieces.state(grass,m,shape,shape==SoilPieces.Shape.SLAB?1:4)),"placement uses clicked footprint");
            require(stack.stackSize==7,"placement consumes one item");checks+=3;
            if(shape==SoilPieces.Shape.SLAB){
                require(stack.onItemUse(player,world,p,EnumHand.MAIN_HAND,EnumFacing.UP,.1f,.5f,.1f)==EnumActionResult.SUCCESS,"complementary slab combines");
                require(world.getBlockState(p).equals(NativeSoils.full(grass,m))&&stack.stackSize==6,"combining preserves native full identity and count");checks+=2;
            }else{
                // Add the adjacent quarter/eighth into the same lower half.
                EnumFacing face=EnumFacing.SOUTH;float x=.5f,z=.5f;
                if(shape==SoilPieces.Shape.CORNER){face=EnumFacing.EAST;x=.5f;z=.1f;}
                require(stack.onItemUse(player,world,p,EnumHand.MAIN_HAND,face,x,.25f,z)==EnumActionResult.SUCCESS,"complementary fine piece combines");
                IBlockState expected=SoilPieces.state(grass,m,shape==SoilPieces.Shape.STEP?SoilPieces.Shape.SLAB:SoilPieces.Shape.STEP,shape==SoilPieces.Shape.STEP?1:4);
                require(world.getBlockState(p).equals(expected)&&stack.stackSize==6,"fine combination retains native material");checks+=2;
            }
        }
        return checks;
    }
    private static void recipe(WorldServer world,ItemStack input,String[] rows,ItemStack expected){
        InventoryCrafting grid=grid(3);for(int y=0;y<rows.length;y++)for(int x=0;x<rows[y].length();x++)if(rows[y].charAt(x)=='X')grid.setInventorySlotContents(x+y*3,input.copy());
        require(ItemStack.areItemStacksEqual(expected,CraftingManager.getInstance().findMatchingRecipe(grid,world)),"crafting manager result "+input+" "+Arrays.toString(rows));
        for(IRecipe candidate:CraftingManager.getInstance().getRecipeList())if(candidate.matches(grid,world))require(ItemStack.areItemStacksEqual(expected,candidate.getCraftingResult(grid)),"no conflicting installed recipe "+candidate.getClass().getName());
    }
    private static int recipes(WorldServer world){
        int checks=0;
        for(boolean grass:new boolean[]{false,true})for(int material=0;material<(grass?8:6);material++){
            IBlockState full=NativeSoils.full(grass,material);ItemStack nativeStack=new ItemStack(full.getBlock(),1,full.getBlock().getMetaFromState(full));
            ItemStack slab=SoilPieces.stack(SoilPieces.state(grass,material,SoilPieces.Shape.SLAB,1),1),step=SoilPieces.stack(SoilPieces.state(grass,material,SoilPieces.Shape.STEP,4),1);
            ItemStack result=slab.copy();result.stackSize=6;recipe(world,nativeStack,new String[]{"XXX"},result);
            recipe(world,slab,new String[]{"XX"},nativeStack);result=step.copy();result.stackSize=4;recipe(world,slab,new String[]{"X "," X"},result);
            recipe(world,step,new String[]{"XX"},SoilPieces.stack(SoilPieces.state(grass,material,SoilPieces.Shape.CORNER,4),4));checks+=4;
        }
        System.out.println("BOP_TERRAIN_RECIPE_PASS recipes="+checks);return checks;
    }
    private static int footprints(WorldServer world){
        BlockPos p=position(world,100,220,100);Chunk owner=world.getChunkFromBlockCoords(p);int checks=0;
        for(boolean grass:new boolean[]{false,true})for(int material=0;material<(grass?8:6);material++)for(int level=1;level<=3;level++)for(int cardinal=0;cardinal<16;cardinal++){
            for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)for(int y=220;y<=223;y++)world.setBlockToAir(p.add(x,y-220,z));
            IBlockState full=NativeSoils.full(grass,material);world.setBlockState(p,full,2);
            int[] dx={0,1,0,-1},dz={-1,0,1,0};for(int d=0;d<4;d++)if((cardinal&(1<<d))!=0)world.setBlockState(p.add(dx[d],1,dz[d]),Blocks.STONE.getDefaultState(),2);
            if(level==3)world.setBlockState(p.north().west().up(),Blocks.STONE.getDefaultState(),2);
            TerrainMaterial mapping=MaterialCatalogue.instance().source(full);
            IBlockState expected=MaterialCatalogue.instance().result(mapping,Footprint.mask(cardinal,level==3?1:0,level),level);
            SkysTerrainSmoother.engine.smooth(world,owner,level);
            require(world.getBlockState(p.up()).equals(expected==null?Blocks.AIR.getDefaultState():expected),"BOP footprint preserves variant "+grass+"/"+material+"/"+level+"/"+cardinal);
            if(grass&&expected!=null)require(world.getBlockState(p).equals(NativeSoils.dirt(full)),"generated grass keeps native soil");checks++;
        }
        return checks;
    }
    private static int nativeGrass(WorldServer world){
        BlockPos p=position(world,140,220,140);int checks=0;
        for(int source=0;source<8;source++)for(int soil=0;soil<6;soil++){
            IBlockState dirt=SoilPieces.state(false,soil,SoilPieces.Shape.CORNER,6),full=NativeSoils.full(true,source),nativeResult=NativeSoils.spread(full,NativeSoils.full(false,soil));
            world.setBlockToAir(p.up());world.setBlockState(p,dirt,2);world.setLightFor(EnumSkyBlock.SKY,p.up(),15);
            boolean changed=SoilLifecycle.grow(world,p,full);
            require(changed==(nativeResult!=null),"native special/coarse spread policy");
            if(nativeResult!=null)require(world.getBlockState(p).equals(SoilPieces.matching(nativeResult,SoilPieces.Shape.CORNER,6)),"spread keeps native grass family and orientation");
            world.setBlockState(p,dirt,2);world.setBlockState(p.up(),SoilPieces.state(true,2,SoilPieces.Shape.SLAB,1),2);
            require(!SoilLifecycle.grow(world,p,full)&&!GrassSpread.growTarget(world,p),"covered BOP target rejected by both paths");checks+=3;
        }
        for(int source:new int[]{5,7})for(SoilPieces.Shape shape:SoilPieces.Shape.values()){
            IBlockState full=NativeSoils.full(true,source);int o=shape==SoilPieces.Shape.SLAB?1:6;
            IBlockState dirt=SoilPieces.matching(Blocks.DIRT.getDefaultState(),shape,o);
            require(GrassSlabsApi.grassFor(dirt,full).equals(SoilPieces.state(true,source,shape,o)),"shared spread preserves Origin/Daisy family and orientation");
            world.setBlockToAir(p.up());world.setBlockState(p,dirt,2);world.setLightFor(EnumSkyBlock.SKY,p.up(),15);
            require(SoilLifecycle.grow(world,p,full)&&world.getBlockState(p).equals(SoilPieces.state(true,source,shape,o)),"real cross-mod dirt growth keeps BOP identity");checks+=2;
        }
        world.setBlockState(p.down(),NativeSoils.full(true,2),2);
        world.setBlockState(p,zone.moddev.mc.skysgrassslabs.init.ModBlocks.GRASS_SLAB.getStateFromMeta(1),3);
        require(world.getBlockState(p.down()).equals(NativeSoils.full(false,0)),"Sky grass slab dirtifies BOP support through the optional rule");checks++;
        for(int special:new int[]{0,1,6})require(!GrassSlabsApi.isSpreadingGrassForm(NativeSoils.full(true,special)),"nonspreading native special surface stays nonspreading");checks+=3;
        return checks;
    }
    private static int persistence(WorldServer world){
        int index=0;
        for(SoilPieceBlock block:SoilPieces.BLOCKS)for(int slot=0;slot<block.count;slot++)for(int o=0;o<block.shape.states;o++){
            BlockPos pos=position(world,2000+index,100,2000);IBlockState state=block.getStateFromMeta(slot*block.shape.states+o);index++;
            if(phase().equals("reload"))require(world.getBlockState(pos).equals(state),"saved soil identity and metadata");else world.setBlockState(pos,state,2);
        }
        BlockPos pos=position(world,2000,105,2000);if(!phase().equals("reload")){
            world.setBlockState(pos,Blocks.CHEST.getDefaultState(),2);ItemStack stack=SoilPieces.stack(SoilPieces.state(true,7,SoilPieces.Shape.SLAB,1),37);NBTTagCompound tag=new NBTTagCompound();tag.setString("custom_label","BOP terrain fixture");stack.setTagCompound(tag);((TileEntityChest)world.getTileEntity(pos)).setInventorySlotContents(0,stack);
        }else{ItemStack stack=((TileEntityChest)world.getTileEntity(pos)).getStackInSlot(0);require(stack.stackSize==37&&stack.getTagCompound().getString("custom_label").equals("BOP terrain fixture"),"saved counts and item NBT");}
        return index+1;
    }
    private static int generation(WorldServer world){
        FakePlayer player=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("6ba324bf-9731-4e8b-98cd-885e882bd3e1"),"BopTerrainProbe"));world.getPlayerChunkMap().setPlayerViewRadius(4);int found=0;
        for(int attempt=0;attempt<10;attempt++){
            int cx=400+attempt*160+(phase().equals("reload")?2000:0),cz=400+attempt*40;
            player.setPosition(cx*16+8,100,cz*16+8);world.getPlayerChunkMap().addPlayer(player);
            try{
                for(int tick=0;tick<100;tick++){world.getPlayerChunkMap().tick();world.getChunkProvider().tick();}
                for(int z=cz-4;z<=cz+4;z++)for(int x=cx-4;x<=cx+4;x++){
                    Chunk chunk=world.getChunkProvider().getLoadedChunk(x,z);if(chunk==null)continue;
                    for(ExtendedBlockStorage section:chunk.getBlockStorageArray())if(section!=null)for(int y=0;y<16;y++)for(int zz=0;zz<16;zz++)for(int xx=0;xx<16;xx++){
                        IBlockState state=section.get(xx,y,zz);if(!(state.getBlock() instanceof SoilPieceBlock))continue;
                        TerrainMaterial material=MaterialCatalogue.instance().piece(state);BlockPos pos=new BlockPos((x<<4)+xx,section.getYLocation()+y,(z<<4)+zz);IBlockState support=chunk.getBlockState(pos.down());
                        require(material.sources.contains(support)||support.equals(material.supportAfterPlacement),"generated BOP piece retains correct support");found++;
                    }
                }
            }finally{world.getPlayerChunkMap().removePlayer(player);}
            if(found>0)break;
        }
        require(found>0,"normal player-tracked BOP generation produced no eligible pieces in bounded candidate regions");return found;
    }
    private static int generationOrder(WorldServer world)throws Exception{
        for(int level=1;level<=3;level++){
            byte[][] hashes=new byte[2][];
            for(int order=0;order<2;order++){
                int origin=order==0?128:256;List<Chunk> chunks=new ArrayList<>();
                for(int z=0;z<9;z++)for(int x=0;x<9;x++)chunks.add(world.getChunkFromChunkCoords(origin+x,128+z));
                for(int z=0;z<144;z++)for(int x=0;x<144;x++){
                    int height=200+Math.floorMod(x*31+z*17+(x>>3)*(z>>3),3),material=Math.floorMod(x*7+z*11,14);
                    Chunk chunk=chunks.get((x>>4)+(z>>4)*9);ExtendedBlockStorage section=chunk.getBlockStorageArray()[12];
                    if(section==null)chunk.getBlockStorageArray()[12]=section=new ExtendedBlockStorage(192,true);
                    for(int y=200;y<205;y++)section.set(x&15,y&15,z&15,Blocks.AIR.getDefaultState());
                    section.set(x&15,height&15,z&15,NativeSoils.full(material<8,material<8?material:material-8));
                    chunk.getHeightMap()[(x&15)|((z&15)<<4)]=height+1;
                }
                if(order==1)Collections.reverse(chunks);
                for(Chunk chunk:chunks)SkysTerrainSmoother.engine.smooth(world,chunk,level);
                java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
                for(int z=0;z<144;z++)for(int x=0;x<144;x++)for(int y=200;y<205;y++){
                    IBlockState state=world.getBlockState(new BlockPos((origin<<4)+x,y,(128<<4)+z));
                    digest.update((state.getBlock().getRegistryName()+"/"+state.getBlock().getMetaFromState(state)+"\n").getBytes("UTF-8"));
                }
                hashes[order]=digest.digest();
            }
            require(Arrays.equals(hashes[0],hashes[1]),"mixed BOP 9x9 generation order differs at level "+level);
            StringBuilder hex=new StringBuilder();for(byte value:hashes[0])hex.append(String.format("%02X",value&255));
            System.out.println("BOP_TERRAIN_GENERATION_ORDER_PASS level="+level+" chunks=81 sha256="+hex);
        }
        return 486;
    }
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
