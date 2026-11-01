package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysterrainsmootherbop.CoarseDirtSlab;

/** Real item placement and fluid checks for the retained coarse soil slab. */
final class CoarseSlabChecks {
    static int run(ServerWorld world) {
        ServerPlayerEntity player = FakePlayerFactory.getMinecraft(world);
        player.abilities.isCreativeMode = false;
        BlockPos position = new BlockPos(40008, 220, 40008);
        world.getChunkAt(position);
        world.setBlockState(position.down(), Blocks.STONE.getDefaultState(), 18);
        for (SlabType half : new SlabType[] { SlabType.BOTTOM, SlabType.TOP }) {
            world.setBlockState(position, CoarseDirtSlab.BLOCK.getDefaultState().with(SlabBlock.TYPE, half), 18);
            ItemStack held = Pieces.stack(world.getBlockState(position), 2);
            RuntimeProbe.require(held.getItem().getRegistryName().toString().equals("skysterrainsmootherbop:coarse_dirt_slab"),
                    "The coarse slab uses its flat playable item, not the hidden carrier");
            RuntimeProbe.require(CoarseDirtSlab.BLOCK.asItem() == held.getItem(),
                    "Coarse slab item identity is stable after registry reload: block="
                            + CoarseDirtSlab.BLOCK.asItem().getRegistryName() + " held=" + held.getItem().getRegistryName());
            player.setHeldItem(Hand.MAIN_HAND, held);
            Direction face = half == SlabType.BOTTOM ? Direction.UP : Direction.DOWN;
            ActionResultType used = held.onItemUse(new ItemUseContext(player, Hand.MAIN_HAND,
                    new BlockRayTraceResult(new Vec3d(position.getX() + .5,
                            position.getY() + (half == SlabType.BOTTOM ? .5 : 1), position.getZ() + .5),
                            face, position, false)));
            RuntimeProbe.require(used == ActionResultType.SUCCESS && held.getCount() == 1
                            && world.getBlockState(position).getBlock() == Blocks.COARSE_DIRT,
                    "Both complementary placement directions make one full coarse dirt block: half=" + half
                            + " action=" + used + " remaining=" + held.getCount()
                            + " state=" + world.getBlockState(position));

            BlockState dry = CoarseDirtSlab.BLOCK.getDefaultState().with(SlabBlock.TYPE, half);
            world.setBlockState(position, dry, 18);
            RuntimeProbe.require(CoarseDirtSlab.BLOCK.receiveFluid(world, position, dry,
                    Fluids.WATER.getStillFluidState(false)), "Coarse slabs use native waterlogging");
            RuntimeProbe.require(world.getBlockState(position).get(SlabBlock.WATERLOGGED), "Water remains in the slab");
            world.setBlockState(position, dry, 18);
            world.setBlockState(position.east(), Blocks.SNOW_BLOCK.getDefaultState(), 18);
            BlockState snowy = CoarseDirtSlab.BLOCK.updatePostPlacement(dry, Direction.EAST,
                    Blocks.SNOW_BLOCK.getDefaultState(), world, position, position.east());
            RuntimeProbe.require(snowy.get(SnowyDirtBlock.SNOWY), "Coarse slabs keep calculated snow caps");
            world.setBlockState(position.east(), Blocks.AIR.getDefaultState(), 18);
        }
        world.setBlockState(position, Blocks.AIR.getDefaultState(), 18);
        System.out.println("TERRAIN_BOP_COARSE_SLAB_PASS halves=2 combines=2 waterlogging=2 snow=2");
        return 10;
    }
}
