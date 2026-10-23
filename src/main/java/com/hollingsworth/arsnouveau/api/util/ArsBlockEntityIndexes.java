package com.hollingsworth.arsnouveau.api.util;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.tile.ArcanePedestalTile;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.PotionJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourcelinkTile;
import com.hollingsworth.arsnouveau.common.block.tile.SpellSensorTile;
import com.suried.gtoutils.spatial.BlockEntityIndex;
import com.suried.gtoutils.spatial.ProbedBlockEntityIndex;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 新生魔艺全部按范围查找方块实体所用的索引，由 GTOUtils 在区块增删方块实体时自动维护；模组构造时调用 {@link #init()} 完成创建。
 */
public final class ArsBlockEntityIndexes {

    private static final List<Predicate<Class<?>>> SOURCE_CANDIDATE_CLASSES = new CopyOnWriteArrayList<>();
    private static volatile Predicate<BlockEntity> sourceProbe;

    public static final BlockEntityIndex<BlockEntity> ALL = BlockEntityIndex.all();
    public static final BlockEntityIndex<SourceJarTile> SOURCE_JARS = BlockEntityIndex.create(SourceJarTile.class, SourceJarTile::isStorageAnchor);
    public static final BlockEntityIndex<SourcelinkTile> EVENT_SOURCELINKS = BlockEntityIndex.create(SourcelinkTile.class,
            tile -> tile.usesEventQueue() && !tile.getLevel().isClientSide);
    public static final ProbedBlockEntityIndex SOURCE_TILES = ProbedBlockEntityIndex.create(ArsBlockEntityIndexes::sourceKind,
            be -> {
                Predicate<BlockEntity> probe = sourceProbe;
                return probe != null && probe.test(be);
            });
    public static final BlockEntityIndex<ArcanePedestalTile> PEDESTALS = BlockEntityIndex.create(ArcanePedestalTile.class);
    public static final BlockEntityIndex<MobJarTile> MOB_JARS = BlockEntityIndex.create(MobJarTile.class);
    public static final BlockEntityIndex<PotionJarTile> POTION_JARS = BlockEntityIndex.create(PotionJarTile.class);
    public static final BlockEntityIndex<SpellSensorTile> SPELL_SENSORS = BlockEntityIndex.create(SpellSensorTile.class,
            tile -> !tile.getLevel().isClientSide);
    public static final BlockEntityIndex<RitualBrazierTile> RITUAL_BRAZIERS = BlockEntityIndex.create(RitualBrazierTile.class,
            tile -> !tile.getLevel().isClientSide);

    private ArsBlockEntityIndexes() {}

    public static void init() {}

    public static void registerSourceCandidateClass(Predicate<Class<?>> rule) {
        SOURCE_CANDIDATE_CLASSES.add(rule);
    }

    public static void registerSourceProbe(Predicate<BlockEntity> probe) {
        sourceProbe = probe;
    }

    private static ProbedBlockEntityIndex.Kind sourceKind(Class<?> type) {
        if (SourceJarTile.class.isAssignableFrom(type)) return ProbedBlockEntityIndex.Kind.NONE;
        if (ISourceTile.class.isAssignableFrom(type)) return ProbedBlockEntityIndex.Kind.CANDIDATE;
        for (Predicate<Class<?>> rule : SOURCE_CANDIDATE_CLASSES) {
            if (rule.test(type)) return ProbedBlockEntityIndex.Kind.CANDIDATE;
        }
        return sourceProbe != null ? ProbedBlockEntityIndex.Kind.PROBE : ProbedBlockEntityIndex.Kind.NONE;
    }
}
