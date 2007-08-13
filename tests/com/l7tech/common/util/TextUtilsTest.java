package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

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
}
