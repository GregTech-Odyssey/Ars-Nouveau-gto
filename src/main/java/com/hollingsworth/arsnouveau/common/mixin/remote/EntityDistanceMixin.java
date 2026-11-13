package com.hollingsworth.arsnouveau.common.mixin.remote;

import com.hollingsworth.arsnouveau.common.util.RemoteMenuAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityDistanceMixin {

    @ModifyReturnValue(method = "distanceToSqr(DDD)D", at = @At("RETURN"))
    private double ars_nouveau$remoteDistance(double original, @Local(argsOnly = true, ordinal = 0) double x,
                                              @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z) {
        return RemoteMenuAccess.isViewer((Entity) (Object) this) ? RemoteMenuAccess.originDistanceSqr(x, y, z) : original;
    }

    @ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"))
    private double ars_nouveau$remoteDistance(double original, @Local(argsOnly = true) Vec3 vec) {
        return RemoteMenuAccess.isViewer((Entity) (Object) this) ? RemoteMenuAccess.originDistanceSqr(vec.x, vec.y, vec.z) : original;
    }
}
