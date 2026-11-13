package com.hollingsworth.arsnouveau.common.mixin.remote;

import com.hollingsworth.arsnouveau.common.util.RemoteMenuAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public class ServerPlayerMenuMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean ars_nouveau$remoteStillValid(AbstractContainerMenu menu, Player player, Operation<Boolean> original) {
        return RemoteMenuAccess.stillValid(menu, player, original);
    }
}
