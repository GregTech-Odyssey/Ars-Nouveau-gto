package com.hollingsworth.arsnouveau.common.network;

import com.hollingsworth.arsnouveau.api.particle.ParticleColorRegistry;
import com.hollingsworth.arsnouveau.client.ClientFxHandler;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A visual event the client renders on its own: flying projectiles and items, poofs, orbs and beams. Replaces server-spawned
 * visual entities and per-particle packets with one small message.
 */
public class PacketClientFx {
    public enum Type {FOLLOW, FLYING, POOF, ORB, BEAM, EMIT, WINDSHEAR}

    public final Type type;
    public final Vec3 start;
    public final Vec3 end;
    public final BlockPos from;
    public final BlockPos to;
    public final ParticleColor color;
    public final boolean touch;
    public final int extra;
    public final float offset;
    public final ItemStack stack;

    public PacketClientFx(Type type, Vec3 start, Vec3 end, BlockPos from, BlockPos to, ParticleColor color, boolean touch, int extra, float offset, ItemStack stack) {
        this.type = type;
        this.start = start;
        this.end = end;
        this.from = from;
        this.to = to;
        this.color = color;
        this.touch = touch;
        this.extra = extra;
        this.offset = offset;
        this.stack = stack;
    }

    public static void encode(PacketClientFx msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.type.ordinal());
        switch (msg.type) {
            case FOLLOW -> {
                writeVec(buf, msg.start);
                buf.writeBlockPos(msg.from);
                buf.writeBlockPos(msg.to);
                writeColor(buf, msg.color);
                buf.writeBoolean(msg.touch);
                buf.writeVarInt(msg.extra);
            }
            case FLYING -> {
                writeVec(buf, msg.start);
                writeVec(buf, msg.end);
                writeColor(buf, msg.color);
                buf.writeBoolean(msg.touch);
                buf.writeFloat(msg.offset);
                buf.writeItem(msg.stack);
            }
            case POOF -> buf.writeBlockPos(msg.from);
            case ORB -> {
                buf.writeBlockPos(msg.from);
                writeColor(buf, msg.color);
                buf.writeVarInt(msg.extra);
            }
            case BEAM -> {
                buf.writeBlockPos(msg.from);
                buf.writeBlockPos(msg.to);
            }
            case EMIT -> {
                writeVec(buf, msg.start);
                buf.writeVarInt(msg.extra);
                buf.writeVarInt((int) msg.offset);
                buf.writeVarInt(msg.from.getX());
            }
            case WINDSHEAR -> writeVec(buf, msg.start);
        }
    }

    public static PacketClientFx decode(FriendlyByteBuf buf) {
        Type type = Type.values()[buf.readByte()];
        Vec3 start = Vec3.ZERO, end = Vec3.ZERO;
        BlockPos from = BlockPos.ZERO, to = BlockPos.ZERO;
        ParticleColor color = ParticleColor.DEFAULT;
        boolean touch = false;
        int extra = 0;
        float offset = Float.NaN;
        ItemStack stack = ItemStack.EMPTY;
        switch (type) {
            case FOLLOW -> {
                start = readVec(buf);
                from = buf.readBlockPos();
                to = buf.readBlockPos();
                color = readColor(buf);
                touch = buf.readBoolean();
                extra = buf.readVarInt();
            }
            case FLYING -> {
                start = readVec(buf);
                end = readVec(buf);
                color = readColor(buf);
                touch = buf.readBoolean();
                offset = buf.readFloat();
                stack = buf.readItem();
            }
            case POOF -> from = buf.readBlockPos();
            case ORB -> {
                from = buf.readBlockPos();
                color = readColor(buf);
                extra = buf.readVarInt();
            }
            case BEAM -> {
                from = buf.readBlockPos();
                to = buf.readBlockPos();
            }
            case EMIT -> {
                start = readVec(buf);
                extra = buf.readVarInt();
                offset = buf.readVarInt();
                from = new BlockPos(buf.readVarInt(), 0, 0);
            }
            case WINDSHEAR -> start = readVec(buf);
        }
        return new PacketClientFx(type, start, end, from, to, color, touch, extra, offset, stack);
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    static void writeColor(FriendlyByteBuf buf, ParticleColor color) {
        if (color.getClass() == ParticleColor.class) {
            buf.writeBoolean(false);
            buf.writeInt(color.getColor());
        } else {
            buf.writeBoolean(true);
            buf.writeNbt(color.serialize());
        }
    }

    static ParticleColor readColor(FriendlyByteBuf buf) {
        return buf.readBoolean() ? ParticleColorRegistry.from(buf.readNbt()) : ParticleColor.fromInt(buf.readInt());
    }

    public static void handle(PacketClientFx msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(new Runnable() {
                @Override
                public void run() {
                    ClientFxHandler.handle(msg);
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
