package com.l7tech.util;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 12/16/11
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class DateUtilsTest {

    private Calendar calendar;

    @Before
    public void setUp() throws Exception {
       calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("PST"));
    }


    @Test
    public void testParseDateTime() throws Exception {
        calendar.set(2011, 11, 16, 10, 26, 45);
        calendar.set(Calendar.MILLISECOND, 78);
        Date expected = calendar.getTime();
        String timeString = "2011-12-16T10:26:45.078-08:00";
        Date actual = DateUtils.parseDateTime(timeString, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertEquals(expected, actual);

        timeString = "2011-12-16T10:26:45.078-0800";
        actual = DateUtils.parseDateTime(timeString, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertEquals(expected,actual);

        timeString = "12/16/2011 10:26:45.078 PST";
        actual = DateUtils.parseDateTime(timeString, "MM/dd/yyyy HH:mm:ss.SSS z");
        assertEquals(expected, actual);

        timeString = "Fri, Dec 16, 2011 10:26:45.078 AM -08:00";
        actual = DateUtils.parseDateTime(timeString, "EEE, MMM d, yyyy hh:mm:ss.SSS aaa Z");
        assertEquals(expected, actual);
    }


    @Test
    public void testCompareDate() throws Exception {

        calendar.set(2011, 11, 16, 10, 26, 45);
        calendar.set(Calendar.MILLISECOND, 78);
        Date d1 = calendar.getTime();
        calendar.set(2011, 10, 3, 10, 45, 57);
        Date d2 = calendar.getTime();
        assertTrue(DateUtils.compareDate(null, null) == 0);

        assertTrue(DateUtils.compareDate(d1, null) > 0);

        assertTrue(DateUtils.compareDate(null, d2) < 0);

        assertTrue(DateUtils.compareDate(d1,d2) > 0);

        assertTrue(DateUtils.compareDate(d2, d1) < 0);

        Date d3 = new Date(d1.getTime());

        assertTrue(DateUtils.compareDate(d1, d3) == 0);
    }
}
