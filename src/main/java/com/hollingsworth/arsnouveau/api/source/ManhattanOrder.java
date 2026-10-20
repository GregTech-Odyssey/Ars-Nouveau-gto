package com.hollingsworth.arsnouveau.api.source;

import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 把带坐标的对象排成原版 {@code BlockPos.withinManhattan} 的遍历顺序，替代逐格扫描时保持结果顺序不变。
 */
public final class ManhattanOrder {

    private ManhattanOrder() {}

    public static <T> void sort(List<T> list, BlockPos center, Function<? super T, BlockPos> pos) {
        int n = list.size();
        if (n < 2) return;
        if (n < (1 << 20) && withinPackedRange(list, center, pos)) {
            sortPacked(list, center, pos);
        } else {
            list.sort(comparator(center, pos));
        }
    }

    private static <T> boolean withinPackedRange(List<T> list, BlockPos center, Function<? super T, BlockPos> pos) {
        int cx = center.getX(), cy = center.getY(), cz = center.getZ();
        for (T t : list) {
            BlockPos p = pos.apply(t);
            if (Math.abs((long) p.getX() - cx) > 63 || Math.abs((long) p.getY() - cy) > 63 || Math.abs((long) p.getZ() - cz) > 63) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> void sortPacked(List<T> list, BlockPos center, Function<? super T, BlockPos> pos) {
        int n = list.size();
        int cx = center.getX(), cy = center.getY(), cz = center.getZ();
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) {
            BlockPos p = pos.apply(list.get(i));
            int dx = p.getX() - cx, dy = p.getY() - cy, dz = p.getZ() - cz;
            long d = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            keys[i] = d << 41 | (long) (dx + 64) << 34 | (long) (dy + 64) << 27 | (long) (64 - dz) << 20 | i;
        }
        Arrays.sort(keys);
        Object[] items = list.toArray();
        for (int i = 0; i < n; i++) {
            list.set(i, (T) items[(int) (keys[i] & 0xFFFFF)]);
        }
    }

    public static <T> Comparator<T> comparator(BlockPos center, Function<? super T, BlockPos> pos) {
        long cx = center.getX(), cy = center.getY(), cz = center.getZ();
        return (a, b) -> {
            BlockPos pa = pos.apply(a), pb = pos.apply(b);
            long ax = pa.getX() - cx, ay = pa.getY() - cy, az = pa.getZ() - cz;
            long bx = pb.getX() - cx, by = pb.getY() - cy, bz = pb.getZ() - cz;
            int c = Long.compare(Math.abs(ax) + Math.abs(ay) + Math.abs(az), Math.abs(bx) + Math.abs(by) + Math.abs(bz));
            if (c != 0) return c;
            c = Long.compare(ax, bx);
            if (c != 0) return c;
            c = Long.compare(ay, by);
            if (c != 0) return c;
            return Long.compare(bz, az);
        };
    }
}
