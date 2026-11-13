package com.hollingsworth.arsnouveau.common.mixin.remote;

import com.hollingsworth.arsnouveau.common.util.RemoteMenuAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class MenuPacketMixin {

    @WrapOperation(method = {"handleContainerClick", "handleContainerButtonClick", "handlePlaceRecipe", "handleSetBeaconPacket"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean ars_nouveau$remoteStillValid(AbstractContainerMenu menu, Player player, Operation<Boolean> original) {
        return RemoteMenuAccess.stillValid(menu, player, original);
    }

    @WrapOperation(method = "handleRenameItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean ars_nouveau$remoteAnvilStillValid(AnvilMenu menu, Player player, Operation<Boolean> original) {
        return RemoteMenuAccess.stillValid(menu, player, original);
    }
}
