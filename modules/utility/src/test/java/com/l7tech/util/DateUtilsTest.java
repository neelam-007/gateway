package com.l7tech.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author darmstrong
 */
public class DateUtilsTest {
    @Test
    public void testGeTimeZone() throws Exception {

        assertNotNull(DateUtils.getTimeZone("utc"));
        assertNotNull(DateUtils.getTimeZone("local"));
        assertNotNull(DateUtils.getTimeZone("Pacific/Midway"));
        assertNotNull(DateUtils.getTimeZone("+00"));
        assertNotNull(DateUtils.getTimeZone("-00"));
        assertNotNull(DateUtils.getTimeZone("+1000"));
        assertNotNull(DateUtils.getTimeZone("-1000"));
        // 5 hours nad 30 minutes is a valid timezone offset from GMT.
        assertNotNull(DateUtils.getTimeZone("+05:30"));
        assertNotNull(DateUtils.getTimeZone("+0530"));

        assertNotNull(DateUtils.getTimeZone("-02:30"));
        assertNotNull(DateUtils.getTimeZone("-0230"));

        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})");
    }

    @Test
    public void testGetZuluFormattedString() throws Exception {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));
            final Date date = new Date();
            final String actual = DateUtils.getZuluFormattedString(date);

            final SimpleDateFormat format = new SimpleDateFormat(DateUtils.ISO8601_PATTERN);
            format.setTimeZone(DateUtils.getZuluTimeZone());
            final String expected = format.format(date);

            assertEquals(expected, actual);
        } finally {
            TimeZone.setDefault(null);
        }
    }

    @Test
    public void testFormattedString() throws Exception {
        final Date date = new Date();
        final TimeZone pstTimeZone = TimeZone.getTimeZone("PST");
        final String actual = DateUtils.getFormattedString(date, pstTimeZone, DateUtils.RFC1123_DEFAULT_PATTERN);

        final SimpleDateFormat format = new SimpleDateFormat(DateUtils.RFC1123_DEFAULT_PATTERN);
        format.setTimeZone(pstTimeZone);
        final String expected = format.format(date);

        assertEquals(expected, actual);
    }

    @Test
    public void testDateFormattingNoTimezoneInPattern() throws Exception {

        final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
        format.setTimeZone(DateUtils.getZuluTimeZone());
        // this date only parses as lenient is true
        final Date date = format.parse("Mon May 07 14:12:24 2012");
        final String expected = format.format(date);
        final String actual =
                DateUtils.getFormattedString(date, DateUtils.getZuluTimeZone(), "EEE MMM dd HH:mm:ss yyyy");
        assertEquals(expected, actual);
    }
}
