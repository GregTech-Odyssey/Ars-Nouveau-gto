package com.hollingsworth.arsnouveau.common.ritual;

import com.hollingsworth.arsnouveau.api.util.ArsBlockEntityIndexes;
import com.hollingsworth.arsnouveau.common.network.ClientFx;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.advancement.ANCriteriaTriggers;
import com.hollingsworth.arsnouveau.common.block.MobJar;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import com.hollingsworth.arsnouveau.common.entity.Starbuncle;
import com.hollingsworth.arsnouveau.common.lib.EntityTags;
import com.hollingsworth.arsnouveau.common.lib.RitualLib;
import com.suried.gtoutils.spatial.BlockOrder;
import com.suried.gtoutils.spatial.BlockRange;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.List;

public class RitualMobCapture extends AbstractRitual {
    private static List<MobJarTile> emptyJarsAround(Level level, BlockPos center, int radius) {
        List<MobJarTile> jars = new ArrayList<>();
        ArsBlockEntityIndexes.MOB_JARS.query(level, center, BlockRange.box(radius),
                tile -> tile.getEntity() == null && level.getBlockState(tile.getBlockPos()).getBlock() instanceof MobJar, jars);
        BlockOrder.sortBetweenClosed(jars, MobJarTile::getBlockPos);
        return jars;
    }

    @Override
    protected void tick() {
        Level world = getWorld();
        int radius = 3;
        if (world.isClientSide) {
            BlockPos pos = getPos();
            ParticleUtil.spawnRitualAreaEffect(getPos(), getWorld(), rand, getCenterColor(), radius);
        }
        if (!getWorld().isClientSide && world.getGameTime() % 60 == 0) {
            boolean didWorkOnce = false;
            Level level = getWorld();
            BlockPos pos = getPos();
            //Get nearby source jars
            for (MobJarTile tile : emptyJarsAround(level, pos, radius)) {
                for(Entity e : level.getEntities((Entity)null, new AABB(tile.getBlockPos()).inflate(5), this::canJar)){
                    if(e instanceof Mob mob && ((Mob) e).isLeashed() && e.shouldBeSaved()){
                        if(mob.isLeashed()){
                            mob.dropLeash(true, true);
                        }
                    }
                    if (e instanceof Raider raider && raider.hasActiveRaid()) {
                        raider.getCurrentRaid().removeFromRaid(raider, false);
                    }
                    if(tile.setEntityData(e)){
                        e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                        EntityFlyingItem followProjectile = new EntityFlyingItem(level, e.position, Vec3.atCenterOf(tile.getBlockPos()), 100, 50, 100);
                        ClientFx.spawn(level, followProjectile);
                        ParticleUtil.spawnPoof((ServerLevel) level, e.getOnPos().above());
                        didWorkOnce = true;
                        if(e instanceof Starbuncle starbuncle){
                            ANCriteriaTriggers.rewardNearbyPlayers(ANCriteriaTriggers.SHRUNK_STARBY, (ServerLevel) level, starbuncle.blockPosition(), 10);
                        }
                        if(e instanceof LightningBolt bolt){
                            ANCriteriaTriggers.rewardNearbyPlayers(ANCriteriaTriggers.CAUGHT_LIGHTNING, (ServerLevel) level, bolt.blockPosition(), 10);
                        }
                        if(e instanceof ItemEntity item && item.getItem().getItem() == Items.CLOCK){
                            ANCriteriaTriggers.rewardNearbyPlayers(ANCriteriaTriggers.TIME_IN_BOTTLE, (ServerLevel) level, item.blockPosition(), 10);
                        }
                        break;
                    }
                }
            }
            if(didWorkOnce){
                this.setNeedsSource(true);
            }
        }
    }

    public boolean canJar(Entity e){
        if(e.getType().is(EntityTags.JAR_WHITELIST))
            return true;
        if(e.getType().is(EntityTags.JAR_BLACKLIST)){
            return false;
        }
        if(e instanceof PartEntity) {
            return false;
        }
        return e instanceof LivingEntity livingEntity && !(e instanceof Player) && !((LivingEntity) e).isDeadOrDying();
    }

    @Override
    public int getSourceCost() {
        return 500;
    }

    @Override
    public String getLangDescription() {
        return "Captures a nearby entity and places it into any nearby placed Containment Jars. After the first capture, this ritual requires additional source to continue. Mobs and jars must be within 3 blocks of the brazier.";
    }

    @Override
    public String getLangName() {
        return "Containment";
    }

    @Override
    public ResourceLocation getRegistryName() {
        return new ResourceLocation(ArsNouveau.MODID, RitualLib.CONTAINMENT);
    }
}
