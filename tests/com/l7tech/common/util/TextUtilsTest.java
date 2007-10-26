package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for TextUtils
 *
 * @author Steve Jones
 */
public class TextUtilsTest extends TestCase {

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TextUtilsTest.class);
    }

    public void testFullMatch() throws Exception {
        Object[][] pats = {
                {"www*", "www.google.com", true},
                {"a*c", "abc", true},
                {"a*", "abc", true},
                {"*", "abc", true},
                {"*c", "abc", true},
                {"*b*", "abc", true},
                {"ABC", "abc", true},
                {"bc", "abc", false},
                {"b*", "abc", false},
                {"*b", "abc", false},
                {"\\", "\\", true},
                {"*\\", "\\", true},
                {"\\*", "\\", true},
                {"[]()\\.", "[]()\\.", true},
        };

        for (Object[] pnt : pats) {
            String pattern = (String) pnt[0];
            String text = (String) pnt[1];
            boolean expectedResult = (Boolean) pnt[2];

            assertTrue("'"+ pattern + "' matches '" + text + "'.", expectedResult == TextUtils.matches(pattern, text, false, true));
        }
    }

    public void testStartsWithMatch() throws Exception {
        Object[][] pats = {
                {"www*", "www.google.com", true},
                {"a******c", "abc", true},
                {"a*", "abc", true},
                {"*", "abc", true},
                {"*c", "abc", true},
                {"*b*", "abc", true},
                {"ABC", "abc", true},
                {"bc", "abc", false},
                {"b*", "abc", false},
                {"*b", "abc", true},
                {"\\", "\\", true},
                {"*\\", "\\", true},
                {"\\*", "\\", true},
                {"abcd", "abcdefg", true},
                {"defg", "abcdefg", false},
        };

        for (Object[] pnt : pats) {
            String pattern = (String) pnt[0];
            String text = (String) pnt[1];
            boolean expectedResult = (Boolean) pnt[2];

            assertTrue("'"+ pattern + "' matches '" + text + "'.", expectedResult == TextUtils.matches(pattern, text, false, false));
        }
    }

    public void testCaseSensitiveMatch() throws Exception {
        Object[][] pats = {
                {"!@#!#$%#^$&^&*)(&\\*){}?:\">DSFGWERHG", "!@#!#$%#^$&^&*)(&\\*){}?:\">DSFGWERHG", true},
                {"www*", "www.google.com", true},
                {"a*c", "abc", true},
                {"a*", "abc", true},
                {"*", "abc", true},
                {"*c", "abC", false},
                {"*b*", "abc", true},
                {"ABC", "abc", false},
                {"bc", "abc", false},
                {"b*", "abc", false},
                {"*b", "abC", true},
                {"\\", "\\", true},
                {"abcd", "Abcdefg", false},
                {"abcd", "abcDefg", false},
                {"abcd", "abcdefg", true},
                {"defg", "abcdefg", false},
        };

        for (Object[] pnt : pats) {
            String pattern = (String) pnt[0];
            String text = (String) pnt[1];
            boolean expectedResult = (Boolean) pnt[2];

            assertTrue("'"+ pattern + "' matches '" + text + "'.", expectedResult == TextUtils.matches(pattern, text, true, false));
        }
    }

    public void testTail() {
        String last1Line = "line 4";
        String last2Line = "line 3\r" + last1Line;
        String last3Line = "line 2\n" + last2Line;
        String last4Line = "line 1\r\n" + last3Line;
        String text = last4Line + "\r\n";

        assertEquals("Last 1 line", last1Line, TextUtils.tail(text, 1));
        assertEquals("Last 2 lines", last2Line, TextUtils.tail(text, 2));
        assertEquals("Last 3 lines", last3Line, TextUtils.tail(text, 3));
        assertEquals("Last 4 lines", last4Line, TextUtils.tail(text, 4));
        assertEquals("Last 5 lines", last4Line, TextUtils.tail(text, 5));
    }
}
