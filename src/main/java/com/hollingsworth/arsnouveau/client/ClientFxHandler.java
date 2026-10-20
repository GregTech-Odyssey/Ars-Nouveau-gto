package com.hollingsworth.arsnouveau.client;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.client.particle.GlowParticleData;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.entity.ColoredProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.hollingsworth.arsnouveau.common.network.PacketClientFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Client half of PacketClientFx: rebuilds the visual locally, as client-only entities or particles.
 */
@Mod.EventBusSubscriber(modid = ArsNouveau.MODID, value = Dist.CLIENT)
public final class ClientFxHandler {
    private static int nextId = -1_000_000;

    private ClientFxHandler() {}

    public static void handle(PacketClientFx msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        switch (msg.type) {
            case FOLLOW -> {
                EntityFollowProjectile e = new EntityFollowProjectile(level);
                e.getEntityData().set(EntityFollowProjectile.from, msg.from);
                e.getEntityData().set(EntityFollowProjectile.to, msg.to);
                setColor(e, msg.color);
                e.getEntityData().set(EntityFollowProjectile.SPAWN_TOUCH, msg.touch);
                e.getEntityData().set(EntityFollowProjectile.DESPAWN, msg.extra);
                e.setPos(msg.start.x, msg.start.y, msg.start.z);
                addLocal(level, e);
            }
            case FLYING -> {
                EntityFlyingItem e = new EntityFlyingItem(level, msg.start, msg.end, colorInt(msg.color, 16), colorInt(msg.color, 8), colorInt(msg.color, 0));
                e.setStack(msg.stack);
                if (!msg.touch) {
                    e.withNoTouch();
                }
                if (!Float.isNaN(msg.offset)) {
                    e.setDistanceAdjust(msg.offset);
                }
                addLocal(level, e);
            }
            case POOF -> {
                BlockPos pos = msg.from;
                RandomSource random = level.random;
                for (int i = 0; i < 10; i++) {
                    emit(level, ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 2,
                            (random.nextFloat() - 0.5) / 3, (random.nextFloat() - 0.5) / 3, (random.nextFloat() - 0.5) / 3, 0.1f);
                }
            }
            case ORB -> {
                BlockPos pos = msg.from;
                for (int i = 0; i <= 10; i++) {
                    emit(level, GlowParticleData.createData(msg.color, 0.4f, 0.5f, msg.extra),
                            pos.getX() + ParticleUtil.inRange(0.3, 0.7), pos.getY() + ParticleUtil.inRange(-0.2, 0.2), pos.getZ() + ParticleUtil.inRange(0.3, 0.7), 1, 0, 0, 0, 0);
                }
            }
            case BEAM -> beam(level, msg.from, msg.to);
            case EMIT -> {
                if (BuiltInRegistries.PARTICLE_TYPE.byId(msg.from.getX()) instanceof SimpleParticleType particle && msg.extra > 0) {
                    EMITTERS.add(new Emitter(particle, msg.start, msg.extra, Math.max(1, (int) msg.offset)));
                }
            }
            case WINDSHEAR -> {
                Vec3 vec = msg.start;
                for (int i = 0; i < 10; i++) {
                    emit(level, ParticleTypes.SWEEP_ATTACK, vec.x + ParticleUtil.inRange(-0.2, 0.2), vec.y + 0.5 + ParticleUtil.inRange(-0.2, 0.2), vec.z + ParticleUtil.inRange(-0.2, 0.2), 30,
                            ParticleUtil.inRange(-0.2, 0.2), ParticleUtil.inRange(-0.2, 0.2), ParticleUtil.inRange(-0.2, 0.2), 0.3);
                }
            }
        }
    }

    private static final class Emitter {
        final SimpleParticleType particle;
        final Vec3 pos;
        final int chance;
        int ticks;

        Emitter(SimpleParticleType particle, Vec3 pos, int ticks, int chance) {
            this.particle = particle;
            this.pos = pos;
            this.ticks = ticks;
            this.chance = chance;
        }
    }

    private static final List<Emitter> EMITTERS = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || EMITTERS.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || Minecraft.getInstance().isPaused()) {
            if (level == null) {
                EMITTERS.clear();
            }
            return;
        }
        var it = EMITTERS.iterator();
        while (it.hasNext()) {
            Emitter e = it.next();
            if (e.chance <= 1 || level.random.nextInt(e.chance) == 0) {
                emit(level, e.particle, e.pos.x, e.pos.y, e.pos.z, 1, ParticleUtil.inRange(-0.2, 0.2), 0, ParticleUtil.inRange(-0.2, 0.2), 0.01);
            }
            if (--e.ticks <= 0) {
                it.remove();
            }
        }
    }

    public static void addLocal(ClientLevel level, Entity entity) {
        level.putNonPlayerEntity(nextId--, entity);
    }

    private static void setColor(ColoredProjectile e, ParticleColor color) {
        e.getEntityData().set(ColoredProjectile.RED, colorInt(color, 16));
        e.getEntityData().set(ColoredProjectile.GREEN, colorInt(color, 8));
        e.getEntityData().set(ColoredProjectile.BLUE, colorInt(color, 0));
    }

    private static int colorInt(ParticleColor color, int shift) {
        return (color.getColor() >> shift) & 0xFF;
    }

    private static void beam(ClientLevel level, BlockPos to, BlockPos from) {
        RandomSource r = level.random;
        double x2 = to.getX() + 0.5, y2 = to.getY() + 0.5, z2 = to.getZ() + 0.5;
        double x1 = from.getX() + 0.5, y1 = from.getY() + 0.5, z1 = from.getZ() + 0.5;
        double d5 = 1.2;
        double d0 = x2 - x1, d1 = y2 - y1, d2 = z2 - z1;
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        d0 /= d3;
        d1 /= d3;
        d2 /= d3;
        double d4 = r.nextDouble();
        while ((d4 + .65) < d3) {
            d4 += 1.8D - d5 + r.nextDouble() * (1.5D - d5);
            emit(level, ParticleTypes.WITCH, x1 + d0 * d4, y1 + d1 * d4, z1 + d2 * d4, r.nextInt(4), 0, 0, 0, 0);
        }
    }

    public static void emit(ClientLevel level, ParticleOptions options, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        RandomSource random = level.random;
        if (count == 0) {
            level.addParticle(options, false, x, y, z, speed * dx, speed * dy, speed * dz);
            return;
        }
        for (int i = 0; i < count; i++) {
            double ox = random.nextGaussian() * dx;
            double oy = random.nextGaussian() * dy;
            double oz = random.nextGaussian() * dz;
            double vx = random.nextGaussian() * speed;
            double vy = random.nextGaussian() * speed;
            double vz = random.nextGaussian() * speed;
            level.addParticle(options, false, x + ox, y + oy, z + oz, vx, vy, vz);
        }
    }
}
