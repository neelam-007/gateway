/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TimeOfDay;
import com.l7tech.policy.assertion.TimeOfDayAssertion;
import com.l7tech.policy.assertion.TimeOfDayRange;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerTimeOfDayAssertion extends ServerDateTimeAssertion implements ServerAssertion {
    public ServerTimeOfDayAssertion( TimeOfDayAssertion data ) {
        _data = data;
    }

    protected AssertionStatus doCheckDate( Date dateTime ) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( dateTime );
        TimeOfDay now = new TimeOfDay( cal.get( Calendar.HOUR), cal.get( Calendar.MINUTE ), cal.get( Calendar.SECOND ) );

        Iterator i = _data.getRanges().iterator();
        TimeOfDayRange range;
        while ( i.hasNext() ) {
            range = (TimeOfDayRange)i.next();
            TimeOfDay from = range.getFrom();
            TimeOfDay to = range.getTo();

            if ( now.after( from ) && now.before( to ) )
                return AssertionStatus.NONE;
            else if ( now.equals( from ) || now.equals( to ) )
                return AssertionStatus.NONE;
        }

        return AssertionStatus.FALSIFIED;
    }

    protected TimeOfDayAssertion _data;
}
