package com.hollingsworth.arsnouveau.common.network;

import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.entity.ColoredProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Server entry for purely visual effects: the client renders them from one small PacketClientFx instead of tracked entities
 * or per-particle packets.
 */
public final class ClientFx {
    private ClientFx() {}

    public static void spawn(Level level, Entity visual) {
        if (level.isClientSide) {
            return;
        }
        if (visual instanceof EntityFollowProjectile e) {
            var data = e.getEntityData();
            BlockPos from = data.get(EntityFollowProjectile.from);
            send(level, from, new PacketClientFx(PacketClientFx.Type.FOLLOW, e.position(), Vec3.ZERO, from, data.get(EntityFollowProjectile.to),
                    color(e), data.get(EntityFollowProjectile.SPAWN_TOUCH), data.get(EntityFollowProjectile.DESPAWN), Float.NaN, ItemStack.EMPTY));
        } else if (visual instanceof EntityFlyingItem e) {
            var data = e.getEntityData();
            Vec3 from = data.get(EntityFlyingItem.from);
            float offset = data.get(EntityFlyingItem.DIDOFFSET) ? data.get(EntityFlyingItem.OFFSET) : Float.NaN;
            send(level, BlockPos.containing(from), new PacketClientFx(PacketClientFx.Type.FLYING, from, data.get(EntityFlyingItem.to), BlockPos.ZERO, BlockPos.ZERO,
                    color(e), data.get(EntityFlyingItem.SPAWN_TOUCH), 0, offset, e.getStack()));
        } else {
            level.addFreshEntity(visual);
        }
    }

    public static void follow(Level level, BlockPos from, BlockPos to, ParticleColor color) {
        if (level.isClientSide || !level.isLoaded(to) || !level.isLoaded(from)) {
            return;
        }
        spawn(level, new EntityFollowProjectile(level, from, to, color.toWrapper()));
    }

    public static void poof(Level level, BlockPos pos) {
        send(level, pos, new PacketClientFx(PacketClientFx.Type.POOF, Vec3.ZERO, Vec3.ZERO, pos, BlockPos.ZERO, ParticleColor.DEFAULT, false, 0, Float.NaN, ItemStack.EMPTY));
    }

    public static void orb(Level level, ParticleColor color, BlockPos pos, int lifetime) {
        send(level, pos, new PacketClientFx(PacketClientFx.Type.ORB, Vec3.ZERO, Vec3.ZERO, pos, BlockPos.ZERO, color, false, lifetime, Float.NaN, ItemStack.EMPTY));
    }

    public static void beam(Level level, BlockPos to, BlockPos from) {
        send(level, from, new PacketClientFx(PacketClientFx.Type.BEAM, Vec3.ZERO, Vec3.ZERO, to, from, ParticleColor.DEFAULT, false, 0, Float.NaN, ItemStack.EMPTY));
    }

    public static void emit(Level level, SimpleParticleType particle, Vec3 pos, int ticks, int chance) {
        int id = BuiltInRegistries.PARTICLE_TYPE.getId(particle);
        send(level, BlockPos.containing(pos), new PacketClientFx(PacketClientFx.Type.EMIT, pos, Vec3.ZERO, new BlockPos(id, 0, 0), BlockPos.ZERO, ParticleColor.DEFAULT, false, ticks, chance, ItemStack.EMPTY));
    }

    public static void windshear(Level level, Vec3 pos) {
        send(level, BlockPos.containing(pos), new PacketClientFx(PacketClientFx.Type.WINDSHEAR, pos, Vec3.ZERO, BlockPos.ZERO, BlockPos.ZERO, ParticleColor.DEFAULT, false, 0, Float.NaN, ItemStack.EMPTY));
    }

    private static ParticleColor color(ColoredProjectile e) {
        var data = e.getEntityData();
        return new ParticleColor(data.get(ColoredProjectile.RED), data.get(ColoredProjectile.GREEN), data.get(ColoredProjectile.BLUE));
    }

    private static void send(Level level, BlockPos pos, PacketClientFx packet) {
        if (!level.isClientSide) {
            Networking.sendToNearby(level, pos, packet);
        }
    }
}
