package com.hollingsworth.arsnouveau.api.source;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 可能带 {@code SOURCE_TILE} 能力的方块实体（魔源罐除外）的按维度索引，由区块放入、移除方块实体时登记，替代逐格扫描。
 */
@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public final class SourceTileIndex {

    private static final Map<LevelAccessor, ProbedBlockIndex<BlockEntity>> LEVELS = new ConcurrentHashMap<>();
    private static final List<Predicate<Class<?>>> CANDIDATE_CLASSES = new CopyOnWriteArrayList<>();
    private static volatile Predicate<BlockEntity> probe;
    private static final ClassValue<ProbedBlockIndex.Kind> KINDS = new ClassValue<>() {
        @Override
        protected ProbedBlockIndex.Kind computeValue(Class<?> type) {
            if (SourceJarTile.class.isAssignableFrom(type)) return ProbedBlockIndex.Kind.NONE;
            if (ISourceTile.class.isAssignableFrom(type)) return ProbedBlockIndex.Kind.CANDIDATE;
            for (Predicate<Class<?>> rule : CANDIDATE_CLASSES) {
                if (rule.test(type)) return ProbedBlockIndex.Kind.CANDIDATE;
            }
            return probe != null ? ProbedBlockIndex.Kind.PROBE : ProbedBlockIndex.Kind.NONE;
        }
    };

    private SourceTileIndex() {}

    public static void registerCandidateClass(Predicate<Class<?>> rule) {
        CANDIDATE_CLASSES.add(rule);
    }

    public static void registerProbe(Predicate<BlockEntity> test) {
        probe = test;
    }

    public static void onAdded(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null) return;
        ProbedBlockIndex.Kind kind = KINDS.get(be.getClass());
        if (kind != ProbedBlockIndex.Kind.NONE) {
            LEVELS.computeIfAbsent(level, l -> new ProbedBlockIndex<>(BlockEntity::getBlockPos)).add(be, kind);
        }
    }

    public static void onRemoved(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null) return;
        ProbedBlockIndex<BlockEntity> index = LEVELS.get(level);
        if (index != null) index.remove(be);
    }

    public static void forEachAround(Level level, BlockPos center, int range, Consumer<BlockEntity> action) {
        ProbedBlockIndex<BlockEntity> index = LEVELS.get(level);
        if (index != null) index.forEach(center, range, probe(), action);
    }

    public static boolean anyAround(Level level, BlockPos center, int range, Predicate<BlockEntity> filter) {
        ProbedBlockIndex<BlockEntity> index = LEVELS.get(level);
        return index != null && index.findAny(center, range, probe(), filter) != null;
    }

    private static Predicate<BlockEntity> probe() {
        Predicate<BlockEntity> test = probe;
        return test == null ? be -> false : test;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LEVELS.remove(event.getLevel());
    }
}
