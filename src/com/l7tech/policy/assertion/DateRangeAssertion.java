/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Date;
import java.util.Iterator;
import java.util.Calendar;

/**
 * Asserts that the specified date falls within a defined set of DateRanges, inclusive of bounds.
 *
 * Note: the times count too, so if you want to allow access for an entire day, use a DateRange that begins at 0:00 and ends at 23:59.
 *
 * @author alex
 * @version $Revision$
 */
public class DateRangeAssertion extends DateTimeAssertion {

}
