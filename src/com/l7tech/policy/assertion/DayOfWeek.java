/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * @author alex
 * @version $Revision$
 */
public class DayOfWeek {
    public static final DayOfWeek SUNDAY    = new DayOfWeek( 0, "Sunday" );
    public static final DayOfWeek MONDAY    = new DayOfWeek( 1, "Monday" );
    public static final DayOfWeek TUESDAY   = new DayOfWeek( 2, "Tuesday" );
    public static final DayOfWeek WEDNESDAY = new DayOfWeek( 3, "Wednesday" );
    public static final DayOfWeek THURSDAY  = new DayOfWeek( 4, "Thursday" );
    public static final DayOfWeek FRIDAY    = new DayOfWeek( 5, "Friday" );
    public static final DayOfWeek SATURDAY  = new DayOfWeek( 6, "Saturday" );

    protected DayOfWeek( int num, String name ) {
        _num = num;
        _name = name;
    }

    protected final int _num;
    protected final String _name;
}
