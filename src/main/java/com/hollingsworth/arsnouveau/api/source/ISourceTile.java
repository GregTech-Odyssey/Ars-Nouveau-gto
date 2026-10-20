package com.hollingsworth.arsnouveau.api.source;

/**
 * Interface for a generic tile that holds source.
 * The long methods are the primary API; the int methods remain for addons compiled against the old signatures.
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

    static int clampToInt(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : value <= Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value;
    }
}
