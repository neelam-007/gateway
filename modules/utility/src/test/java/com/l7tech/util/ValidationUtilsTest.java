package com.l7tech.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.regex.Pattern;

/**
 * Tests for validation utilities
 */
public class ValidationUtilsTest {

    @Test
    public void testHttpUrlRegex() {
        assertFalse("Invalid no host", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http:///pathhere"));
        assertFalse("Invalid port", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://host:88888/pathhere"));
        assertFalse("Invalid characters", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://host:88/path here"));

        assertTrue("Valid http", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost/some/path?a=b&3=4"));
        assertTrue("Valid https", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost"));
        assertTrue("Valid commas", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost/path,with,commas"));
        assertTrue("Valid semi", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost/path;with;..."));
        assertTrue("Valid url escaping", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://localhost:8181/this%20is%20a%20valid%20url"));

        assertTrue("Valid url query", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://example.org?" ));
        assertTrue("Valid url fragment", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://example.org/page.html#frag" ));
        assertTrue("Valid url odd chars", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://example.org/~,$'*;" ));
    }

    @Test
    public void testValidLong() {
        assertFalse("Invalid empty", ValidationUtils.isValidLong("", false, 0, 1000));
        assertFalse("Invalid range min", ValidationUtils.isValidLong("1", false, 10, 1000));
        assertFalse("Invalid range max", ValidationUtils.isValidLong("10000", false, 10, 1000));
        assertFalse("Invalid negative", ValidationUtils.isValidLong("-1", false, 10, 1000));
        assertFalse("Invalid negative min", ValidationUtils.isValidLong("-101", false, -100, -10));
        assertFalse("Invalid negative max", ValidationUtils.isValidLong("-1", false, -100, -10));

        assertTrue("Valid simple",  ValidationUtils.isValidLong("10", false, 1, 100));
        assertTrue("Valid large",  ValidationUtils.isValidLong("100000000000", false, 10000000000L, 100000000000L));
        assertTrue("Valid empty", ValidationUtils.isValidLong("", true, 0, 1000));
        assertTrue("Valid inclusive min",  ValidationUtils.isValidLong("10", false, 10, 100));
        assertTrue("Valid inclusive max",  ValidationUtils.isValidLong("100", false, 10, 100));
        assertTrue("Valid inclusive min/max",  ValidationUtils.isValidLong("10", false, 10, 10));
    }

    @Test
    public void testValidInteger() {
        assertFalse("Invalid empty", ValidationUtils.isValidInteger("", false, 0, 1000));
        assertFalse("Invalid range min", ValidationUtils.isValidInteger("1", false, 10, 1000));
        assertFalse("Invalid range max", ValidationUtils.isValidInteger("10000", false, 10, 1000));
        assertFalse("Invalid negative", ValidationUtils.isValidInteger("-1", false, 10, 1000));
        assertFalse("Invalid negative min", ValidationUtils.isValidInteger("-101", false, -100, -10));
        assertFalse("Invalid negative max", ValidationUtils.isValidInteger("-1", false, -100, -10));

        assertTrue("Valid simple",  ValidationUtils.isValidInteger("10", false, 1, 100));
        assertTrue("Valid empty", ValidationUtils.isValidInteger("", true, 0, 1000));
        assertTrue("Valid inclusive min",  ValidationUtils.isValidInteger("10", false, 10, 100));
        assertTrue("Valid inclusive max",  ValidationUtils.isValidInteger("100", false, 10, 100));
        assertTrue("Valid inclusive min/max",  ValidationUtils.isValidInteger("10", false, 10, 10));
    }

    @Test
    public void testValidCharacters() {
        assertFalse("Invalid none", ValidationUtils.isValidCharacters("a",""));
        assertFalse("Invalid simple", ValidationUtils.isValidCharacters("a","b"));
        assertFalse("Invalid case", ValidationUtils.isValidCharacters("a","A"));
        assertFalse("Invalid char", ValidationUtils.isValidCharacters("aaabaa","a"));
        assertFalse("Invalid all char", ValidationUtils.isValidCharacters("abcdef","ghijklm"));

        assertTrue("Valid simple", ValidationUtils.isValidCharacters("a","a"));
        assertTrue("Valid complex", ValidationUtils.isValidCharacters("This is a test for validation of a longer string of characters. All alpha and some punctuation used.","abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ. "));
    }

    @Test
    public void testValidUrl() {
        assertFalse( "Invalid none", ValidationUtils.isValidUrl( "" ) );
        assertFalse( "Invalid http url", ValidationUtils.isValidUrl( "http:/path" ) );
        assertFalse( "Invalid http url", ValidationUtils.isValidUrl( "http:///path" ) );
        assertFalse( "Invalid ftp url", ValidationUtils.isValidUrl( "ftp:/path" ) );
        assertFalse( "Invalid ftp url", ValidationUtils.isValidUrl( "ftp:///path" ) );
        assertFalse( "Invalid url syntax", ValidationUtils.isValidUrl( "htt p:///path" ) );
        assertFalse( "Invalid url protocol", ValidationUtils.isValidUrl( "unknown:///path" ) );

        assertTrue( "Valid none", ValidationUtils.isValidUrl( "", true ) );
        assertTrue( "Valid http url", ValidationUtils.isValidUrl( "HtTp://host/path/to/file" ) );
        assertTrue( "Valid file url 1 slash", ValidationUtils.isValidUrl( "File:/path/to/file" ) );
        assertTrue( "Valid file url 2 slash", ValidationUtils.isValidUrl( "fiLe://path/to/file" ) );
        assertTrue( "Valid file url 3 slash", ValidationUtils.isValidUrl( "file:///path/to/file" ) );
        assertTrue( "Valid ftp url", ValidationUtils.isValidUrl( "fTp://host/path/to/file" ) );
    }
}
