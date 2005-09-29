package com.l7tech.common.http;

import junit.framework.TestCase;

import java.util.logging.Logger;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
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
public class CookieTest extends TestCase {


    //- PUBLIC

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

    public void testSimpleSetCookieHeader() throws Exception {
        String name = "test";
        String value = "testvalue";
        String path = "/apath/blah";
        String domain = "www.testdomain.com";
        String headerValue = name + "=" + value;

        HttpCookie cookie = new HttpCookie(new URL("http://"+domain+path+"/apage.html"), headerValue);

        assertEquals("Cookie should be new", true, cookie.isNew());
        assertEquals("Checking name property", name, cookie.getCookieName());
        assertEquals("Checking path property", path, cookie.getPath());
        assertEquals("Checking domain property", domain, cookie.getDomain());
        assertEquals("Should not be secure", false, cookie.isSecure());
        assertEquals("No expiry set", false, cookie.hasExpiry());
    }

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

    public void testFullV1SetCookieHeader() {
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


    public void testEnsurePathDomain() {
        HttpCookie cookie1 = new HttpCookie("name", "valuea", 1, "/some", ".domain.com");

        HttpCookie cookie2 = CookieUtils.ensureValidForDomainAndPath(cookie1, "a.domain.com", "/some/path");
        assertEquals("Check valid", cookie1, cookie2);

        HttpCookie cookie3 = CookieUtils.ensureValidForDomainAndPath(cookie1, "b.bdomain.com", "/some/path");
        assertFalse("Check modified domain", cookie1.equals(cookie3));

        HttpCookie cookie4 = CookieUtils.ensureValidForDomainAndPath(cookie1, "a.domain.com", "/otherpath");
        assertFalse("Check modified path", cookie1.equals(cookie4));
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CookieTest.class.getName());
}
