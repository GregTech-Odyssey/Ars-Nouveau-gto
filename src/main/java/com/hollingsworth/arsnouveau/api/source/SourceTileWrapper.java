package com.hollingsworth.arsnouveau.api.source;

public record SourceTileWrapper(ISourceTile tile, boolean takeFrom, boolean giveTo) implements IAdvancedSourceTile {
    @Override
    public int getTransferRate() {
        return tile.getTransferRate();
    }

    @Override
    public boolean canAcceptSource() {
        return tile.canAcceptSource();
    }

    @Override
    public int getSource() {
        return tile.getSource();
    }

    @Override
    public int getMaxSource() {
        return tile.getMaxSource();
    }

    @Override
    public void setMaxSource(int max) {
        tile.setMaxSource(max);
    }

    @Override
    public int setSource(int source) {
        return tile.setSource(source);
    }

    @Override
    public int addSource(int source) {
        return tile.addSource(source);
    }

    @Override
    public int removeSource(int source) {
        return tile.removeSource(source);
    }

    @Override
    public long getTransferRateLong() {
        return tile.getTransferRateLong();
    }

    @Override
    public long getSourceLong() {
        return tile.getSourceLong();
    }

    @Override
    public long getMaxSourceLong() {
        return tile.getMaxSourceLong();
    }

    @Override
    public void setMaxSourceLong(long max) {
        tile.setMaxSourceLong(max);
    }

    @Override
    public long setSourceLong(long source) {
        return tile.setSourceLong(source);
    }

    @Override
    public long addSourceLong(long source) {
        return tile.addSourceLong(source);
    }

    @Override
    public long removeSourceLong(long source) {
        return tile.removeSourceLong(source);
    }

    @Override
    public boolean relayCanTakePower() {
        return takeFrom;
    }

    @Override
    public boolean sourcelinksCanProvidePower() {
        return giveTo;
    }
}
