/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.io.Serializable;


/**
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class TimeOfDayRange implements Serializable {
    // default constructor for bean mapping purposes
    public TimeOfDayRange() {
    }
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

    public void setFrom(TimeOfDay arg) {
        _from = arg;
    }
    public void setTo(TimeOfDay arg) {
        _to = arg;
    }

    protected TimeOfDay _from;
    protected TimeOfDay _to;
}
