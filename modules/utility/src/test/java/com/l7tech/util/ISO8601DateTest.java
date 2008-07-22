package com.l7tech.util;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Date;
import java.util.logging.Logger;
import java.text.ParseException;

import com.l7tech.util.ISO8601Date;

/**
 * Test various behavior of ISO8601Date
 */
public class ISO8601DateTest extends TestCase {
    protected static final Logger logger = Logger.getLogger(ISO8601DateTest.class.getName());
    private static final long DATE_A_LONG = 1164940290765L;
    private static final String DATE_A_STRING = "2006-12-01T02:31:30.765Z";
    private static final String DATE_A_STRING_NOZ = "2006-12-01T02:31:30.765";
    private static final String DATE_A_STRING_NANOS = "2006-12-01T02:31:30.765123456Z";

    public ISO8601DateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ISO8601DateTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFormatDate() {
        assertEquals(DATE_A_STRING, ISO8601Date.format(new Date(DATE_A_LONG)));
    }

    public void testFormatDateNanos() {
        assertEquals(DATE_A_STRING_NANOS, ISO8601Date.format(new Date(DATE_A_LONG), 123456L));
    }

    public void testParseDate() throws ParseException {
        assertEquals(DATE_A_LONG, ISO8601Date.parse(DATE_A_STRING).getTime());
    }

    public void testParseDateNanos() throws ParseException {
        assertEquals(DATE_A_LONG, ISO8601Date.parse(DATE_A_STRING_NANOS).getTime());
    }

    public void testParseDateExtraDigits() throws ParseException {
        assertEquals(DATE_A_LONG, ISO8601Date.parse(DATE_A_STRING_NOZ + "72387293873723287173872345924Z").getTime());
    }

    public void testParseDateWithWhitespaceBefore() {
        try {
            ISO8601Date.parse("   " + DATE_A_STRING).getTime();
            fail("Whitespace before the date was allowed");
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testParseDateWithWhitespaceBeforeTz() {
        try {
            ISO8601Date.parse(DATE_A_STRING_NOZ + "723872938737232   Z");
            fail("Whitespace was allowed before the time zone");
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testParseDateWithWhitespaceAfter() {
        try {
            ISO8601Date.parse(DATE_A_STRING + "    ").getTime();
            fail("Whitespace after the date was allowed");
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testParseDateWithGarbageBefore() {
        try {
            ISO8601Date.parse("asdfasdf" + DATE_A_STRING).getTime();
            fail("Whitespace before the date was allowed");
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testParseDateWithGarbageBeforeTz() {
        try {
            ISO8601Date.parse(DATE_A_STRING_NOZ + "723872938737232871s73gg23 45924Z");
            fail("Extra garbage was allowed before the time zone");
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testParseDateWithGarbageAfter() {
        try {
            ISO8601Date.parse(DATE_A_STRING_NOZ + "7238729387372328723424Zasdfga");
            fail("Extra garbage was allowed after the time zone");
        } catch (ParseException e) {
            // Ok
        }
    }
}

