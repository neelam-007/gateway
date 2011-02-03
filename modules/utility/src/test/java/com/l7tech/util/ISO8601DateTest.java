package com.l7tech.util;

import static org.junit.Assert.*;

import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.util.Date;
import java.util.logging.Logger;
import java.text.ParseException;

/**
 * Test various behavior of ISO8601Date
 */
public class ISO8601DateTest {
    protected static final Logger logger = Logger.getLogger(ISO8601DateTest.class.getName());
    private static final long DATE_A_LONG = 1164940290765L;
    private static final String DATE_A_STRING = "2006-12-01T02:31:30.765Z";
    private static final String DATE_A_STRING_NOZ = "2006-12-01T02:31:30.765";
    private static final String DATE_A_STRING_NANOS = "2006-12-01T02:31:30.765123456Z";
    private static final String DATE_A_STRING_TZ = "2006-12-01T02:31:30.765123456+00:00";

    @Test
    public void testFormatDate() {
        assertEquals(DATE_A_STRING, ISO8601Date.format(new Date(DATE_A_LONG)));
    }

    @Test
    public void testFormatDateNanos() {
        assertEquals(DATE_A_STRING_NANOS, ISO8601Date.format(new Date(DATE_A_LONG), 123456L));
    }

    @Test
    public void testParseDate() throws ParseException {
        assertEquals(DATE_A_LONG, ISO8601Date.parse(DATE_A_STRING).getTime());
    }

    @Test
    public void testParseDateNanos() throws ParseException {
        assertEquals(DATE_A_LONG, ISO8601Date.parse(DATE_A_STRING_NANOS).getTime());
    }

    @Test
    public void testParseDateExtraDigits() throws ParseException {
        assertEquals(DATE_A_LONG, ISO8601Date.parse(DATE_A_STRING_NOZ + "72387293873723287173872345924Z").getTime());
    }

    @Test
    public void testParseDateWithWhitespaceBefore() {
        try {
            ISO8601Date.parse("   " + DATE_A_STRING).getTime();
            fail("Whitespace before the date was allowed");
        } catch (ParseException e) {
            // Ok
        }
    }

    @Test
    public void testParseDateWithWhitespaceBeforeTz() {
        try {
            ISO8601Date.parse(DATE_A_STRING_NOZ + "723872938737232   Z");
            fail("Whitespace was allowed before the time zone");
        } catch (ParseException e) {
            // Ok
        }
    }

    @Test
    public void testParseDateWithWhitespaceAfter() {
        try {
            ISO8601Date.parse(DATE_A_STRING + "    ").getTime();
            fail("Whitespace after the date was allowed");
        } catch (ParseException e) {
            // Ok
        }
    }

    @Test
    public void testParseDateWithGarbageBefore() {
        try {
            ISO8601Date.parse("asdfasdf" + DATE_A_STRING).getTime();
            fail("Whitespace before the date was allowed");
        } catch (ParseException e) {
            // Ok
        }
    }

    @Test
    public void testParseDateWithGarbageBeforeTz() {
        try {
            ISO8601Date.parse(DATE_A_STRING_NOZ + "723872938737232871s73gg23 45924Z");
            fail("Extra garbage was allowed before the time zone");
        } catch (ParseException e) {
            // Ok
        }
    }

    @Test
    public void testParseDateWithGarbageAfter() {
        try {
            ISO8601Date.parse(DATE_A_STRING_NOZ + "7238729387372328723424Zasdfga");
            fail("Extra garbage was allowed after the time zone");
        } catch (ParseException e) {
            // Ok
        }
    }

    @BugNumber(9789)
    @Test
    public void testParsePartialDate() {
        for ( int i=DATE_A_STRING_TZ.length(); i>=0; i-- ) {
            try {
                final String dateStr = DATE_A_STRING_TZ.substring( 0, i );
                logger.info( "Parsing date fragment: " + dateStr );
                ISO8601Date.parse( dateStr );
            } catch (ParseException e) {
                // OK
            }
        }
    }
}

