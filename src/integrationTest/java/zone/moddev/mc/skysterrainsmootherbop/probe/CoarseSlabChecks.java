package zone.moddev.mc.skysterrainsmootherbop.probe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.TriState;
import zone.moddev.mc.skysbuildingpieces.content.Pieces;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysterrainsmootherbop.CoarseDirtSlab;

/** Real item placement and fluid checks for the retained coarse soil slab. */
final class CoarseSlabChecks {
    static int run(ServerLevel world) {
        ServerPlayer player = ProbeItems.player(world, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("adcb6240-23dd-4444-bba9-c3b6d4e106a1"), "CoarseSlabTest"));
        player.getAbilities().instabuild = false;
        BlockPos position = new BlockPos(40008, 220, 40008);
        world.getChunkAt(position);
        world.setBlock(position.below(), Blocks.STONE.defaultBlockState(), 18);
        for (SlabType half : new SlabType[] { SlabType.BOTTOM, SlabType.TOP }) {
            world.setBlock(position, CoarseDirtSlab.BLOCK.defaultBlockState().setValue(SlabBlock.TYPE, half), 18);
            ItemStack held = Pieces.stack(world.getBlockState(position), 2);
            RuntimeProbe.require(BuiltInRegistries.ITEM.getKey(held.getItem()).toString().equals("skysterrainsmootherbop:coarse_dirt_slab"),
                    "The coarse slab uses its flat playable item, not the hidden carrier");
            RuntimeProbe.require(CoarseDirtSlab.BLOCK.asItem() == held.getItem(),
                    "Coarse slab item identity is stable after registry reload: block="
                            + BuiltInRegistries.ITEM.getKey(CoarseDirtSlab.BLOCK.asItem()) + " held=" + BuiltInRegistries.ITEM.getKey(held.getItem()));
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
            checkFlattening(world, player, position, dry);
            world.setBlock(position, dry, 18);
            checkPlantSupport(world, position, dry);

            world.setBlock(position.east(), Blocks.SNOW_BLOCK.defaultBlockState(), 18);
            BlockState snowy = CoarseDirtSlab.BLOCK.updateShape(dry, Direction.EAST,
                    Blocks.SNOW_BLOCK.defaultBlockState(), world, position, position.east());
            RuntimeProbe.require(snowy.getValue(SnowyDirtBlock.SNOWY), "Coarse slabs keep calculated snow caps");
            world.setBlock(position.east(), Blocks.AIR.defaultBlockState(), 18);
            world.setBlock(position.east(), Blocks.STONE.defaultBlockState(), 18);
            world.setBlock(position.east().above(), Blocks.SNOW.defaultBlockState(), 18);
            world.setBlock(position, dry, 18);
            world.getBlockState(position).randomTick(world, position, net.minecraft.util.RandomSource.create(1));
            RuntimeProbe.require(world.getBlockState(position).getValue(SnowyDirtBlock.SNOWY),
                    "Normal random ticks recognise the higher snowy terrace beside a coarse slab");
            world.setBlock(position.east().above(), Blocks.AIR.defaultBlockState(), 18);
            world.setBlock(position.east(), Blocks.AIR.defaultBlockState(), 18);
            BlockState wet = dry.setValue(SlabBlock.WATERLOGGED, true);
            world.setBlock(position, wet, 18);
            RuntimeProbe.require(wet.canSustainPlant(world, position, Direction.UP,
                            Blocks.OAK_SAPLING.defaultBlockState()) == TriState.FALSE,
                    "A waterlogged coarse slab cannot support a dry-land plant");
        }
        world.setBlock(position, Blocks.AIR.defaultBlockState(), 18);
        zone.moddev.mc.skysterrainsmoother.SkysTerrainSmoother.logger.info(
                "TERRAIN_BOP_COARSE_SLAB_PASS halves=2 combines=2 waterlogging=2 snow=2"
                        + " flattened=2 wet_blocked=2 plant_faces=4 plants=6 random_tick_snow=2 wet_plants=2");
        return 28;
    }

    private static void checkFlattening(ServerLevel world, ServerPlayer player, BlockPos position, BlockState dry) {
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        world.setBlock(position.above(), Blocks.AIR.defaultBlockState(), 18);
        UseOnContext use = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
        BlockState wet = world.getBlockState(position);
        shovel.useOn(use);
        RuntimeProbe.require(world.getBlockState(position).equals(wet) && shovel.getDamageValue() == 0,
                "A waterlogged coarse slab cannot be flattened");
        world.setBlock(position, dry, 18);
        shovel.useOn(use);
        BlockState path = world.getBlockState(position);
        RuntimeProbe.require(path.is(ModBlocks.PATH_SLAB.get())
                        && path.getValue(SlabBlock.TYPE) == dry.getValue(SlabBlock.TYPE)
                        && !path.getValue(SlabBlock.WATERLOGGED) && shovel.getDamageValue() == 1,
                "A shovel flattens a dry coarse slab without changing its half");
    }

    private static void checkPlantSupport(ServerLevel world, BlockPos position, BlockState slab) {
        BlockState sapling = Blocks.OAK_SAPLING.defaultBlockState();
        BlockState flower = Blocks.DANDELION.defaultBlockState();
        boolean hasFullTop = slab.getValue(SlabBlock.TYPE) == SlabType.TOP;
        RuntimeProbe.require(sapling.canSurvive(world, position.above()) == hasFullTop
                        && flower.canSurvive(world, position.above()) == hasFullTop,
                "Saplings and flowers survive on the upper coarse slab only");
        RuntimeProbe.require(!Blocks.WHEAT.defaultBlockState().canSurvive(world, position.above()),
                "Coarse slabs do not replace farmland");
        RuntimeProbe.require(slab.canSustainPlant(world, position, Direction.UP, sapling)
                            == (hasFullTop ? TriState.TRUE : TriState.FALSE)
                        && slab.canSustainPlant(world, position, Direction.EAST, sapling) == TriState.FALSE
                        && world.getBlockState(position).equals(slab),
                "Support checks respect the slab face and leave the world unchanged");
    }
}
