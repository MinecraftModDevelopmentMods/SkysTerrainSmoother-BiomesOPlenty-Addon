package zone.moddev.mc.skysterrainsmootherbop.probe;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class ProbeItems {
    private ProbeItems() {
    }

    static ServerPlayer player(ServerLevel world, GameProfile profile) {
        return new ServerPlayer(world.getServer(), world, profile, ClientInformation.createDefault());
    }

    static ItemStack read(ServerLevel world, CompoundTag data) {
        return ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), data).result()
                .orElseThrow(() -> new AssertionError("The recovered item could not be read: " + data));
    }

    static ItemStack readPreComponentStack(ServerLevel world, CompoundTag data) {
        int targetVersion = net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
        CompoundTag converted = (CompoundTag) world.getServer().getFixerUpper().update(References.ITEM_STACK,
                new Dynamic<>(NbtOps.INSTANCE, data), 3700, targetVersion).getValue();
        return read(world, converted);
    }

    static CompoundTag custom(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
