/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Date;

/**
 * @author alex
 * @version $Revision$
 */
public class DayOfWeekAssertion extends DateTimeAssertion {
    protected AssertionStatus doCheckDate(Date now) {
        return null;
    }
}
