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
 *
 * @author alex
 * @version $Revision$
 */
public class TimeOfDay implements Comparable, Serializable {

    // default constructor for bean serialization
    public TimeOfDay() {
    }

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

    public int getHour() {return _hour;}
    public int getMinute() {return _minute;}
    public int getSecond() {return _second;}

    public void setHour(int arg) {
        _hour = arg;
        _secondsSinceMidnight = ( _hour * 60 * 60 ) + ( _minute * 60 ) + _second;
    }
    public void setMinute(int arg) {
        _minute = arg;
        _secondsSinceMidnight = ( _hour * 60 * 60 ) + ( _minute * 60 ) + _second;
    }

    public void setSecond(int arg) {
        _second = arg;
        _secondsSinceMidnight = ( _hour * 60 * 60 ) + ( _minute * 60 ) + _second;
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

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        TimeOfDay other = (TimeOfDay)o;
        if (other._secondsSinceMidnight == _secondsSinceMidnight)
            return true;
        else
            return false;
    }

    public int hashCode() {
        int result;
        result = _hour;
        result = 29 * result + _minute;
        result = 29 * result + _second;
        result = 29 * result + _secondsSinceMidnight;
        return result;
    }

    protected int _hour;
    protected int _minute;
    protected int _second;

    protected transient int _secondsSinceMidnight;
}
