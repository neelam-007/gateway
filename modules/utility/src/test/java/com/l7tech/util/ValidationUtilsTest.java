package com.l7tech.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.regex.Pattern;

/**
 * Tests for validation utilities
 */
public class ValidationUtilsTest {

    @Test
    public void testHttpUrlRegex() {
        Assert.assertFalse("Invalid no host", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http:///pathhere"));   
        Assert.assertFalse("Invalid port", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://host:88888/pathhere"));
        Assert.assertFalse("Invalid characters", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://host:88/path here"));

        Assert.assertTrue("Valid http", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost/some/path?a=b&3=4"));
        Assert.assertTrue("Valid https", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost"));
        Assert.assertTrue("Valid commas", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost/path,with,commas"));
        Assert.assertTrue("Valid semi", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://somehost/path;with;..."));
        Assert.assertTrue("Valid url escaping", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://localhost:8181/this%20is%20a%20valid%20url"));

        Assert.assertTrue("Valid url query", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://example.org?" ));
        Assert.assertTrue("Valid url fragment", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://example.org/page.html#frag" ));
        Assert.assertTrue("Valid url odd chars", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://example.org/~,$'*;" ));
        Assert.assertTrue("Valid url escape host", Pattern.matches(ValidationUtils.getHttpUrlRegex(), "http://caf%C3%A9.example.org" ));
    }

    @Test
    public void testValidLong() {
        Assert.assertFalse("Invalid empty", ValidationUtils.isValidLong("", false, 0, 1000));
        Assert.assertFalse("Invalid range min", ValidationUtils.isValidLong("1", false, 10, 1000));
        Assert.assertFalse("Invalid range max", ValidationUtils.isValidLong("10000", false, 10, 1000));
        Assert.assertFalse("Invalid negative", ValidationUtils.isValidLong("-1", false, 10, 1000));
        Assert.assertFalse("Invalid negative min", ValidationUtils.isValidLong("-101", false, -100, -10));
        Assert.assertFalse("Invalid negative max", ValidationUtils.isValidLong("-1", false, -100, -10));

        Assert.assertTrue("Valid simple",  ValidationUtils.isValidLong("10", false, 1, 100));
        Assert.assertTrue("Valid large",  ValidationUtils.isValidLong("100000000000", false, 10000000000L, 100000000000L));
        Assert.assertTrue("Valid empty", ValidationUtils.isValidLong("", true, 0, 1000));
        Assert.assertTrue("Valid inclusive min",  ValidationUtils.isValidLong("10", false, 10, 100));
        Assert.assertTrue("Valid inclusive max",  ValidationUtils.isValidLong("100", false, 10, 100));
        Assert.assertTrue("Valid inclusive min/max",  ValidationUtils.isValidLong("10", false, 10, 10));
    }

    @Test
    public void testValidInteger() {
        Assert.assertFalse("Invalid empty", ValidationUtils.isValidInteger("", false, 0, 1000));
        Assert.assertFalse("Invalid range min", ValidationUtils.isValidInteger("1", false, 10, 1000));
        Assert.assertFalse("Invalid range max", ValidationUtils.isValidInteger("10000", false, 10, 1000));
        Assert.assertFalse("Invalid negative", ValidationUtils.isValidInteger("-1", false, 10, 1000));
        Assert.assertFalse("Invalid negative min", ValidationUtils.isValidInteger("-101", false, -100, -10));
        Assert.assertFalse("Invalid negative max", ValidationUtils.isValidInteger("-1", false, -100, -10));

        Assert.assertTrue("Valid simple",  ValidationUtils.isValidInteger("10", false, 1, 100));
        Assert.assertTrue("Valid empty", ValidationUtils.isValidInteger("", true, 0, 1000));
        Assert.assertTrue("Valid inclusive min",  ValidationUtils.isValidInteger("10", false, 10, 100));
        Assert.assertTrue("Valid inclusive max",  ValidationUtils.isValidInteger("100", false, 10, 100));
        Assert.assertTrue("Valid inclusive min/max",  ValidationUtils.isValidInteger("10", false, 10, 10));
    }

    @Test
    public void testValidCharacters() {
        Assert.assertFalse("Invalid none", ValidationUtils.isValidCharacters("a",""));
        Assert.assertFalse("Invalid simple", ValidationUtils.isValidCharacters("a","b"));
        Assert.assertFalse("Invalid case", ValidationUtils.isValidCharacters("a","A"));
        Assert.assertFalse("Invalid char", ValidationUtils.isValidCharacters("aaabaa","a"));
        Assert.assertFalse("Invalid all char", ValidationUtils.isValidCharacters("abcdef","ghijklm"));

        Assert.assertTrue("Valid simple", ValidationUtils.isValidCharacters("a","a"));
        Assert.assertTrue("Valid complex", ValidationUtils.isValidCharacters("This is a test for validation of a longer string of characters. All alpha and some punctuation used.","abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ. "));
    }
}
