package zone.moddev.mc.skysterrainsmootherbop;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.item.ShovelSupport;

/** Retains the old coarse-soil slab without turning it into spreading dirt. */
public final class CoarseDirtSlab extends SlabBlock {
    public static final String BLOCK_ID = SkysTerrainSmootherBop.ID + ":dirt_slab_00";
    // Forge creates block holders only while the block registry is open.
    public static CoarseDirtSlab BLOCK;
    static Item item;

    CoarseDirtSlab() {
        this(Properties.of().mapColor(MapColor.DIRT).strength(.5f).sound(SoundType.GRAVEL)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.parse(BLOCK_ID)))
                .noOcclusion().randomTicks());
    }

    private CoarseDirtSlab(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.SNOWY, false));
    }

    static boolean flatten(PlayerInteractEvent.RightClickBlock event) {
        ItemStack shovel = event.getItemStack();
        if (!ShovelSupport.isShovel(shovel) || event.getFace() == Direction.DOWN) {
            return false;
        }

        var world = event.getLevel();
        BlockPos position = event.getPos();
        BlockState state = world.getBlockState(position);
        if (!state.is(BLOCK) || state.getValue(WATERLOGGED) || !world.isEmptyBlock(position.above())) {
            return false;
        }

        var player = event.getEntity();
        if (!world.mayInteract(player, position) || !player.mayUseItemAt(position, event.getFace(), shovel)
                || event.getHand() == InteractionHand.MAIN_HAND
                        && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
                        && !player.isSecondaryUseActive()) {
            return false;
        }

        BlockState path = state.getValue(TYPE) == SlabType.DOUBLE
                ? Blocks.DIRT_PATH.defaultBlockState()
                : ModBlocks.PATH_SLAB.get().defaultBlockState()
                        .setValue(TYPE, state.getValue(TYPE)).setValue(WATERLOGGED, false);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, position, shovel);
        }
        shovel.hurtAndBreak(1, player, event.getHand().asEquipmentSlot());
        world.setBlock(position, path, Block.UPDATE_ALL_IMMEDIATE);
        world.playSound(player, position, SoundEvents.SHOVEL_FLATTEN.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(player, path));
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    @Override
    public Item asItem() {
        // Registry reloads can otherwise select the old palette's hidden recovery item.
        // Placement, picking and drops must keep using the playable coarse-dirt slab.
        return item == null ? super.asItem() : item;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        if (state.getValue(TYPE) == SlabType.DOUBLE) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        return state.setValue(BlockStateProperties.SNOWY, snowy(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks,
            BlockPos position, Direction facing, BlockPos neighbourPosition, BlockState neighbour,
            RandomSource random) {
        return super.updateShape(state, world, ticks, position, facing, neighbourPosition, neighbour, random)
                .setValue(BlockStateProperties.SNOWY, snowy(world, position));
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos position, RandomSource random) {
        if (!world.isClientSide()) {
            BlockState updated = state.setValue(BlockStateProperties.SNOWY, snowy(world, position));
            if (updated != state) {
                world.setBlock(position, updated, 2 | 16);
            }
        }
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos position, Direction face,
            net.minecraftforge.common.IPlantable plant) {
        BlockState full = Blocks.COARSE_DIRT.defaultBlockState();
        return state.getValue(TYPE) == SlabType.TOP && face == Direction.UP
                && full.getBlock().canSustainPlant(full, world, position, face, plant);
    }

    private static boolean snowy(BlockGetter world, BlockPos position) {
        for (Direction direction : new Direction[] {
                Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            BlockPos neighbour = position.relative(direction);
            Block block;
            if (world instanceof ServerLevel) {
                LevelChunk chunk = ((ServerLevel) world).getChunkSource()
                        .getChunkNow(neighbour.getX() >> 4, neighbour.getZ() >> 4);
                if (chunk == null) {
                    continue;
                }
                block = chunk.getBlockState(neighbour).getBlock();
            } else {
                if (world instanceof LevelReader
                        && !((LevelReader) world).hasChunk(neighbour.getX() >> 4, neighbour.getZ() >> 4)) {
                    continue;
                }
                block = world.getBlockState(neighbour).getBlock();
            }
            if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
                return true;
            }
        }
        return false;
    }
}
