package com.hollingsworth.arsnouveau.common.mob_jar;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.entity.IDispellable;
import com.hollingsworth.arsnouveau.api.entity.ISummon;
import com.hollingsworth.arsnouveau.api.mob_jar.JarBehavior;
import com.hollingsworth.arsnouveau.api.util.ArsBlockEntityIndexes;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.suried.gtoutils.spatial.BlockRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class WitherBehavior extends JarBehavior<WitherBoss> {

    private static final int ROSE_RANGE = 4;

    @Override
    public void onRedstonePower(MobJarTile tile) {
        destroyBlocks(tile);
    }

    private void destroyBlocks(MobJarTile tile) {
        BlockPos pos = tile.getBlockPos();
        WitherBoss entity = entityFromJar(tile);
        if (!ForgeEventFactory.getMobGriefingEvent(tile.getLevel(), entity)) return;
        for (BlockPos block : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (block.equals(pos)) continue;
            BlockState bs = tile.getLevel().getBlockState(block);
            if (bs.canEntityDestroy(tile.getLevel(), block, entity) && ForgeEventFactory.onEntityDestroyBlock(entity, block, bs)) {
                tile.getLevel().destroyBlock(block, true, entity);
            }
        }
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide() || entity instanceof IDispellable || entity instanceof ISummon)
            return;

        BlockPos blockPos = entity.blockPosition();
        Vec3 at = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        MobJarTile jar = ArsBlockEntityIndexes.MOB_JARS.findAny(level, blockPos, BlockRange.box(ROSE_RANGE + 1),
                tile -> tile.getEntity() instanceof WitherBoss && BlockUtil.distanceFrom(at, tile.getBlockPos().getCenter()) <= ROSE_RANGE);
        if (jar != null) {
            level.addFreshEntity(new ItemEntity(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), new ItemStack(Items.WITHER_ROSE)));
        }
    }
}
