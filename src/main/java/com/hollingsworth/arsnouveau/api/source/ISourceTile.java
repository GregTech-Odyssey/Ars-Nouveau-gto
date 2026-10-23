package com.hollingsworth.arsnouveau.api.source;

/**
 * Interface for a generic tile that holds source. Moves between holders go through {@link #transfer}, which takes first
 * and only gives what was taken; addSource/removeSource(long, boolean) return the amount that actually changed.
 */
public interface ISourceTile {

    int getTransferRate();

    boolean canAcceptSource();

    int getSource();

    int getMaxSource();

    void setMaxSource(int max);

    int setSource(final int source);

    int addSource(final int source);

    int removeSource(final int source);

    default long getTransferRateLong() {
        return getTransferRate();
    }

    default long getSourceLong() {
        return getSource();
    }

    default long getMaxSourceLong() {
        return getMaxSource();
    }

    default void setMaxSourceLong(long max) {
        setMaxSource(clampToInt(max));
    }

    default long setSourceLong(long source) {
        return setSource(clampToInt(source));
    }

    default long addSourceLong(long source) {
        return addSource(clampToInt(source));
    }

    default long removeSourceLong(long source) {
        return removeSource(clampToInt(source));
    }

    default long addSource(long amount, boolean simulate) {
        long change = Math.min(amount, getMaxSourceLong() - getSourceLong());
        if (change <= 0) {
            return 0;
        }
        if (!simulate) {
            addSourceLong(change);
        }
        return change;
    }

    default long removeSource(long amount, boolean simulate) {
        long change = Math.min(amount, getSourceLong());
        if (change <= 0) {
            return 0;
        }
        if (!simulate) {
            removeSourceLong(change);
        }
        return change;
    }

    static long transfer(ISourceTile from, ISourceTile to, long amount) {
        if (amount <= 0) {
            return 0;
        }
        long room = to.addSource(amount, true);
        long taken = room > 0 ? from.removeSource(room, false) : 0;
        if (taken <= 0) {
            return 0;
        }
        long given = to.addSource(taken, false);
        if (given < taken) {
            from.addSource(taken - given, false);
        }
        return given;
    }

    static int clampToInt(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : value <= Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value;
    }
}
