package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

/**
 * Reserved GOID ranges.
 */
public enum GoidRange {
    DEFAULT(0, -1, 0, -1),
    WRAPPED_OID(3, Long.MIN_VALUE, 3, Long.MAX_VALUE),
    ;

    /**
     * Check if the specified GOID falls within this GOID range.
     *
     * @param goid the goid to check.  Required.
     * @return true if the specified goid is within this GOID range.
     */
    public boolean isInRange( @NotNull Goid goid ) {
        return goid.getHi() >= hiStart && goid.getHi() <= hiEnd && goid.getLow() >= loStart && goid.getLow() <= loEnd;
    }

    /**
     * @return the first high value that is within this GOID range.
     */
    public long getFirstHi() {
        return hiStart;
    }

    // PRIVATE

    private final long hiStart;
    private final long loStart;
    private final long hiEnd;
    private final long loEnd;

    private GoidRange(long hiStart, long loStart, long hiEnd, long loEnd) {
        this.hiStart = hiStart;
        this.loStart = loStart;
        this.hiEnd = hiEnd;
        this.loEnd = loEnd;
    }
}