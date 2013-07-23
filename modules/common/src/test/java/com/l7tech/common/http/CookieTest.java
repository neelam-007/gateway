package com.l7tech.common.http;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.logging.Logger;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.text.SimpleDateFormat;

/**
 * Tests for HttpCookies.
 *
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
        int version = 0;
        String path = "/test/path";
        String domain = ".testdomain.com";
        int maxAge = 300;
        boolean secure = false;
        String comment = "Blahblah";

        HttpCookie cookie = new HttpCookie(name, value, version, path, domain, maxAge, secure, comment);

        assertEquals("Cookie should be new (as from a Set-Cookie header)", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Checking version property", version, cookie.getVersion());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Checking max-age property", maxAge, cookie.getMaxAge());
        assertEquals("Should not be secure", false, cookie.isSecure());
        assertEquals("Expiry set",true,cookie.hasExpiry());
        assertNull("Checking comment property",cookie.getComment()); // should be null since version 0 cookies don't have comments
    }

    @Test
    public void testSimpleSetCookieHeader() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/apath/blah";
        String domain = "www.testdomain.com";
        String headerValue = name + "=" + value;

        HttpCookie cookie = new HttpCookie(new URL("http://"+domain+path+"/apage.html"), headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
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

        HttpCookie cookie = new HttpCookie(new URL("http://"+domain+path+"/apage.html"), headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
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
        String headerValue = name + "=" + value + "; Path=" + path + "; Domain=" + domain + "; Expires=" +expires;

        SimpleDateFormat expiryFormat = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expiryFormat.setLenient(false);
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
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
        String comment = "This is a cookie";
        String headerValue = name + "=" + value + "; Path=" + path + "; Domain=" + domain + "; Max-Age=" +maxAge+ "; Comment=\""+comment+"\"; Secure; Version=1";

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertEquals("Checking max-age property", maxAge, cookie.getMaxAge());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Checking comment property", comment, cookie.getComment());
        assertEquals("Should be secure", true, cookie.isSecure());
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
    public void testEnsurePathDomain() {
        HttpCookie cookie1 = new HttpCookie("name", "valuea", 1, "/some", ".domain.com");

        HttpCookie cookie2 = CookieUtils.ensureValidForDomainAndPath(cookie1, "a.domain.com", "/some/path");
        assertEquals("Check valid", cookie1, cookie2);

        HttpCookie cookie3 = CookieUtils.ensureValidForDomainAndPath(cookie1, "b.bdomain.com", "/some/path");
        assertFalse("Check modified domain", cookie1.equals(cookie3));

        HttpCookie cookie4 = CookieUtils.ensureValidForDomainAndPath(cookie1, "a.domain.com", "/otherpath");
        assertFalse("Check modified path", cookie1.equals(cookie4));

        HttpCookie cookie5 = CookieUtils.ensureValidForDomainAndPath(cookie1, "a.domain.com", null);
        assertEquals("Check unmodified path", cookie1, cookie5);
    }

    @Test
    public void testQuoting() {
        HttpCookie cookie1 = new HttpCookie("name", "value with spaces", 1, "/some", ".domain.com");
        String header1 = cookie1.getV0CookieHeaderPart();
        assertEquals("Correctly quoted", "name=\"value with spaces\"", header1);

        HttpCookie cookie2 = new HttpCookie("name", "value_with_;", 1, "/some", ".domain.com");
        String header2 = cookie2.getV0CookieHeaderPart();
        assertEquals("Correctly quoted", "name=\"value_with_;\"", header2);

        HttpCookie cookie3 = new HttpCookie("name", "value_without_spaces", 1, "/some", ".domain.com");
        String header3 = cookie3.getV0CookieHeaderPart();
        assertEquals("Correctly quoted", "name=value_without_spaces", header3);
    }

    @Test
    public void testCookieHeader() {
        HttpCookie cookie1 = new HttpCookie("name", "value", 1, "/", ".layer7tech.com");
        String header1 = HttpCookie.getCookieHeader(Collections.singletonList(cookie1));
        assertEquals("Cookie header", "name=value", header1);

        HttpCookie cookie2 = new HttpCookie("name2", "value2", 1, "/", ".layer7tech.com");
        List cookieList2 = new ArrayList();
        cookieList2.add(cookie1);
        cookieList2.add(cookie2);
        String header2 = HttpCookie.getCookieHeader(cookieList2);
        assertEquals("Cookie header", "name=value; name2=value2", header2);

        HttpCookie cookie3 = new HttpCookie("name3", "value with \"spaces", 1, "/", ".layer7tech.com");
        List cookieList3 = new ArrayList();
        cookieList3.add(cookie1);
        cookieList3.add(cookie2);
        cookieList3.add(cookie3);
        String header3 = HttpCookie.getCookieHeader(cookieList3);
        assertEquals("Cookie header", "name=value; name2=value2; name3=\"value with \\\"spaces\"", header3);
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
    public void testCookieExpiresNetscape() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun, 17-Jan-2038 19:14:07 GMT";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.NETSCAPE_RFC850_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expiryFormat.setLenient(false);
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date format: Sun, 06 Nov 1994 08:49:37 GMT
    * */
    @Test
    public void testCookieExpiresRfc1123() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun, 17 Jan 2038 19:14:07 GMT";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        expiryFormat.setLenient(false);
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
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
    public void testCookieExpiresRfc850() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sunday, 04-Nov-38 08:49:37 GMT";
        String expiresFullYear = "Sunday, 04-Nov-2038 08:49:37 GMT";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.NETSCAPE_RFC850_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expiresFullYear);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date format: Sun, 06 Nov 16 08:49:37 GMT
    * This date format accepts two digit years. SimpleDateFormat will not always handle this two digit year as we
    * need it done as it may assume it's in the last century.
    * Internally HttpCookie will expand the year to the current century
    * */
    @Test
    public void testCookieExpiresRfc1036AndRfc822() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun, 06 Nov 16 08:49:37 GMT";
        String expiresFullYear = "Sun, 06 Nov 2016 08:49:37 GMT";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expiresFullYear);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date Format: Sun Nov  6 08:49:37 1994
    * */
    @Test
    public void testCookieExpiresAnsiC_SingleDayDigit() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun Nov  6 08:49:37 2038";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.ANSI_C_DATEFORMAT, Locale.US);
        //Don't set a time zone as this time format doesn't specify. Will have to go with our local timezone
        //expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    /*
    * Date Format: Sun Nov  6 08:49:37 1994
    * */
    @Test
    public void testCookieExpiresAnsiC_DoubleDayDigit() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Sun Nov 06 08:49:37 2038";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.ANSI_C_DATEFORMAT, Locale.US);
        //Don't set a time zone as this time format doesn't specify. Will have to go with our local timezone
        //expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }

    //Wed Aug 29 07:00:00 2007 GMT
    @Test
    public void testCookieExpiresAmazon() throws Exception{
        String name = "test";
        String value = "testvalue";
        String path = "/test/path";
        String domain = ".testdomain.com";
        String expires = "Wed Aug 29 07:00:00 2038 GMT";
        String headerValue = name + "=" + value + "; expires="+expires+"; Path=" + path + "; Domain=" + domain + "; Version=0";

        SimpleDateFormat expiryFormat = new SimpleDateFormat(CookieUtils.AMAZON_DATEFORMAT, Locale.US);
        //Don't set a time zone as this time format doesn't specify. Will have to go with our local timezone
        //expiryFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expiresDate = expiryFormat.parse(expires);
        int maxAgeTarget = (int)((expiresDate.getTime()-System.currentTimeMillis())/1000L);

        HttpCookie cookie = new HttpCookie(domain, path, headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking value property", value, cookie.getCookieValue());
        assertEquals("Expiry set", true, cookie.hasExpiry());
        assertTrue("Checking max-age property", 3 > Math.abs(cookie.getMaxAge()-maxAgeTarget)); //allow few secs difference
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
    }
    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CookieTest.class.getName());
}
