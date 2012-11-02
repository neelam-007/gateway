package com.l7tech.util;

import static com.l7tech.util.Option.some;

import com.l7tech.test.BugNumber;
import com.l7tech.util.ValidationUtils.Validator;
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
        assertFalse("Invalid empty", ValidationUtils.isValidLong("", false, 0L, 1000L ));
        assertFalse("Invalid range min", ValidationUtils.isValidLong("1", false, 10L, 1000L ));
        assertFalse("Invalid range max", ValidationUtils.isValidLong("10000", false, 10L, 1000L ));
        assertFalse("Invalid negative", ValidationUtils.isValidLong("-1", false, 10L, 1000L ));
        assertFalse("Invalid negative min", ValidationUtils.isValidLong("-101", false, -100L, -10L ));
        assertFalse("Invalid negative max", ValidationUtils.isValidLong("-1", false, -100L, -10L ));

        assertTrue("Valid simple",  ValidationUtils.isValidLong("10", false, 1L, 100L ));
        assertTrue("Valid large",  ValidationUtils.isValidLong("100000000000", false, 10000000000L, 100000000000L));
        assertTrue("Valid empty", ValidationUtils.isValidLong("", true, 0L, 1000L ));
        assertTrue("Valid inclusive min",  ValidationUtils.isValidLong("10", false, 10L, 100L ));
        assertTrue("Valid inclusive max",  ValidationUtils.isValidLong("100", false, 10L, 100L ));
        assertTrue("Valid inclusive min/max",  ValidationUtils.isValidLong("10", false, 10L, 10L ));
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

    @Test
    public void testIntegerValidator() {
        final Validator<String> validatorS = ValidationUtils.getIntegerTextValidator( 1, 10 );
        final Validator<Integer> validatorI = ValidationUtils.getIntegerValidator( ConversionUtils.<Integer>getIdentityConverter(), 1, 10 );

        assertTrue( "Valid string 1", validatorS.isValid( "1" ) );
        assertTrue( "Valid integer 1", validatorI.isValid( 1 ) );

        assertFalse( "Invalid string 0", validatorS.isValid( "0" ) );
        assertFalse( "Invalid integer 0", validatorI.isValid( 0 ) );

        assertTrue( "Valid string 10 with space", validatorS.isValid( " 10 " ) );
        assertTrue( "Valid integer 10", validatorI.isValid( 10 ) );

        assertFalse( "Invalid string 11", validatorS.isValid( "11" ) );
        assertFalse( "Invalid integer 11", validatorI.isValid( 11 ) );

        assertFalse( "Invalid string null", validatorS.isValid( null ) );
        assertFalse( "Invalid string empty", validatorS.isValid( null ) );

        assertTrue( some( "1" ).filter( validatorS ).isSome() );
        assertFalse( some( "-1" ).filter( validatorS ).isSome() );
        assertFalse( some( "R1" ).filter( validatorS ).isSome() );
        assertTrue( some( 1 ).filter( validatorI ).isSome() );
        assertFalse( some( -1 ).filter( validatorI ).isSome() );
    }

    @Test
    public void testLongValidator() {
        final Validator<String> validatorS = ValidationUtils.getLongTextValidator( -10000000000L, 10000000000L );
        final Validator<String> validatorSTU = ValidationUtils.getLongValidator( ConversionUtils.getTimeUnitTextToLongConverter(), -10000000000L, 10000000000L );
        final Validator<Long> validatorL = ValidationUtils.getLongValidator( ConversionUtils.<Long>getIdentityConverter(), -10000000000L, 10000000000L );

        assertTrue( "Valid string -10000000000", validatorS.isValid( "-10000000000" ) );
        assertTrue( "Valid string -100d", validatorSTU.isValid( "-100d" ) );
        assertTrue( "Valid integer -10000000000", validatorL.isValid( -10000000000L ) );

        assertFalse( "Invalid string -10000000001", validatorS.isValid( " -10000000001" ) );
        assertFalse( "Invalid string 1000d", validatorSTU.isValid( "1000d" ) );
        assertFalse( "Invalid integer -10000000001", validatorL.isValid(  -10000000001L ) );

        assertTrue( "Valid string 10000000000 with space", validatorS.isValid( " 10000000000 " ) );
        assertTrue( "Valid string 100d with space", validatorSTU.isValid( " 100d " ) );
        assertTrue( "Valid integer 10000000000", validatorL.isValid( 10000000000L ) );

        assertFalse( "Invalid string 10000000001", validatorS.isValid( "10000000001" ) );
        assertFalse( "Invalid integer 10000000001", validatorL.isValid( 10000000001L ) );

        assertFalse( "Invalid string null", validatorS.isValid( null ) );
        assertFalse( "Invalid string empty", validatorS.isValid( null ) );
        assertFalse( "Invalid string null timeunit", validatorSTU.isValid( null ) );
        assertFalse( "Invalid string empty timeunit", validatorSTU.isValid( null ) );
        assertFalse( "Invalid string syntax timeunit", validatorSTU.isValid( "asdf" ) );

        assertTrue( "Valid time unit 100 d", validatorSTU.isValid( "100 d" ) );
        assertTrue( "Valid time unit [ 100d]", validatorSTU.isValid( " 100d" ) );
        assertTrue( "Valid time unit [100d ]", validatorSTU.isValid( "100d " ) );
        assertTrue( "Valid time unit 1,0,0 d", validatorSTU.isValid( "1,0,0 d" ) );
        assertFalse( "Invalid time unit 100  d", validatorSTU.isValid( "100  d" ) );
        assertFalse( "Invalid time unit 1 0 0 d", validatorSTU.isValid( "1 0 0 d" ) );
        assertFalse( "Invalid time unit 100de", validatorSTU.isValid( "100de" ) );
        assertFalse( "Invalid time unit 100X", validatorSTU.isValid( "100X" ) );

        assertTrue( some( "10000000000" ).filter( validatorS ).isSome() );
        assertFalse( some( "10000000001" ).filter( validatorS ).isSome() );
        assertFalse( some( "1L" ).filter( validatorS ).isSome() );
        assertTrue( some( 10000000000L ).filter( validatorL ).isSome() );
        assertFalse( some( -10000000001L ).filter( validatorL ).isSome() );
    }

    @Test
    public void testPatternValidator() {
        final Validator<String> validator = ValidationUtils.getPatternTextValidator( Pattern.compile( "[a-z]{1,3}" ) );

        assertTrue( "Valid string abc", validator.isValid( "abc" ) );
        assertFalse( "Invalid string abcd", validator.isValid( "abcd" ) );
    }

    @BugNumber(11771)
    @Test
    public void testIsValidUri() throws Exception {
        assertFalse(ValidationUtils.isValidUri(""));
        assertFalse(ValidationUtils.isValidUri(null));
        assertFalse(ValidationUtils.isValidUri("invalid%"));
        assertFalse(ValidationUtils.isValidUri("invalid invalid"));

        assertTrue(ValidationUtils.isValidUri("valid"));
        assertTrue(ValidationUtils.isValidUri("http://valid.com:8080/path?query"));
    }

    @BugNumber(11594)
    @Test
    public void testIsValidUriQuery() throws Exception {
        assertTrue(ValidationUtils.isValidUri("http://valid.com:8080/path"));
        assertFalse(ValidationUtils.isValidUri("http://valid.com:8080/path?query=abc]"));
        assertFalse(ValidationUtils.isValidUri("http://valid.com:8080/path?query=[abc]"));
        assertFalse(ValidationUtils.isValidUri("http://valid.com:8080/path?query=abc]"));

        assertFalse(ValidationUtils.isValidUrl("http://valid.com:8080/path?query=abc]", false, null));
        assertFalse(ValidationUtils.isValidUrl("http://valid.com:8080/path?query=[abc]", false, null));
        assertFalse(ValidationUtils.isValidUrl("http://valid.com:8080/path?query=abc]", false, null));
    }
}
