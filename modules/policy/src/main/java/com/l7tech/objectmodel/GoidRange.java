package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

/**
 * Reserved GOID ranges.
 */
public enum GoidRange {
    /**
     * Represents a new, not-yet-saved entity.  Equivalent to GoidEntity.DEFAULT_GOID and
     * analagous to the old PersistentEntity.DEFAULT_OID.
     */
    DEFAULT(0, -1, 0, -1),

    /**
     * Represents a goid with a 0 prefix. These will include default and other hardcoded goids.
     * Note that this will include the default goid (0,-1)
     */
    ZEROED_PREFIX(0, Long.MIN_VALUE, 0, Long.MAX_VALUE),

    /**
     * Represents a temporary GOID value to use for a wrapped OID.
     * This is an OID that cannot be mapped to a (reserved or upgraded) GOID value.
     * Such wrapped OID GOIDs should be used only for transitional purposes
     * and must never be persisted as the GOID of some entity.
     */
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