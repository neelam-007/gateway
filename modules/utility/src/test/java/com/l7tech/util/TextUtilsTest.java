package com.l7tech.util;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests for TextUtils
 *
 * @author Steve Jones
 */
public class TextUtilsTest {

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testConvertLineBreaks() {
        final String CR = "\r";
        final String LF = "\n";
        final String CRLF = "\r\n";

        assertEquals("null - should return null", null, TextUtils.convertLineBreaks(null, CR));

        final String empty = "";
        assertTrue("empty - should return same String object", empty == TextUtils.convertLineBreaks(empty, CR));

        final String noLineBreak = "123456";
        assertTrue("No line break - should return same String object", noLineBreak == TextUtils.convertLineBreaks(noLineBreak, CR));
        assertTrue("No line break - should return same String object", noLineBreak == TextUtils.convertLineBreaks(noLineBreak, LF));
        assertTrue("No line break - should return same String object", noLineBreak == TextUtils.convertLineBreaks(noLineBreak, CRLF));

        final String crOnly = "1\r2\r3\r";
        final String lfOnly = "1\n2\n3\n";
        final String crlfOnly = "1\r\n2\r\n3\r\n";

        assertTrue("CR to CR - should return same String object", crOnly == TextUtils.convertLineBreaks(crOnly, CR));
        assertEquals("CR to LF", lfOnly, TextUtils.convertLineBreaks(crOnly, LF));
        assertEquals("CR to CR-LF", crlfOnly, TextUtils.convertLineBreaks(crOnly, CRLF));

        assertEquals("LF to CR", crOnly, TextUtils.convertLineBreaks(lfOnly, CR));
        assertTrue("LF to LF - should return same String object", lfOnly == TextUtils.convertLineBreaks(lfOnly, LF));
        assertEquals("LF to CR-LF", crlfOnly, TextUtils.convertLineBreaks(lfOnly, CRLF));

        assertEquals("CR-LF to CR", crOnly, TextUtils.convertLineBreaks(crlfOnly, CR));
        assertEquals("CR-LF to LF", lfOnly, TextUtils.convertLineBreaks(crlfOnly, LF));
        assertTrue("CR-LF to CR-LF - should return same String object", crlfOnly == TextUtils.convertLineBreaks(crlfOnly, CRLF));

        final String mixed = "1\r2\n3\r\n4";
        assertEquals("mixed to CR", "1\r2\r3\r4", TextUtils.convertLineBreaks(mixed, CR));
        assertEquals("mixed to LF", "1\n2\n3\n4", TextUtils.convertLineBreaks(mixed, LF));
        assertEquals("mixed to CR-LF", "1\r\n2\r\n3\r\n4", TextUtils.convertLineBreaks(mixed, CRLF));

        final String mixedWithTrailingCR = mixed + "\r";
        assertEquals("mixed to CR", "1\r2\r3\r4\r", TextUtils.convertLineBreaks(mixedWithTrailingCR, CR));
        final String mixedWithTrailingLF = mixed + "\n";
        assertEquals("mixed to LF", "1\n2\n3\n4\n", TextUtils.convertLineBreaks(mixedWithTrailingLF, LF));
        final String mixedWithTrailingCRLF = mixed + "\r\n";
        assertEquals("mixed to CR-LF", "1\r\n2\r\n3\r\n4\r\n", TextUtils.convertLineBreaks(mixedWithTrailingCRLF, CRLF));
    }
    
    @Test
    public void testGlobToRegexp() {
        assertEquals("", TextUtils.globToRegex(""));
        assertEquals("adze1290ADZE", TextUtils.globToRegex("adze1290ADZE"));
        assertEquals("foo.*blah.\\.jar", TextUtils.globToRegex("foo*blah?.jar"));
        assertEquals("snarf\\$gobble\\_blarf\\`22", TextUtils.globToRegex("snarf$gobble_blarf`22"));
    }

    @Test
    public void testTruncateMiddleExact(){

        String s = "12345";
        String expected = TextUtils.truncStringMiddleExact(s,2);
        assertTrue("String equals to s should be returned, expected: "+s+", actual was: " + expected, s.equals(expected));

        expected = TextUtils.truncStringMiddleExact(s,10);
        assertTrue("String equals to s should be returned", s.equals(expected));

        s = "1234567890";

        expected = "1...0";
        String actual = TextUtils.truncStringMiddleExact(s,5);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "12...0";
        actual = TextUtils.truncStringMiddleExact(s,6);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "12...90";
        actual = TextUtils.truncStringMiddleExact(s,7);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "123...90";
        actual = TextUtils.truncStringMiddleExact(s,8);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "123...890";
        actual = TextUtils.truncStringMiddleExact(s,9);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));
    }

    @Test
    public void testTruncateEnd(){

        String s = "1234";
        String expected = TextUtils.truncateStringAtEnd(s,2);
        assertTrue("String equals to s should be returned, expected: "+s+", actual was: " + expected, s.equals(expected));

        expected = TextUtils.truncateStringAtEnd(s,10);
        assertTrue("String equals to s should be returned", s.equals(expected));

        s = "1234567890";

        expected = "1...";
        String actual = TextUtils.truncateStringAtEnd(s,4);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "123...";
        actual = TextUtils.truncateStringAtEnd(s,6);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "1234...";
        actual = TextUtils.truncateStringAtEnd(s,7);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "12345...";
        actual = TextUtils.truncateStringAtEnd(s,8);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));

        expected = "123456...";
        actual = TextUtils.truncateStringAtEnd(s,9);
        assertTrue("Tructated string should equal: "+ expected+" it was: " + actual, expected.equals(actual));
    }

    @Test
    public void testEnforceToBreakOnMultipleLines() {
        final int MAX_LEN = 5;
        final String testInput = "0123456789abcdef";
        final String expectedOutput = "01234<br>56789<br>abcde<br>f<br>";
        final String actualOutput = TextUtils.enforceToBreakOnMultipleLines(testInput, MAX_LEN, "<br>", true);

        assertNotSame(expectedOutput, testInput);
        assertEquals("Correctly enforce to break a string on multiple lines", expectedOutput, actualOutput);
    }

    @Test
    public void testEscapeHtmlSpecialCharacters() {
        final String testInput = "abc&#1234;abc<br>abc/abc\"abc\'abc";
        final String expectedOutput = "abc&amp;#1234;abc&lt;br&gt;abc&#47;abc&quot;abc&#39;abc";
        final String actualOutput = TextUtils.escapeHtmlSpecialCharacters(testInput);

        assertEquals("Correctly escape HTML special characters", expectedOutput, actualOutput);
    }

    @Test
    public void testMakeIgnorableCharactersViewableAsUnicode(){
        StringBuilder builder = new StringBuilder("Hello\u0000World");
        TextUtils.makeIgnorableCharactersViewableAsUnicode(builder);
        assertEquals("Unicode character should be converted into characters", "Hello\\u0000World", builder.toString());
    }

    @Test
    public void testUriSplitPattern() throws Exception {
        String test = "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig1 urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig2 urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig";
        String[] tokens = TextUtils.URI_STRING_SPLIT_PATTERN.split(test);
        assertEquals(3, tokens.length);

        String val = "one  two three   four";
        tokens = TextUtils.URI_STRING_SPLIT_PATTERN.split(val);
        assertEquals(4, tokens.length);

        String s = "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig1 \n  " +
                "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig\n  " +
                "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig3  \n" +
                "urn:oasis:names:tc:SAML:2.0:ac:classes:Password";

        tokens = TextUtils.URI_STRING_SPLIT_PATTERN.split(s);
        for (String token : tokens) {
            System.out.println(token);
        }
        assertEquals(4, tokens.length);
    }
}