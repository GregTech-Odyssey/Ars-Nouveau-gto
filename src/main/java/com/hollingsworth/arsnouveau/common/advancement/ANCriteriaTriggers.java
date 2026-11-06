package com.hollingsworth.arsnouveau.common.advancement;

import com.hollingsworth.arsnouveau.ArsNouveau;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public class ANCriteriaTriggers {
    public static final PlayerTrigger POOF_MOB = register(new PlayerTrigger(ArsNouveau.id( "poof_mob")));
    public static final PlayerTrigger FAMILIAR = register(new PlayerTrigger(ArsNouveau.id( "familiar")));
    public static final PlayerTrigger CHIMERA_EXPLOSION = register(new PlayerTrigger(ArsNouveau.id( "chimera_explosion")));
    public static final PlayerTrigger CREATE_PORTAL = register(new PlayerTrigger(ArsNouveau.id( "portals")));
    public static final PlayerTrigger PRISMATIC = register(new PlayerTrigger(ArsNouveau.id( "prismatic")));
    public static final PlayerTrigger SHRUNK_STARBY = register(new PlayerTrigger(ArsNouveau.id( "shrunk_starby")));
    public static final PlayerTrigger CAUGHT_LIGHTNING = register(new PlayerTrigger(ArsNouveau.id( "catch_lightning")));
    public static final PlayerTrigger TIME_IN_BOTTLE = register(new PlayerTrigger(ArsNouveau.id( "time_in_bottle")));

    public static void rewardNearbyPlayers(PlayerTrigger criteria, ServerLevel level, BlockPos pos, int radius){
        AABB aabb = new AABB(pos).inflate(radius);
        for (ServerPlayer player : level.players()) {
            if (aabb.contains(player.getX(), player.getY(), player.getZ())) {
                criteria.trigger(player);
            }
        }
    }

    public static <T extends CriterionTrigger<?>> T register(T trigger) {
        return CriteriaTriggers.register(trigger);
    }

    public static void init() {}
}
