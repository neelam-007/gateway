/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class TimeOfDay implements Comparable {
    /**
     * Construct a new TimeOfDay from a given hour, minute and second.  Assumed to be in 24-hour format!
     * @param hour
     * @param minute
     * @param second
     */
    public TimeOfDay( int hour, int minute, int second ) {
        _hour = hour;
        _minute = minute;
        _second = second;
        _secondsSinceMidnight = ( hour * 60 * 60 ) + ( minute * 60 ) + second;
    }

    public boolean before( TimeOfDay other ) {
        return _secondsSinceMidnight < other._secondsSinceMidnight;
    }

    public boolean after( TimeOfDay other ) {
        return _secondsSinceMidnight > other._secondsSinceMidnight;
    }

    public int compareTo(Object o) {
        TimeOfDay other = (TimeOfDay)o;
        if ( other._secondsSinceMidnight == _secondsSinceMidnight )
            return 0;
        else if ( other._secondsSinceMidnight < _secondsSinceMidnight )
            return 1;
        else
            return -1;
    }

    public boolean equals( Object o ) {
        TimeOfDay other = (TimeOfDay)o;
        if ( other._secondsSinceMidnight == _secondsSinceMidnight )
            return true;
        else
            return false;
    }

    protected final int _hour;
    protected final int _minute;
    protected final int _second;

    protected final int _secondsSinceMidnight;
}
