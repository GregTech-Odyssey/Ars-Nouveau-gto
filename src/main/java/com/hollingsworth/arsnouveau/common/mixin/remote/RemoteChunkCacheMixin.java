package com.hollingsworth.arsnouveau.common.mixin.remote;

import com.hollingsworth.arsnouveau.common.util.RemoteMenuAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public abstract class RemoteChunkCacheMixin {

    @Shadow
    volatile ClientChunkCache.Storage storage;

    @Shadow
    @Final
    ClientLevel level;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ars_nouveau$resetRemoteChunk(ClientLevel level, int viewDistance, CallbackInfo ci) {
        RemoteMenuAccess.Client.reset();
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"), cancellable = true)
    private void ars_nouveau$loadRemoteChunk(int x, int z, FriendlyByteBuf buffer, CompoundTag heightmaps,
                                             Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities,
                                             CallbackInfoReturnable<LevelChunk> cir) {
        if (storage.inRange(x, z)) {
            RemoteMenuAccess.Client.onVanillaLoad(x, z);
        } else if (RemoteMenuAccess.Client.expects(x, z)) {
            cir.setReturnValue(RemoteMenuAccess.Client.load(level, x, z, buffer, heightmaps, blockEntities));
        }
    }

    @ModifyReturnValue(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("RETURN"))
    private LevelChunk ars_nouveau$remoteChunk(LevelChunk original, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int z) {
        return RemoteMenuAccess.Client.holding() && (original == null || original instanceof EmptyLevelChunk) ? RemoteMenuAccess.Client.orHeld(original, x, z) : original;
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void ars_nouveau$dropRemoteChunk(int x, int z, CallbackInfo ci) {
        RemoteMenuAccess.Client.onDrop(level, x, z);
    }
}
