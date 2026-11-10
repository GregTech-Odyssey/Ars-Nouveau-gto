package com.hollingsworth.arsnouveau.common.event;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketSyncLitEntities;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import com.suried.gtoutils.schedule.Cadence;
import com.suried.gtoutils.schedule.Schedulers;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class LightEvents {
    private static final Map<ServerPlayer, IntOpenHashSet> LAST_SENT = new WeakHashMap<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        Schedulers.server().repeat(Cadence.every(100), e.getServer(), LightEvents::syncLitPlayers, null);
    }

    private static void syncLitPlayers(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty())
            return;
        IntOpenHashSet litID = new IntOpenHashSet();
        for (ServerPlayer player : players) {
            NonNullList<ItemStack> list = player.inventory.items;
            for (int i = 0; i < 9; i++) {
                ItemStack jar = list.get(i);
                if (jar.getItem() == ItemsRegistry.JAR_OF_LIGHT.asItem()) {
                    litID.add(player.getId());
                    break;
                }
            }
        }
        for (ServerPlayer serverPlayer : players) {
            if (!litID.equals(LAST_SENT.get(serverPlayer))) {
                LAST_SENT.put(serverPlayer, litID);
                Networking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PacketSyncLitEntities(litID));
            }
        }
    }
}
