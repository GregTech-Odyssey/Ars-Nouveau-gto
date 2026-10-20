package com.hollingsworth.arsnouveau.client;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Regenerates the local player's mana on the client with the rate the server last sent; the server only corrects drift.
 */
@Mod.EventBusSubscriber(modid = ArsNouveau.MODID, value = Dist.CLIENT)
public final class ClientManaPrediction {
    private ClientManaPrediction() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ClientInfo.manaRegenInterval <= 0) {
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || mc.level.getGameTime() % ClientInfo.manaRegenInterval != 0
                || mc.level.getGameTime() <= ClientInfo.manaSyncTime) {
            return;
        }
        CapabilityRegistry.getMana(mc.player).ifPresent(mana -> {
            if (mana.getCurrentMana() < mana.getMaxMana()) {
                mana.addMana(ClientInfo.manaRegen);
            }
        });
    }
}
