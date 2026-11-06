package com.hollingsworth.arsnouveau.common.network;

import com.hollingsworth.arsnouveau.common.light.LightManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncLitEntities {
    IntOpenHashSet entityIDs = new IntOpenHashSet();

    //Decoder
    public PacketSyncLitEntities(FriendlyByteBuf buf) {
        int num = buf.readInt();
        for (int i = 0; i < num; i++) {
            entityIDs.add(buf.readInt());
        }
    }

    //Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityIDs.size());
        for (Integer i : entityIDs)
            buf.writeInt(i);
    }

    public PacketSyncLitEntities(IntOpenHashSet entityIDs) {
        this.entityIDs = entityIDs;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (LightManager.shouldUpdateDynamicLight()) {
                LightManager.jarHoldingEntityList = entityIDs;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
