package com.hollingsworth.arsnouveau.common.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * Per-variant parameters of one source jar unit; stacked large jars sum unit capacities into one column.
 * The transfer rate defaults to the unit capacity, so a jar can be emptied in one transfer but merging units never multiplies it.
 */
public final class SourceJarSpec {
    private static int maxReach;

    public static final SourceJarSpec JAR = builder().capacity(10_000L).build();
    public static final SourceJarSpec LARGE = builder().size(3, 3, 3).capacity(JAR.capacity() * 27 * 4).build();

    private final int width;
    private final int height;
    private final int depth;
    private final long capacity;
    private final long transferRate;

    private SourceJarSpec(int width, int height, int depth, long capacity, long transferRate) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.capacity = capacity;
        this.transferRate = transferRate;
        maxReach = Math.max(maxReach, reach());
    }

    public static int maxReach() {
        return maxReach;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().size(width, height, depth).capacity(capacity).transferRate(transferRate);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public long capacity() {
        return capacity;
    }

    public long transferRate() {
        return transferRate > 0 ? transferRate : capacity;
    }

    public BlockPos nearestPart(BlockPos core, BlockPos to) {
        if (width == 1 && height == 1 && depth == 1) {
            return core;
        }
        int rx = width / 2, ry = height / 2, rz = depth / 2;
        return new BlockPos(clamp(to.getX(), core.getX() - rx, core.getX() + rx), clamp(to.getY(), core.getY() - ry, core.getY() + ry),
                clamp(to.getZ(), core.getZ() - rz, core.getZ() + rz));
    }

    public int reach() {
        return Math.max(width, Math.max(height, depth)) / 2;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static SourceJarSpec of(Block block, SourceJarSpec fallback) {
        return block instanceof Holder holder ? holder.jarSpec() : fallback;
    }

    public interface Holder {
        SourceJarSpec jarSpec();
    }

    public static final class Builder {
        private int width = 1;
        private int height = 1;
        private int depth = 1;
        private long capacity;
        private long transferRate;

        private Builder() {}

        public Builder size(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
            return this;
        }

        public Builder capacity(long capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder transferRate(long transferRate) {
            this.transferRate = transferRate;
            return this;
        }

        public SourceJarSpec build() {
            if (width <= 0 || height <= 0 || depth <= 0 || capacity <= 0 || transferRate < 0) {
                throw new IllegalArgumentException("invalid source jar spec");
            }
            return new SourceJarSpec(width, height, depth, capacity, transferRate);
        }
    }
}
