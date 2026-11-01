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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysterrainsmootherbop.CoarseDirtSlab;

/** Real item placement and fluid checks for the retained coarse soil slab. */
final class CoarseSlabChecks {
    static int run(ServerWorld world) {
        ServerPlayerEntity player = FakePlayerFactory.getMinecraft(world);
        player.abilities.instabuild = false;
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
            player.setItemInHand(Hand.MAIN_HAND, held);
            Direction face = half == SlabType.BOTTOM ? Direction.UP : Direction.DOWN;
            ActionResultType used = held.useOn(new ItemUseContext(player, Hand.MAIN_HAND,
                    new BlockRayTraceResult(new Vector3d(position.getX() + .5,
                            position.getY() + (half == SlabType.BOTTOM ? .5 : 1), position.getZ() + .5),
                            face, position, false)));
            RuntimeProbe.require(used == ActionResultType.CONSUME && held.getCount() == 1
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
        System.out.println("TERRAIN_BOP_COARSE_SLAB_PASS halves=2 combines=2 waterlogging=2 snow=2");
        return 10;
    }
}
