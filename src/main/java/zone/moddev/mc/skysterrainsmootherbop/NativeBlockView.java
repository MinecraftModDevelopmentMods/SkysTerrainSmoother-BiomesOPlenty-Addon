package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;

/** Read-only native soil view for APIs that inspect their block's properties. */
final class NativeBlockView implements IBlockAccess {
    private final IBlockAccess delegate;
    private final BlockPos pos;
    private final IBlockState full;
    NativeBlockView(IBlockAccess delegate,BlockPos pos,IBlockState full){this.delegate=delegate;this.pos=pos;this.full=full;}
    public TileEntity getTileEntity(BlockPos at){return at.equals(pos)?null:delegate.getTileEntity(at);}
    public int getCombinedLight(BlockPos at,int light){return delegate.getCombinedLight(at,light);}
    public IBlockState getBlockState(BlockPos at){return at.equals(pos)?full:delegate.getBlockState(at);}
    public boolean isAirBlock(BlockPos at){return !at.equals(pos)&&delegate.isAirBlock(at);}
    public Biome getBiome(BlockPos at){return delegate.getBiome(at);}
    public int getStrongPower(BlockPos at,EnumFacing face){return delegate.getStrongPower(at,face);}
    public WorldType getWorldType(){return delegate.getWorldType();}
    public boolean isSideSolid(BlockPos at,EnumFacing face,boolean fallback){return at.equals(pos)?full.isSideSolid(this,at,face):delegate.isSideSolid(at,face,fallback);}
}
