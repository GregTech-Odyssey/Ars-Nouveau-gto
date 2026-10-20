package com.hollingsworth.arsnouveau.api.source;

import com.suried.gtoutils.spatial.BlockIndex;
import com.suried.gtoutils.spatial.BlockRange;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 一个维度内可能提供某种能力的对象索引：确定提供的直接进候选树，其余先进待探测树，首次被查询覆盖时探测一次。
 */
public final class ProbedBlockIndex<T> {

    public enum Kind {
        NONE,
        CANDIDATE,
        PROBE
    }

    private final BlockIndex<T> candidates = new BlockIndex<>();
    private final BlockIndex<T> pending = new BlockIndex<>();
    private final Function<? super T, BlockPos> pos;
    private final ArrayList<T> scratch = new ArrayList<>();

    public ProbedBlockIndex(Function<? super T, BlockPos> pos) {
        this.pos = pos;
    }

    public void add(T value, Kind kind) {
        BlockPos p = pos.apply(value);
        if (kind == Kind.CANDIDATE) {
            pending.remove(p);
            candidates.put(p, value);
        } else if (kind == Kind.PROBE) {
            candidates.remove(p);
            pending.put(p, value);
        } else {
            candidates.remove(p);
            pending.remove(p);
        }
    }

    public void remove(T value) {
        BlockPos p = pos.apply(value);
        candidates.remove(p, value);
        pending.remove(p, value);
    }

    public void forEach(BlockPos center, int range, Predicate<? super T> probe, Consumer<? super T> action) {
        BlockRange box = BlockRange.box(range);
        probePending(center, box, probe);
        if (!candidates.isEmpty()) candidates.query(center.getX(), center.getY(), center.getZ(), box, t -> true, action);
    }

    public T findAny(BlockPos center, int range, Predicate<? super T> probe, Predicate<? super T> filter) {
        BlockRange box = BlockRange.box(range);
        probePending(center, box, probe);
        return candidates.isEmpty() ? null : candidates.findAny(center, box, filter);
    }

    private void probePending(BlockPos center, BlockRange box, Predicate<? super T> probe) {
        if (pending.isEmpty()) return;
        scratch.clear();
        pending.query(center, box, t -> true, scratch);
        for (T t : scratch) {
            BlockPos p = pos.apply(t);
            pending.remove(p, t);
            if (probe.test(t)) candidates.put(p, t);
        }
        scratch.clear();
    }

    public int candidateCount() {
        return candidates.size();
    }

    public int pendingCount() {
        return pending.size();
    }
}
