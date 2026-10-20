package com.hollingsworth.arsnouveau.common.mixin;

import com.hollingsworth.arsnouveau.api.source.SourceTileIndex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @WrapOperation(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ars$indexAdded(Map<Object, Object> map, Object pos, Object value, Operation<Object> original) {
        Object old = original.call(map, pos, value);
        if (old != value) {
            if (old instanceof BlockEntity previous) SourceTileIndex.onRemoved(previous);
            SourceTileIndex.onAdded((BlockEntity) value);
        }
        return old;
    }

    @WrapOperation(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ars$indexRemoved(Map<Object, Object> map, Object pos, Operation<Object> original) {
        Object old = original.call(map, pos);
        if (old instanceof BlockEntity previous) SourceTileIndex.onRemoved(previous);
        return old;
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void ars$indexCleared(CallbackInfo ci) {
        for (BlockEntity be : ((LevelChunk) (Object) this).getBlockEntities().values()) {
            SourceTileIndex.onRemoved(be);
        }
    }
}
