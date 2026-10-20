package com.hollingsworth.arsnouveau.common.event;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateMana;
import com.hollingsworth.arsnouveau.setup.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class ManaCapEvents {
    public static double MEAN_TPS = 20.0;

    private static final Map<ServerPlayer, double[]> PREDICTED = new WeakHashMap<>();

    @SubscribeEvent
    public static void playerOnTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.getCommandSenderWorld().isClientSide || !(e.player instanceof ServerPlayer player))
            return;
        int interval = ServerConfig.REGEN_INTERVAL.get();
        long time = player.level().getGameTime();
        if (time % interval != 0)
            return;

        IManaCap mana = CapabilityRegistry.getMana(player).orElse(null);
        if (mana == null)
            return;
        double regenPerPhase = ManaUtil.getManaRegen(player) / Math.max(1, ((int) MEAN_TPS / interval));
        double regen = regenPerPhase * 2;
        double[] predicted = PREDICTED.computeIfAbsent(player, p -> new double[]{Double.NaN, Double.NaN, Double.NaN});
        if (mana.getCurrentMana() != mana.getMaxMana()) {
            mana.addMana(regen);
        }
        if (!Double.isNaN(predicted[0]) && predicted[0] < mana.getMaxMana()) {
            predicted[0] = Math.min(mana.getMaxMana(), predicted[0] + regen);
        }
        ManaUtil.Mana maxmana = ManaUtil.calcMaxMana(player);
        int max = maxmana.getRealMax();
        boolean maxChanged = mana.getMaxMana() != max;
        if (maxChanged) {
            mana.setMaxMana(max);
        }
        boolean full = mana.getCurrentMana() >= mana.getMaxMana();
        boolean heartbeat = time % (full ? 400 : 60) == 0;
        boolean drift = Double.isNaN(predicted[0]) || Math.abs(predicted[0] - mana.getCurrentMana()) > 0.01;
        boolean regenChanged = predicted[1] != regen;
        if (maxChanged || drift || regenChanged || heartbeat) {
            predicted[0] = mana.getCurrentMana();
            predicted[1] = regen;
            Networking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketUpdateMana(mana.getCurrentMana(), mana.getMaxMana(), mana.getGlyphBonus(), mana.getBookTier(), maxChanged ? maxmana.Reserve() : -1, regen, interval, time));
        }
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            PREDICTED.remove(player);
        }
    }

    @SubscribeEvent
    public static void playerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        syncPlayerEvent(e.getEntity());
    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone e) {
        if (e.getOriginal().level.isClientSide)
            return;

        CapabilityRegistry.getMana(e.getEntity()).ifPresent(newMana -> CapabilityRegistry.getMana(e.getOriginal()).ifPresent(origMana -> {
            newMana.setMaxMana(origMana.getMaxMana());
            newMana.setGlyphBonus(origMana.getGlyphBonus());
            newMana.setBookTier(origMana.getBookTier());
            Networking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) e.getEntity()), new PacketUpdateMana(newMana.getCurrentMana(), newMana.getMaxMana(), newMana.getGlyphBonus(), newMana.getBookTier()));
        }));
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        syncPlayerEvent(e.getEntity());
    }

    @SubscribeEvent
    public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        syncPlayerEvent(e.getEntity());
    }

    public static void syncPlayerEvent(Player playerEntity) {
        if (playerEntity instanceof ServerPlayer serverPlayer) {
            PREDICTED.remove(serverPlayer);
            CapabilityRegistry.getMana(playerEntity).ifPresent(mana -> {
                var manaCalc = ManaUtil.calcMaxMana(playerEntity);
                mana.setMaxMana(manaCalc.getRealMax());
                mana.setGlyphBonus(mana.getGlyphBonus());
                mana.setBookTier(mana.getBookTier());
                Networking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) playerEntity), new PacketUpdateMana(mana.getCurrentMana(), mana.getMaxMana(), mana.getGlyphBonus(), mana.getBookTier(), manaCalc.Reserve()));
            });
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.player.level.isClientSide)
            return;
        if (e.player.level.getGameTime() % 600 == 0 && e.player.getServer() != null) {

            double meanTickTime = mean(e.player.getServer().tickTimes) * 1.0E-6D;
            double meanTPS = Math.min(1000.0 / meanTickTime, 20);
            ManaCapEvents.MEAN_TPS = Math.max(1, meanTPS);
        }
    }

    private static long mean(long[] values) {
        long sum = 0L;
        for (long v : values)
            sum += v;
        return sum / values.length;
    }
}
