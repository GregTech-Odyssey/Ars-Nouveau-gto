package com.hollingsworth.arsnouveau.common.block.tile;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.LargeSourceJar;
import com.hollingsworth.arsnouveau.common.block.SourceJar;
import com.hollingsworth.arsnouveau.common.network.BlockEntitySync;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * One block of a 3x3x3 large source jar. Every part exposes the shared storage of its column; only the center block (core) stores data and ticks.
 * Units stacked with aligned cores form one column whose storage lives in {@link Column} while loaded and is split back into the cores bottom-up (lower units fill first) for saving and breaking.
 */
public class LargeSourceJarTile extends SourceJarTile {
    public static final int SEGMENT_PIXELS = 48;
    private static final String TAG_STORED = "source";
    private static final String TAG_SHOWN = "shown";
    private static final String TAG_CAPACITY = "capacity";
    private static final String TAG_INDEX = "index";
    private static final String TAG_SEGMENT = "segment";
    private static final int FLUSH_INTERVAL = 10;

    private final boolean core;
    @Nullable
    private LargeSourceJarTile coreCache;
    long stored;
    @Nullable
    Column column;
    int columnIndex;
    private long shownSource;
    private long shownCapacity;
    private int shownIndex;
    private int shownSegment;

    public LargeSourceJarTile(BlockEntityType<? extends LargeSourceJarTile> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.core = LargeSourceJar.isCore(state);
    }

    public LargeSourceJarTile(BlockPos pos, BlockState state) {
        this(BlockRegistry.LARGE_SOURCE_JAR_TILE.get(), pos, state);
    }

    public boolean isCore() {
        return core;
    }

    public BlockPos getCorePos() {
        return LargeSourceJar.corePos(getBlockState(), worldPosition);
    }

    @Nullable
    public LargeSourceJarTile getCore() {
        if (core) {
            return this;
        }
        LargeSourceJarTile cached = coreCache;
        if (cached != null && !cached.isRemoved() && cached.level == level) {
            return cached;
        }
        cached = level == null ? null : coreAt(level, getCorePos(), getBlockState().getBlock());
        coreCache = cached;
        return cached;
    }

    @Nullable
    static LargeSourceJarTile coreAt(Level level, BlockPos pos, Block block) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof LargeSourceJarTile tile) || tile.isRemoved()) {
            return null;
        }
        return tile.isCore() && tile.getBlockState().is(block) ? tile : null;
    }

    @Nullable
    static LargeSourceJarTile stackedCore(Level level, LargeSourceJarTile core, boolean up) {
        BlockPos center = core.worldPosition;
        BlockPos lower = up ? center.above() : center.below(2);
        if (!level.isLoaded(lower) || !level.isLoaded(lower.above())
                || !LargeSourceJar.stacks(level.getBlockState(lower), level.getBlockState(lower.above()))) {
            return null;
        }
        return coreAt(level, up ? center.above(3) : center.below(3), core.getBlockState().getBlock());
    }

    @Override
    public boolean isStorageAnchor() {
        return core;
    }

    @Override
    public Object sharedStorage() {
        Column current = column();
        return current != null ? current : this;
    }

    @Nullable
    private Column column() {
        if (level == null || level.isClientSide) {
            return null;
        }
        LargeSourceJarTile core = getCore();
        if (core == null) {
            return null;
        }
        Column current = core.column;
        if (current == null || current.dissolved) {
            current = Column.form(core);
        }
        return current;
    }

    @Nullable
    private LargeSourceJarTile clientLeader() {
        LargeSourceJarTile core = getCore();
        if (core == null || level == null) {
            return null;
        }
        if (core.shownIndex == 0) {
            return core;
        }
        return coreAt(level, core.worldPosition.below(3 * core.shownIndex), core.getBlockState().getBlock());
    }

    public void onUnitPlaced(long initialSource) {
        stored = Math.max(0, Math.min(initialSource, jarSpec().capacity()));
        Block block = getBlockState().getBlock();
        for (BlockPos neighbor : new BlockPos[]{worldPosition.below(3), worldPosition.above(3)}) {
            LargeSourceJarTile other = coreAt(level, neighbor, block);
            if (other != null && other.column != null) {
                other.column.dissolve();
            }
        }
        setChanged();
    }

    public long currentShare() {
        return column != null && !column.dissolved ? column.shareOf(this) : stored;
    }

    public void dissolveColumn() {
        if (column != null) {
            column.dissolve();
        }
    }

    @Override
    public void setRemoved() {
        if (column != null && level != null && !level.isClientSide) {
            column.dissolve();
        }
        super.setRemoved();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeSourceJarTile tile) {
        Column current = tile.column();
        if (current != null) {
            current.tick(level.getGameTime());
        }
    }

    @Override
    public long getSourceLong() {
        if (level != null && level.isClientSide) {
            LargeSourceJarTile leader = clientLeader();
            return leader == null ? 0 : leader.shownSource;
        }
        Column current = column();
        return current == null ? 0 : current.total;
    }

    @Override
    public long getMaxSourceLong() {
        if (level != null && level.isClientSide) {
            LargeSourceJarTile leader = clientLeader();
            return leader == null ? 0 : leader.shownCapacity;
        }
        Column current = column();
        return current == null ? 0 : current.capacity;
    }

    @Override
    public long getTransferRateLong() {
        return jarSpec().transferRate();
    }

    @Override
    public long setSourceLong(long source) {
        Column current = column();
        if (current == null) {
            return 0;
        }
        current.set(source);
        return current.total;
    }

    @Override
    public long addSourceLong(long source) {
        Column current = column();
        if (current == null) {
            return 0;
        }
        current.set(saturatedAdd(current.total, source));
        return current.total;
    }

    @Override
    public long removeSourceLong(long source) {
        Column current = column();
        if (current == null) {
            return 0;
        }
        current.set(saturatedAdd(current.total, -source));
        return current.total;
    }

    @Override
    public void setMaxSourceLong(long max) {
    }

    @Override
    public int getSource() {
        return ISourceTile.clampToInt(getSourceLong());
    }

    @Override
    public int getMaxSource() {
        return ISourceTile.clampToInt(getMaxSourceLong());
    }

    @Override
    public int getTransferRate() {
        return ISourceTile.clampToInt(getTransferRateLong());
    }

    @Override
    public int setSource(int source) {
        return ISourceTile.clampToInt(setSourceLong(source));
    }

    @Override
    public int addSource(int source) {
        return ISourceTile.clampToInt(addSourceLong(source));
    }

    @Override
    public int removeSource(int source) {
        return ISourceTile.clampToInt(removeSourceLong(source));
    }

    @Override
    public void setMaxSource(int max) {
    }

    @Override
    public boolean canAcceptSource() {
        return getSourceLong() < getMaxSourceLong();
    }

    @Override
    public boolean updateBlock() {
        if (level != null && !level.isClientSide && isCore()) {
            BlockEntitySync.request(this);
        }
        return true;
    }

    public int getComparatorSignal() {
        return SourceJar.comparatorSignal(getSourceLong(), getMaxSourceLong());
    }

    public int getShownSegment() {
        return shownSegment;
    }

    public int getShownIndex() {
        return shownIndex;
    }

    @Override
    public void getTooltip(List<Component> tooltip) {
        long max = getMaxSourceLong();
        tooltip.add(Component.translatable("ars_nouveau.source_jar.fullness", max <= 0 ? 0 : getSourceLong() * 100 / max));
        tooltip.add(Component.translatable("ars_nouveau.large_source_jar.amount", getSourceLong(), max));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = isCore() ? Math.max(0, Math.min(tag.getLong(TAG_STORED), jarSpec().capacity())) : 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.remove(MAX_SOURCE_TAG);
        tag.remove(COLOR_TAG);
        if (isCore()) {
            tag.putLong(TAG_STORED, currentShare());
        } else {
            tag.remove(TAG_STORED);
        }
    }

    @Override
    protected CompoundTag writeSyncTag() {
        CompoundTag tag = new CompoundTag();
        if (isCore()) {
            tag.putInt(TAG_INDEX, shownIndex);
            tag.putInt(TAG_SEGMENT, shownSegment);
            if (shownIndex == 0) {
                tag.putLong(TAG_SHOWN, shownSource);
                tag.putLong(TAG_CAPACITY, shownCapacity);
            }
        }
        return tag;
    }

    @Override
    protected void readSyncTag(CompoundTag tag) {
        shownIndex = tag.getInt(TAG_INDEX);
        shownSegment = tag.getInt(TAG_SEGMENT);
        shownSource = tag.getLong(TAG_SHOWN);
        shownCapacity = tag.getLong(TAG_CAPACITY);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, -1, -1), worldPosition.offset(2, 2, 2));
    }

    private void publish(long total, long capacity, int index, int units) {
        long span = units * (long) SEGMENT_PIXELS - LargeSourceJar.BASE_PIXELS - LargeSourceJar.TOP_PIXELS;
        long surface = total <= 0 || capacity <= 0 ? 0 : Math.max(LargeSourceJar.BASE_PIXELS + 1, LargeSourceJar.BASE_PIXELS + total * span / capacity);
        int segment = (int) Math.max(0, Math.min(SEGMENT_PIXELS, surface - (long) index * SEGMENT_PIXELS));
        boolean changed = shownIndex != index || shownSegment != segment;
        if (index == 0 && (shownSource != total || shownCapacity != capacity)) {
            changed = true;
        }
        shownIndex = index;
        shownSegment = segment;
        shownSource = index == 0 ? total : 0;
        shownCapacity = index == 0 ? capacity : 0;
        if (changed) {
            BlockEntitySync.request(this);
        }
    }

    private static long saturatedAdd(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    static final class Column {
        private final Level level;
        private final List<LargeSourceJarTile> cores;
        private final long capacity;
        private long total;
        private boolean dissolved;
        private boolean dirty = true;
        private long lastTick = Long.MIN_VALUE;
        private int comparator = -1;

        private Column(Level level, List<LargeSourceJarTile> cores, long capacity) {
            this.level = level;
            this.cores = cores;
            this.capacity = capacity;
        }

        static Column form(LargeSourceJarTile start) {
            Level level = start.level;
            LargeSourceJarTile bottom = start;
            for (LargeSourceJarTile below = stackedCore(level, bottom, false); below != null; below = stackedCore(level, bottom, false)) {
                bottom = below;
            }
            List<LargeSourceJarTile> cores = new ArrayList<>();
            for (LargeSourceJarTile current = bottom; current != null; current = stackedCore(level, current, true)) {
                cores.add(current);
            }
            for (LargeSourceJarTile core : cores) {
                if (core.column != null) {
                    core.column.dissolve();
                }
            }
            long capacity = 0;
            long sum = 0;
            for (LargeSourceJarTile core : cores) {
                capacity += core.jarSpec().capacity();
                sum += core.stored;
            }
            Column result = new Column(level, cores, capacity);
            result.total = Math.min(sum, capacity);
            for (int i = 0; i < cores.size(); i++) {
                LargeSourceJarTile core = cores.get(i);
                core.column = result;
                core.columnIndex = i;
            }
            return result;
        }

        void dissolve() {
            if (dissolved) {
                return;
            }
            dissolved = true;
            for (int i = 0; i < cores.size(); i++) {
                LargeSourceJarTile core = cores.get(i);
                core.stored = shareOf(i);
                if (core.column == this) {
                    core.column = null;
                }
            }
            markChunk();
        }

        private long shareOf(int index) {
            long below = 0;
            for (int i = 0; i < index; i++) {
                below += cores.get(i).jarSpec().capacity();
            }
            return Math.max(0, Math.min(cores.get(index).jarSpec().capacity(), total - below));
        }

        long shareOf(LargeSourceJarTile core) {
            int index = core.column == this ? core.columnIndex : cores.indexOf(core);
            return index < 0 ? core.stored : shareOf(index);
        }

        void set(long value) {
            long next = Math.max(0, Math.min(capacity, value));
            if (next != total) {
                total = next;
                dirty = true;
                markChunk();
            }
        }

        private void markChunk() {
            level.blockEntityChanged(cores.get(0).worldPosition);
        }

        void tick(long gameTime) {
            if (gameTime == lastTick) {
                return;
            }
            lastTick = gameTime;
            if (!dirty || gameTime % FLUSH_INTERVAL != 0) {
                return;
            }
            dirty = false;
            int units = cores.size();
            for (int i = 0; i < units; i++) {
                cores.get(i).publish(total, capacity, i, units);
            }
            int signal = SourceJar.comparatorSignal(total, capacity);
            if (signal != comparator) {
                comparator = signal;
                Block block = cores.get(0).getBlockState().getBlock();
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (LargeSourceJarTile core : cores) {
                    BlockPos center = core.worldPosition;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dz == 0) {
                                    continue;
                                }
                                pos.setWithOffset(center, dx, dy, dz);
                                if (level.isLoaded(pos)) {
                                    level.updateNeighbourForOutputSignal(pos, block);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
