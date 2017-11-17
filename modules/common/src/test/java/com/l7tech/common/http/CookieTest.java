package com.l7tech.common.http;

import com.l7tech.test.BugId;
import org.junit.Test;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for HttpCookies.
 * <p/>
 * User: steve
 * Date: Sep 28, 2005
 * Time: 10:07:11 AM
 * $Id$
 */
public class CookieTest {


    //- PUBLIC

    @Test
    public void testSetCookieProperties() {
        String name = "test";
        String value = "avalue";
        int version = 1;
        String path = "/test/path";
        String domain = ".testdomain.com";
        int maxAge = 300;
        boolean secure = false;
        String comment = "Blahblah";
        boolean httpOnly = false;

        HttpCookie cookie = new HttpCookie(name, value, version, path, domain, maxAge, secure, comment, httpOnly);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Checking version property", version, cookie.getVersion());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Checking max-age property", maxAge, cookie.getMaxAge());
        assertEquals("Should not be secure", false, cookie.isSecure());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertEquals("Checking comment property", comment, cookie.getComment());
        assertEquals("Should not be http only", httpOnly, cookie.isHttpOnly());
    }

    @Test
    public void testSimpleSetCookieHeader() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/apath/blah";
        String domain = "www.testdomain.com";
        String headerValue = name + "=" + value;

        HttpCookie cookie = new HttpCookie(new URL("http://" + domain + path + "/apage.html"), headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
        assertEquals("No expiry set", false, cookie.hasExpiry());
    }

    @Test
    public void testSetCookieHeaderValueEndsWithEquals() throws Exception {
        String name = "test";
        String value = "testvalue==";
        String path = "/apath/blah";
        String domain = "www.testdomain.com";
        String headerValue = name + "=" + value;

        HttpCookie cookie = new HttpCookie(new URL("http://" + domain + path + "/apage.html"), headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
        assertEquals("No expiry set", false, cookie.hasExpiry());
    }

    @Test
    public void testFullV0ExpiresSetCookieHeader() throws Exception {
        String name = "test";
        String value = "testvalue";
        String expires = "Sun, 17-Jan-2038 19:14:07 GMT";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String headerValue = name + "=" + value + "; Path=" + path + "; Domain=" + domain + "; Expires=" + expires;

        SimpleDateFormat expiryFormat = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expiryFormat.setLenient(false);
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertEquals("Checking expires property", expires, cookie.getExpires());
//        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking version property", 0, cookie.getVersion());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    @Test
    public void testFullV1SetCookieHeader() throws Exception {
        String name = "test";
        String value = "testvalue";
        int maxAge = 300;
        String path = "/test/path";
        String domain = ".testdomain.com";
        String comment = "\"This is a cookie\"";
        String headerValue = name + "=" + value + "; Path=" + path + "; Domain=" + domain + "; Max-Age=" + maxAge + "; Comment=" + comment + "; Secure; Version=1; HttpOnly";

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertEquals("Checking max-age property", maxAge, cookie.getMaxAge());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Checking comment property", comment, cookie.getComment());
        assertEquals("Should be secure", true, cookie.isSecure());
        assertEquals("Should be HttpOnly", true, cookie.isHttpOnly());
    }

    @Test
    public void testFullV1CookieHeader() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String headerValue = name + "=" + value + "; $Path=" + path + "; $Domain=" + domain + "; $Version=1";

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertNull(cookie.getComment());
        assertFalse(cookie.hasExpiry());
        assertEquals(-1, cookie.getMaxAge());
        assertFalse(cookie.isSecure());
    }

    @Test
    public void testIdentity() {
        HttpCookie cookie1 = new HttpCookie("name", "valuea", 1, "/", "a.domain.com");
        HttpCookie cookie2 = new HttpCookie("name", "valueb", 1, "/", "a.domain.com");
        HttpCookie cookie3 = new HttpCookie("name", "valuec", 1, "/path", "a.domain.com");
        HttpCookie cookie4 = new HttpCookie("name", "valued", 1, "/", "b.domain.com");
        HttpCookie cookie5 = new HttpCookie("Name", "valuee", 1, "/", "a.domain.com");

        assertEquals("Cookie identity test", cookie1, cookie2);
        assertEquals("Cookie identity case insensitivity test", cookie1, cookie5);
        assertFalse("Cookie path difference test", cookie1.equals(cookie3));
        assertFalse("Cookie domain difference test", cookie1.equals(cookie4));
    }

    @Test
    public void testCookieWithEqualsInValue() throws Exception {
        // Test as though a Set-Cookie
        HttpCookie cookie1 = new HttpCookie(new URL("http://www.layer7tech.com/"), "iPlanetDirectoryPro=AQIC5wM2LY4SfcyETgWNhyoF+BR2H4zTz5vMoPKxzP2EPDA=@AAJTSQACMDE=#");
        assertEquals("Value correct", "AQIC5wM2LY4SfcyETgWNhyoF+BR2H4zTz5vMoPKxzP2EPDA=@AAJTSQACMDE=#", cookie1.getCookieValue());

        // Test as though incoming
        HttpCookie cookie2 = new HttpCookie("iPlanetDirectoryPro", "AQIC5wM2LY4SfcyETgWNhyoF+BR2H4zTz5vMoPKxzP2EPDA=@AAJTSQACMDE=#", 0, null, null);
        assertEquals("Value correct", "AQIC5wM2LY4SfcyETgWNhyoF+BR2H4zTz5vMoPKxzP2EPDA=@AAJTSQACMDE=#", cookie2.getCookieValue());
    }

    /*
    * Date Format: Sun, 17-Jan-2038 19:14:07 GMT
    * */
    @Test
    public void testCookieExpiresNetscape() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun, 17-Jan-2038 19:14:07 GMT";
        String headerValue = name + "=" + value + "; expires=" + expires + "; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.NETSCAPE_RFC850_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expiryFormat.setLenient(false);
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking expires property", expires, cookie.getExpires());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date format: Sun, 06 Nov 1994 08:49:37 GMT
    * */
    @Test
    public void testCookieExpiresRfc1123() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun, 17 Jan 2038 19:14:07 GMT";
        String headerValue = name + "=" + value + "; expires=" + expires + "; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expiryFormat.setLenient(false);
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking expires property", expires, cookie.getExpires());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date format: Sunday, 06-Nov-94 08:49:37 GMT
    * This date format accepts two digit years. SimpleDateFormat will not always handle this two digit year as we
    * need it done as it may assume it's in the last century.
    * Internally HttpCookie will expand the year to the current century
    * */
    @Test
    public void testCookieExpiresRfc850() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sunday, 04-Nov-38 08:49:37 GMT";
        String expiresFullYear = "Sunday, 04-Nov-2038 08:49:37 GMT";
        String headerValue = name + "=" + value + "; expires=" + expires + "; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.NETSCAPE_RFC850_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expiresFullYear);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date format: Sun, 06 Nov 16 08:49:37 GMT
    * Parse the full year; check to see if the short year is within a second (because it's based off
    * System.currentTimeMillis()) of the cookie's expiry time. We do this because the date format accepts two
    * digit years. SimpleDateFormat will not always handle this two digit year as we need it done as it may assume
    * it's in the last century. Internally HttpCookie will expand the year to the current century.
    * */
    @Test
    public void testRfc1123Rfc1036Rfc822DateFormatParsedCorrectly() throws Exception {
        // Create some time in the future.
        final String nextFullYear = getNextYear();
        final String nextShortYear = nextFullYear.substring(2);
        final String date = "Sun, 06 Nov " + nextShortYear + " 08:49:37 GMT";
        final String dateWithFullYear = "Sun, 06 Nov " + nextFullYear + " 08:49:37 GMT";

        // Full year calculation.
        final SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        final long expiryTime = expiryFormat.parse(dateWithFullYear).getTime();

        // Create a cookie that uses the two character representation.
        final String name = "test";
        final String value = "testvalue";
        final String path = "/test/path";
        final String domain = ".testdomain.com";
        final String headerValue = name + "=" + value + "; expires=" + date + "; Path=" + path + "; Domain=" + domain + "; Version=0";
        final HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        // Compare to be very close. It won't be exactly the same because internally HttpCookie bases expiry time on
        // System.currentTimeMillis(), which will not be the same as the calculation above... But it should be close.
        assertTrue(Math.abs(cookie.getExpiryTime() - expiryTime) < 1000L);
    }

    private static String getNextYear() {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(Calendar.YEAR, 1);
        return Integer.toString(calendar.get(Calendar.YEAR));
    }

    /*
    * Using the same date format as above ("Sun, 06 Nov 16 08:49:37 GMT"), test the expiry input and calculated
    * max-age property. The same notes on date formats above apply here:  This date format accepts two digit years.
    * SimpleDateFormat will not always handle this two digit year as we need it done as it may assume it's in the
    * last century. Internally HttpCookie will expand the year to the current century.
    * */
    @Test
    public void testCookieExpiresRfc1036AndRfc822ExpiryMaxAgeInFuture() throws Exception {
        final String name = "test";
        final String value = "testvalue";
        final String path = "/test/path";
        final String domain = ".testdomain.com";

        // Create some time in the future.
        final String nextFullYear = getNextYear();
        final String nextShortYear = nextFullYear.substring(2);
        final String date = "Sun, 06 Nov " + nextShortYear + " 08:49:37 GMT";
        final String dateWithFullYear = "Sun, 06 Nov " + nextFullYear + " 08:49:37 GMT";

        // Calculate the expiry time based on the full four digit year.
        final SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        final long expiryTime = expiryFormat.parse(dateWithFullYear).getTime();

        // The max age property is expressed in seconds from now. It should be very close to this (but won't be exact)
        // because HttpCookie internally bases time calcuations on System.currentTimeMillis().
        int maxAgeTarget = (int) ((expiryTime - System.currentTimeMillis()) / 1000L);

        final String headerValue = name + "=" + value + "; expires=" + date + "; Path=" + path + "; Domain=" + domain + "; Version=0";
        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        // Allow for a little bit of difference. It should be very close to this (but won't be exact) because
        // HttpCookie internally bases time calcuations on System.currentTimeMillis().
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget));
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    @Test
    public void testCookieExpiresRfc1036AndRfc822ExpiryMaxAgeInPast() throws Exception {
        final String name = "test";
        final String value = "testvalue";
        final String path = "/test/path";
        final String domain = ".testdomain.com";

        // Create some time in the future.
        final String date = "Sun, 06 Nov 16 08:49:37 GMT";
        final String dateWithFullYear = "Sun, 06 Nov 2016 08:49:37 GMT";

        // Calculate the expiry time based on the full four digit year.
        final SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        final long expiryTime = expiryFormat.parse(dateWithFullYear).getTime();

        // The max age property is expressed in seconds from now. It should be very close to this (but won't be exact)
        // because HttpCookie internally bases time calcuations on System.currentTimeMillis().
        int maxAgeTarget = (int) ((expiryTime - System.currentTimeMillis()) / 1000L);

        final String headerValue = name + "=" + value + "; expires=" + date + "; Path=" + path + "; Domain=" + domain + "; Version=0";
        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertEquals("Making sure cookie max-age is zero.", 0, cookie.getMaxAge());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date Format: Sun Nov  6 08:49:37 1994
    * */
    @Test
    public void testCookieExpiresAnsiC_SingleDayDigit() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun Nov  6 08:49:37 2038";
        String headerValue = name + "=" + value + "; expires=" + expires + "; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.ANSI_C_DATEFORMAT, Locale.US);
        //Don't set a time zone as this time format doesn't specify. Will have to go with our local timezone
        //expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date Format: Sun Nov  6 08:49:37 1994
    * */
    @Test
    public void testCookieExpiresAnsiC_DoubleDayDigit() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun Nov 06 08:49:37 2038";
        String headerValue = name + "=" + value + "; expires=" + expires + "; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.ANSI_C_DATEFORMAT, Locale.US);
        //Don't set a time zone as this time format doesn't specify. Will have to go with our local timezone
        //expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    //Wed Aug 29 07:00:00 2007 GMT
    @Test
    public void testCookieExpiresAmazon() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Wed Aug 29 07:00:00 2038 GMT";
        String headerValue = name + "=" + value + "; expires=" + expires + "; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.AMAZON_DATEFORMAT, Locale.US);
        //Don't set a time zone as this time format doesn't specify. Will have to go with our local timezone
        //expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int) ((expiresDate.getTime() - System.currentTimeMillis()) / 1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge() - maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    @Test
    public void nullRequestDomainAndPath() throws Exception {
        final HttpCookie cookie = new HttpCookie((String) null, null, "foo=bar");
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
    }

    @Test
    public void copyCookieWithNullDomainAndPath() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        final HttpCookie copy = new HttpCookie(cookie, null, null);
        assertNull(copy.getDomain());
        assertNull(copy.getPath());
        assertFalse(copy.isDomainExplicit());
    }

    @Test
    public void parseCookieDoesNotTrimQuotesForNonNumericAttributes() throws Exception {
        final HttpCookie cookie = new HttpCookie("\"foo\"=\"bar\";domain=\"localhost\";path=\"/\";comment=\"test\";version=\"1\";max-age=\"60\"");
        assertEquals("\"foo\"", cookie.getCookieName());
        assertEquals("\"bar\"", cookie.getCookieValue());
        assertEquals("\"localhost\"", cookie.getDomain());
        assertEquals("\"/\"", cookie.getPath());
        assertEquals("\"test\"", cookie.getComment());
        assertEquals(1, cookie.getVersion());
        assertEquals(60, cookie.getMaxAge());
    }

    @Test
    public void parseValueWithSemiColonInDoubleQuotes() throws Exception {
        final HttpCookie cookie = new HttpCookie("SCALAEYE_SESSION=\"()<>@,;:\\\\\\”[]?={} \\t\"; Path=/");
        assertEquals("SCALAEYE_SESSION", cookie.getCookieName());
        assertEquals("\"()<>@,;:\\\\\\”[]?={} \\t\"", cookie.getCookieValue());
        assertEquals("/", cookie.getPath());
    }

    @BugId("SSG-8746")
    @Test
    public void parseCookieNoValueAndNoAttributes() throws Exception {
        final HttpCookie cookie = new HttpCookie("foo");
        assertEquals("foo", cookie.getCookieName());
        assertEquals("", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
    }

    @BugId("SSG-8746")
    @Test
    public void parseCookieNoValue() throws Exception {
        final HttpCookie cookie = new HttpCookie("foo; domain=localhost; path=/");
        assertEquals("foo", cookie.getCookieName());
        assertEquals("", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertEquals(-1, cookie.getMaxAge());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
    }

    @Test
    public void parseCookieMultipleEquals() throws Exception {
        final HttpCookie cookie = new HttpCookie("foo=bar=bar");
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar=bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertNull(cookie.getComment());
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());
    }

    @Test(expected = HttpCookie.IllegalFormatException.class)
    public void parseCookieEmpty() throws Exception {
        new HttpCookie("");
    }

    @BugId("SSG-9076")
    @Test
    public void constructorVersion0WithComment() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 0, null, null, -1, false, "test", false, null);
        assertEquals("test", cookie.getComment());
    }
}
