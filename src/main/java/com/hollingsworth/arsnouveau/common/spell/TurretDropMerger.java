package com.hollingsworth.arsnouveau.common.spell;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

// Holds back the item drops of one turret cast and respawns them merged by item type at the center block of the drops.
@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public final class TurretDropMerger {

    private static Capture active;

    private TurretDropMerger() {}

    public static void run(Level level, SpellContext context, Runnable action) {
        if (!(level instanceof ServerLevel serverLevel) || context.getCaster().getCasterType() != SpellContext.CasterType.TURRET) {
            action.run();
            return;
        }
        Capture previous = active;
        Capture capture = new Capture(serverLevel);
        active = capture;
        try {
            action.run();
        } finally {
            active = previous;
            capture.release();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        Capture capture = active;
        if (capture == null || event.getLevel() != capture.level || event.loadedFromDisk()) return;
        if (event.getEntity().getClass() != ItemEntity.class) return;
        ItemEntity entity = (ItemEntity) event.getEntity();
        if (entity.getItem().isEmpty()) return;
        capture.add(entity);
        event.setCanceled(true);
    }

    private static final class Capture {

        final ServerLevel level;
        final List<ItemStack> stacks = new ArrayList<>();
        final List<BlockPos> sources = new ArrayList<>();
        long sumX, sumY, sumZ;

        Capture(ServerLevel level) {
            this.level = level;
        }

        void add(ItemEntity entity) {
            BlockPos pos = entity.blockPosition();
            sources.add(pos);
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
            ItemStack stack = entity.getItem();
            for (ItemStack merged : stacks) {
                if (merged.getCount() >= merged.getMaxStackSize() || !ItemStack.isSameItemSameTags(merged, stack)) continue;
                int moved = Math.min(stack.getCount(), merged.getMaxStackSize() - merged.getCount());
                merged.grow(moved);
                stack = stack.copyWithCount(stack.getCount() - moved);
                if (stack.isEmpty()) return;
            }
            stacks.add(stack.copy());
        }

        void release() {
            if (stacks.isEmpty()) return;
            BlockPos center = center();
            double x = center.getX() + 0.5D;
            double y = center.getY() + 0.5D - EntityType.ITEM.getHeight() / 2.0D;
            double z = center.getZ() + 0.5D;
            for (ItemStack stack : stacks) {
                ItemEntity entity = new ItemEntity(level, x, y, z, stack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            }
        }

        BlockPos center() {
            int n = sources.size();
            double cx = (double) sumX / n, cy = (double) sumY / n, cz = (double) sumZ / n;
            BlockPos best = sources.get(0);
            double bestDist = Double.MAX_VALUE;
            for (BlockPos pos : sources) {
                double dx = pos.getX() - cx, dy = pos.getY() - cy, dz = pos.getZ() - cz;
                double dist = dx * dx + dy * dy + dz * dz;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = pos;
                }
            }
            return best;
        }
    }
}
