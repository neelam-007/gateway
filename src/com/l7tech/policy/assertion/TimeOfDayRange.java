/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Date;
import java.util.Calendar;

/**
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class TimeOfDayRange {
    public TimeOfDayRange( TimeOfDay from, TimeOfDay to ) {
        _from = from;
        _to = to;
    }

    public TimeOfDay getFrom() {
        return _from;
    }

    public TimeOfDay getTo() {
        return _to;
    }

    protected final TimeOfDay _from;
    protected final TimeOfDay _to;
}
