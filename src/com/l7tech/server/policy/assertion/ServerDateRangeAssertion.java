/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.DateRangeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.DateRange;

import java.util.Date;
import java.util.Calendar;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerDateRangeAssertion extends ServerDateTimeAssertion implements ServerAssertion {
    public ServerDateRangeAssertion( DateRangeAssertion data ) {
        _data = data;
    }

    protected AssertionStatus doCheckDate(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( now );
        DateRange range;
        Date from, to;
        for (Iterator i = _data.getRanges().iterator(); i.hasNext();) {
            range = (DateRange)i.next();
            from = range.getFrom();
            to = range.getTo();

            if ( now.after( from ) && now.before( to ) )
                return AssertionStatus.NONE;
            else if ( now.equals( from ) || now.equals( to ) )
                return AssertionStatus.NONE;
        }

        return AssertionStatus.FALSIFIED;
    }

    protected DateRangeAssertion _data;
}
