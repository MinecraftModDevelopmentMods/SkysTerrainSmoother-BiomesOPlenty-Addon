package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysterrainsmootherbop.CoarseDirtSlab;

/** Real item placement and fluid checks for the retained coarse soil slab. */
final class CoarseSlabChecks {
    static int run(ServerLevel world) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(world);
        player.getAbilities().instabuild = false;
        BlockPos position = new BlockPos(40008, 220, 40008);
        world.getChunkAt(position);
        world.setBlock(position.below(), Blocks.STONE.defaultBlockState(), 18);
        for (SlabType half : new SlabType[] { SlabType.BOTTOM, SlabType.TOP }) {
            world.setBlock(position, CoarseDirtSlab.BLOCK.defaultBlockState().setValue(SlabBlock.TYPE, half), 18);
            ItemStack held = Pieces.stack(world.getBlockState(position), 2);
            RuntimeProbe.require(held.getItem().getRegistryName().toString().equals("skysterrainsmootherbop:coarse_dirt_slab"),
                    "The coarse slab uses its flat playable item, not the hidden carrier");
            RuntimeProbe.require(CoarseDirtSlab.BLOCK.asItem() == held.getItem(),
                    "Coarse slab item identity is stable after registry reload: block="
                            + CoarseDirtSlab.BLOCK.asItem().getRegistryName() + " held=" + held.getItem().getRegistryName());
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            Direction face = half == SlabType.BOTTOM ? Direction.UP : Direction.DOWN;
            InteractionResult used = held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(position.getX() + .5,
                            position.getY() + (half == SlabType.BOTTOM ? .5 : 1), position.getZ() + .5),
                            face, position, false)));
            RuntimeProbe.require(used == InteractionResult.CONSUME && held.getCount() == 1
                            && world.getBlockState(position).getBlock() == Blocks.COARSE_DIRT,
                    "Both complementary placement directions make one full coarse dirt block: half=" + half
                            + " action=" + used + " remaining=" + held.getCount()
                            + " state=" + world.getBlockState(position));

            BlockState dry = CoarseDirtSlab.BLOCK.defaultBlockState().setValue(SlabBlock.TYPE, half);
            world.setBlock(position, dry, 18);
            RuntimeProbe.require(CoarseDirtSlab.BLOCK.placeLiquid(world, position, dry,
                    Fluids.WATER.getSource(false)), "Coarse slabs use native waterlogging");
            RuntimeProbe.require(world.getBlockState(position).getValue(SlabBlock.WATERLOGGED), "Water remains in the slab");
            world.setBlock(position, dry, 18);
            world.setBlock(position.east(), Blocks.SNOW_BLOCK.defaultBlockState(), 18);
            BlockState snowy = CoarseDirtSlab.BLOCK.updateShape(dry, Direction.EAST,
                    Blocks.SNOW_BLOCK.defaultBlockState(), world, position, position.east());
            RuntimeProbe.require(snowy.getValue(SnowyDirtBlock.SNOWY), "Coarse slabs keep calculated snow caps");
            world.setBlock(position.east(), Blocks.AIR.defaultBlockState(), 18);
        }
        world.setBlock(position, Blocks.AIR.defaultBlockState(), 18);
        zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info("TERRAIN_BOP_COARSE_SLAB_PASS halves=2 combines=2 waterlogging=2 snow=2");
        return 10;
    }
}
