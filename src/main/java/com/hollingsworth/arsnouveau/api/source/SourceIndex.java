package com.hollingsworth.arsnouveau.api.source;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourcelinkTile;
import com.suried.gtoutils.spatial.BlockIndex;
import com.suried.gtoutils.spatial.BlockRange;
import com.suried.gtoutils.spatial.LevelBlockIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * 魔源罐与事件型源质链的按维度八叉树索引，方块实体加载时登记、移除时注销；查询结果按原版 withinManhattan 顺序排列。
 */
public final class SourceIndex {

    private static final LevelBlockIndex<SourceJarTile> JARS = LevelBlockIndex.create();
    private static final LevelBlockIndex<SourcelinkTile> EVENT_SOURCELINKS = LevelBlockIndex.create();

    private SourceIndex() {}

    public static void register(SourceJarTile jar) {
        Level level = jar.getLevel();
        if (level != null) JARS.get(level).put(jar.getBlockPos(), jar);
    }

    public static void unregister(SourceJarTile jar) {
        Level level = jar.getLevel();
        if (level == null) return;
        BlockIndex<SourceJarTile> index = JARS.getIfPresent(level);
        if (index != null) index.remove(jar.getBlockPos(), jar);
    }

    public static List<SourceJarTile> jarsAround(Level level, BlockPos center, int range, Predicate<? super SourceJarTile> filter) {
        BlockIndex<SourceJarTile> index = JARS.getIfPresent(level);
        if (index == null || index.isEmpty()) return Collections.emptyList();
        List<SourceJarTile> out = new ArrayList<>();
        index.query(center, BlockRange.box(range), filter, out);
        ManhattanOrder.sort(out, center, SourceJarTile::getBlockPos);
        return out;
    }

    public static boolean anyJarAround(Level level, BlockPos center, int range, Predicate<? super SourceJarTile> filter) {
        BlockIndex<SourceJarTile> index = JARS.getIfPresent(level);
        return index != null && index.findAny(center, BlockRange.box(range), filter) != null;
    }

    public static void registerSourcelink(SourcelinkTile tile) {
        Level level = tile.getLevel();
        if (level != null) EVENT_SOURCELINKS.get(level).put(tile.getBlockPos(), tile);
    }

    public static void unregisterSourcelink(SourcelinkTile tile) {
        Level level = tile.getLevel();
        if (level == null) return;
        BlockIndex<SourcelinkTile> index = EVENT_SOURCELINKS.getIfPresent(level);
        if (index != null) index.remove(tile.getBlockPos(), tile);
    }

    public static SourcelinkTile nearestSourcelink(Level level, BlockPos center, int range, Predicate<? super SourcelinkTile> filter) {
        BlockIndex<SourcelinkTile> index = EVENT_SOURCELINKS.getIfPresent(level);
        return index == null ? null : index.findNearest(center, BlockRange.sphere(range), filter);
    }
}
