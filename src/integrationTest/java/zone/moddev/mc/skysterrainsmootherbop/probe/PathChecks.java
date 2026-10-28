package zone.moddev.mc.skysterrainsmootherbop.probe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.skysterrainsmootherbop.*;

/** Exercises real shovel interactions, geometry and saved paths in isolated worlds. */
public final class PathChecks {
    public static int run(WorldServer world) {
        EntityPlayerMP player = FakePlayerFactory.getMinecraft(world);
        BlockPos position = new BlockPos(80, 220, 96);
        world.getChunkFromBlockCoords(position);
        player.capabilities.allowEdit = true;
        player.capabilities.isCreativeMode = false;
        int interactions = 0;
        int converted = 0;
        for (boolean grass : new boolean[] { false, true }) {
            for (int material = 0; material < (grass ? 8 : 6); material++) {
                boolean eligible = grass ? material == 2 || material == 3 || material == 4
                        || material == 5 || material == 7 : material < 3;
                for (SoilPieces.Shape shape : SoilPieces.Shape.values()) {
                    for (int orientation = 0; orientation < shape.states; orientation++) {
                        IBlockState source = SoilPieces.state(grass, material, shape, orientation);
                        for (EnumHand hand : EnumHand.values()) {
                            for (EnumFacing face : EnumFacing.values()) {
                                world.setBlockToAir(position);
                                world.setBlockToAir(position.up());
                                world.setBlockState(position.down(), NativeSoils.full(true, 2), 2);
                                world.setBlockState(position, source, 2);
                                IBlockState support = world.getBlockState(position.down());
                                AxisAlignedBB originalBounds = source.getBoundingBox(world, position);
                                ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
                                player.setSneaking(hand == EnumHand.OFF_HAND);
                                EnumActionResult result = click(world, player, position, shovel, hand, face);
                                IBlockState actual = world.getBlockState(position);
                                boolean expected = eligible && face != EnumFacing.DOWN;
                                if (!expected) {
                                    require(actual.equals(source) && shovel.getItemDamage() == 0,
                                            "unsupported soil or downward click must stay unchanged");
                                    require(world.getBlockState(position.down()).equals(support), "rejected support edit");
                                } else {
                                    require(result == EnumActionResult.SUCCESS && !actual.equals(source),
                                            "BOP piece must flatten through the normal shovel interaction");
                                    require(shovel.getItemDamage() == 1, "one durability for the complete action");
                                    AxisAlignedBB bounds = actual.getBoundingBox(world, position);
                                    require(close(bounds.maxY, originalBounds.maxY - 1.0 / 16.0), "one-pixel path top");
                                    require(close(bounds.minX, originalBounds.minX) && close(bounds.maxX, originalBounds.maxX)
                                            && close(bounds.minZ, originalBounds.minZ) && close(bounds.maxZ, originalBounds.maxZ),
                                            "path footprint and orientation");
                                    boolean flattenSupport = grass && shape != SoilPieces.Shape.SLAB;
                                    require(world.getBlockState(position.down()).equals(flattenSupport
                                            ? NativeSoils.path(0) : support), "matching native grass support path");
                                    if (actual.getBlock() instanceof SoilPathBlock) {
                                        SoilPathBlock path = (SoilPathBlock) actual.getBlock();
                                        require(path.shape == shape && path.orientation(actual) == orientation,
                                                "native path shape");
                                        require(path.material(actual) == (grass ? material - 2 : material),
                                                "loamy, sandy and silty paths retain their soil");
                                        require(!path.canSilkHarvest(world, position, actual, player), "path Silk Touch rule");
                                        ItemStack dropped = path.getDrops(world, position, actual, 0).get(0);
                                        ItemStack dirt = SoilPieces.stack(SoilPieces.state(false,
                                                grass ? material - 2 : material, shape, orientation), 1);
                                        require(ItemStack.areItemStacksEqual(dropped, dirt), "matching native dirt drop");
                                        if (flattenSupport && originalBounds.minY == 0) {
                                            require(close(bounds.minY, -1.0 / 16.0), "no gap over the lowered support");
                                        }
                                        world.setBlockState(position.up(), Blocks.STONE.getDefaultState(), 3);
                                        require(world.getBlockState(position).equals(path.dirt(actual)),
                                                "covered path returns to matching dirt geometry");
                                    }
                                    converted++;
                                }
                                interactions++;
                            }
                        }
                    }
                }
            }
        }
        player.setSneaking(false);
        IBlockState corner = SoilPieces.state(true, 2, SoilPieces.Shape.CORNER, 4);
        world.setBlockToAir(position.up());
        world.setBlockToAir(position);
        world.setBlockState(position.down(), Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(position, corner, 2);
        player.capabilities.isCreativeMode = true;
        ItemStack creative = new ItemStack(Items.IRON_SHOVEL);
        click(world, player, position, creative, EnumHand.MAIN_HAND, EnumFacing.UP);
        require(creative.getItemDamage() == 0 && world.getBlockState(position.down()).getBlock() == Blocks.STONE,
                "creative durability and unrelated supports");
        player.capabilities.isCreativeMode = false;

        world.setBlockState(position, corner, 2);
        world.setBlockState(position.up(), Blocks.STONE.getDefaultState(), 2);
        ItemStack blocked = new ItemStack(Items.IRON_SHOVEL);
        click(world, player, position, blocked, EnumHand.MAIN_HAND, EnumFacing.UP);
        require(world.getBlockState(position).equals(corner) && blocked.getItemDamage() == 0, "covered conversion rejected");
        world.setBlockToAir(position.up());
        ItemStack wrongTool = new ItemStack(Items.STICK);
        click(world, player, position, wrongTool, EnumHand.MAIN_HAND, EnumFacing.UP);
        require(world.getBlockState(position).equals(corner), "non-shovel rejected");
        player.capabilities.allowEdit = false;
        ItemStack denied = new ItemStack(Items.IRON_SHOVEL);
        click(world, player, position, denied, EnumHand.MAIN_HAND, EnumFacing.UP);
        require(world.getBlockState(position).equals(corner) && denied.getItemDamage() == 0, "edit permission respected");
        player.capabilities.allowEdit = true;

        Item multiTool = new Item() {
            @Override
            public Set<String> getToolClasses(ItemStack stack) {
                return new HashSet<String>(Arrays.asList("shovel", "axe"));
            }
        }.setMaxDamage(100);
        ItemStack compatible = new ItemStack(multiTool);
        click(world, player, position, compatible, EnumHand.MAIN_HAND, EnumFacing.UP);
        require(world.getBlockState(position).getBlock() instanceof SoilPathBlock
                && compatible.getItemDamage() == 1, "Forge tool-class compatibility");

        for (int material = 0; material < 3; material++) {
            world.setBlockToAir(position.up());
            world.setBlockToAir(position);
            world.setBlockState(position.down(), Blocks.STONE.getDefaultState(), 2);
            world.setBlockState(position, SoilPaths.state(material, SoilPieces.Shape.CORNER, 4), 2);
            ItemStack cornerItem = SoilPieces.stack(SoilPaths.state(material, SoilPieces.Shape.CORNER, 0), 1);
            cornerItem.getItem().onItemUse(cornerItem, player, world, position, EnumHand.MAIN_HAND,
                    EnumFacing.UP, .75F, .25F, .25F);
            require(world.getBlockState(position).equals(SoilPaths.state(material, SoilPieces.Shape.STEP, 4)),
                    "path corners join into a matching step");
            ItemStack stepItem = SoilPieces.stack(SoilPaths.state(material, SoilPieces.Shape.STEP, 0), 1);
            stepItem.getItem().onItemUse(stepItem, player, world, position, EnumHand.MAIN_HAND,
                    EnumFacing.UP, .5F, .25F, .75F);
            require(world.getBlockState(position).equals(SoilPaths.state(material, SoilPieces.Shape.SLAB, 1)),
                    "path steps join into a matching slab");
            ItemStack slabItem = SoilPieces.stack(SoilPaths.state(material, SoilPieces.Shape.SLAB, 0), 1);
            slabItem.getItem().onItemUse(slabItem, player, world, position, EnumHand.MAIN_HAND,
                    EnumFacing.UP, .5F, .75F, .5F);
            require(world.getBlockState(position).equals(NativeSoils.path(material)),
                    "path slabs join into their native BOP path block");
        }
        fixture(world, System.getProperty("skysterrainsmootherbop.integrationPhase", "fresh").contains("reload"));
        require(converted == 1440 && SoilPaths.BLOCKS.size() == 5, "conversion and appended palette coverage");
        System.out.println("BOP_TERRAIN_PATH_PASS interactions=" + interactions + " conversions=" + converted + " palettes=5");
        return interactions;
    }

    public static void fixture(WorldServer world, boolean reload) {
        int index = 0;
        BlockPos inventory = new BlockPos(160, 220, 96);
        world.getChunkFromBlockCoords(inventory);
        if (!reload) {
            world.setBlockState(inventory, Blocks.CHEST.getDefaultState(), 2);
            world.setBlockState(inventory.east(), Blocks.CHEST.getDefaultState(), 2);
        }
        for (int material = 0; material < 3; material++) {
            for (SoilPieces.Shape shape : SoilPieces.Shape.values()) {
                for (int orientation = 0; orientation < shape.states; orientation++) {
                    BlockPos position = new BlockPos(176 + (index % 9) * 2, 220, 96 + (index / 9) * 2);
                    world.getChunkFromBlockCoords(position);
                    IBlockState state = SoilPaths.state(material, shape, orientation);
                    TileEntityChest chest = (TileEntityChest) world.getTileEntity(inventory.add(index / 27, 0, 0));
                    require(chest != null, "path fixture chest");
                    if (!reload) {
                        world.setBlockToAir(position.up());
                        world.setBlockState(position.down(), index % 2 == 0
                                ? NativeSoils.path(material) : NativeSoils.full(false, material), 2);
                        world.setBlockState(position, state, 2);
                        ItemStack stack = new ItemStack(state.getBlock(), index + 1, state.getBlock().getMetaFromState(state));
                        NBTTagCompound data = new NBTTagCompound();
                        data.setString("bop_path_fixture", material + "/" + shape + "/" + orientation);
                        stack.setTagCompound(data);
                        stack.setStackDisplayName("Saved BOP path " + index);
                        chest.setInventorySlotContents(index % 27, stack);
                    } else {
                        require(world.getBlockState(position).equals(state), "saved path identity and orientation " + index);
                        ItemStack stack = chest.getStackInSlot(index % 27);
                        require(stack != null && stack.stackSize == index + 1
                                && stack.getItem() == Item.getItemFromBlock(state.getBlock())
                                && stack.getItemDamage() == state.getBlock().getMetaFromState(state)
                                && stack.getTagCompound().getString("bop_path_fixture")
                                        .equals(material + "/" + shape + "/" + orientation)
                                && stack.getDisplayName().equals("Saved BOP path " + index),
                                "saved path stack count, metadata and NBT " + index);
                    }
                    index++;
                }
            }
        }
        require(index == 54, "all saved native BOP path states");
        System.out.println("BOP_TERRAIN_PATH_FIXTURE_PASS reload=" + reload + " states=54 stacks=54 units=1485");
    }

    private static EnumActionResult click(WorldServer world, EntityPlayerMP player, BlockPos position,
            ItemStack stack, EnumHand hand, EnumFacing face) {
        player.setHeldItem(hand, stack);
        return player.interactionManager.processRightClickBlock(player, world, stack, hand,
                position, face, .25F, .5F, .25F);
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 1.0e-9;
    }

    private static void require(boolean condition, String message) {
        RuntimeProbe.require(condition, message);
    }

    private PathChecks() {
    }
}
