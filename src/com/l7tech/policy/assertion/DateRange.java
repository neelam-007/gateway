/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Date;

/**
 * A contiguous range of concrete calendar days.  Inclusive of bounds.  Times are significant too, so if you want whole days, make sure you begin at 0:00:00 and end at 23:59:59!
 *
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class DateRange {
    public DateRange( Date from, Date to ) {
        if ( from.compareTo(to) == 0 || from.after(to)  ) throw new IllegalArgumentException( "Invalid date range! To must be after from!" );
        _from = from;
        _to = to;
    }

    public Date getFrom() {
        return _from;
    }

    public Date getTo() {
        return _to;
    }

    /**
     * Always inclusive!
     *
     * @param candidate A Date to test for inclusion
     * @return true if the provided Date falls within the range.
     */
    public boolean includes( Date candidate ) {
        return candidate.compareTo( _from ) <= 0 && candidate.compareTo( _to ) >= 0;
    }

    protected final Date _from;
    protected final Date _to;
}
