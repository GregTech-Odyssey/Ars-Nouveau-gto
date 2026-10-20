package com.hollingsworth.arsnouveau.api.source;

import com.hollingsworth.arsnouveau.api.util.IWololoable;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.tile.ModdedTile;
import com.hollingsworth.arsnouveau.common.util.RegistryWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractSourceMachine extends ModdedTile implements ISourceTile, IWololoable {

    private long source = 0;
    private long maxSource = 0;
    private ParticleColor color = ParticleColor.defaultParticleColor();
    public static String SOURCE_TAG = "source";
    public static String MAX_SOURCE_TAG = "max_source";
    public static String COLOR_TAG = "color";

    public AbstractSourceMachine(BlockEntityType<?> manaTile, BlockPos pos, BlockState state) {
        super(manaTile, pos, state);
    }

    public AbstractSourceMachine(RegistryWrapper< ? extends BlockEntityType<?>> manaTile, BlockPos pos, BlockState state) {
        this(manaTile.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        source = tag.getLong(SOURCE_TAG);
        maxSource = tag.getLong(MAX_SOURCE_TAG);
        color = ParticleColor.fromInt(tag.getInt(COLOR_TAG));
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(SOURCE_TAG, getSourceLong());
        tag.putLong(MAX_SOURCE_TAG, getMaxSourceLong());
        tag.putInt(COLOR_TAG, getColor().getColor());
    }

    @Override
    public int setSource(int source) {
        return ISourceTile.clampToInt(storeSource(source));
    }

    protected long storeSource(long source) {
        if (this.source == source)
            return this.source;
        long previous = this.source;
        this.source = source;
        long clamped = Math.max(0, Math.min(source, this.getMaxSourceLong()));
        this.source = clamped;
        if (clamped == previous)
            return clamped;
        updateBlock();
        return clamped;
    }

    protected long storedSource() {
        return this.source;
    }

    @Override
    public int addSource(int source) {
        return this.setSource(ISourceTile.clampToInt((long) this.getSource() + source));
    }

    @Override
    public int getSource() {
        return ISourceTile.clampToInt(this.source);
    }

    @Override
    public int removeSource(int source) {
        if (source == 0)
            return this.getSource();
        return this.setSource(ISourceTile.clampToInt((long) this.getSource() - source));
    }

    @Override
    public void setMaxSource(int max) {
        this.maxSource = max;
        updateBlock();
    }

    @Override
    public int getMaxSource() {
        return ISourceTile.clampToInt(maxSource);
    }

    public boolean canAcceptSource() {
        return this.getSourceLong() < this.getMaxSourceLong();
    }

    public boolean canAcceptSource(int source) {
        return canAcceptSource((long) source);
    }

    public boolean canAcceptSource(long source) {
        return this.getSourceLong() + source <= this.getMaxSourceLong();
    }

    /**
     * Transfers the maximum possible amount of source from one tile to another.
     * Takes the maximum transfer rate of the two tiles into account, and the space remaining.
     *
     * @return The amount of source that was transferred.
     */
    public int transferSource(ISourceTile from, ISourceTile to) {
        return ISourceTile.clampToInt(transferSourceLong(from, to));
    }

    public long transferSourceLong(ISourceTile from, ISourceTile to) {
        return transferSourceLong(from, to, from.getTransferRateLong());
    }

    /**
     * Gets the maximum amount of source that can be transferred from one tile to another.
     */
    public int getTransferRate(ISourceTile from, ISourceTile to) {
        return ISourceTile.clampToInt(getTransferRateLong(from, to));
    }

    public long getTransferRateLong(ISourceTile from, ISourceTile to) {
        return getTransferRateLong(from, to, from.getTransferRateLong());
    }

    public int transferSource(ISourceTile from, ISourceTile to, int fromTransferRate) {
        return ISourceTile.clampToInt(transferSourceLong(from, to, fromTransferRate));
    }

    public long transferSourceLong(ISourceTile from, ISourceTile to, long fromTransferRate) {
        long transferRate = getTransferRateLong(from, to, fromTransferRate);
        if (transferRate <= 0)
            return 0;
        from.removeSourceLong(transferRate);
        to.addSourceLong(transferRate);
        return transferRate;
    }

    public int getTransferRate(ISourceTile from, ISourceTile to, int fromTransferRate) {
        return ISourceTile.clampToInt(getTransferRateLong(from, to, fromTransferRate));
    }

    public long getTransferRateLong(ISourceTile from, ISourceTile to, long fromTransferRate) {
        return Math.max(0, Math.min(Math.min(fromTransferRate, from.getSourceLong()), to.getMaxSourceLong() - to.getSourceLong()));
    }

    @Override
    public ParticleColor getColor() {
        return color;
    }

    @Override
    public void setColor(ParticleColor color) {
        this.color = color;
    }
}

