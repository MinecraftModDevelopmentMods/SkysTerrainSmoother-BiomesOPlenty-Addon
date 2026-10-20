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

    NativeBlockView(IBlockAccess delegate, BlockPos pos, IBlockState full) {
        this.delegate = delegate;
        this.pos = pos;
        this.full = full;
    }

    public TileEntity getTileEntity(BlockPos targetPosition) {
        return targetPosition.equals(pos) ? null : delegate.getTileEntity(targetPosition);
    }

    public int getCombinedLight(BlockPos targetPosition, int light) {
        return delegate.getCombinedLight(targetPosition, light);
    }

    public IBlockState getBlockState(BlockPos targetPosition) {
        return targetPosition.equals(pos) ? full : delegate.getBlockState(targetPosition);
    }

    public boolean isAirBlock(BlockPos targetPosition) {
        return !targetPosition.equals(pos) && delegate.isAirBlock(targetPosition);
    }

    public Biome getBiome(BlockPos targetPosition) {
        return delegate.getBiome(targetPosition);
    }

    public int getStrongPower(BlockPos targetPosition, EnumFacing face) {
        return delegate.getStrongPower(targetPosition, face);
    }

    public WorldType getWorldType() {
        return delegate.getWorldType();
    }

    public boolean isSideSolid(BlockPos targetPosition, EnumFacing face, boolean fallback) {
        return targetPosition.equals(pos) ? full.isSideSolid(this, targetPosition, face)
                : delegate.isSideSolid(targetPosition, face, fallback);
    }
}
