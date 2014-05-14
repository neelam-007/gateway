package com.l7tech.common.http;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;

import javax.servlet.http.Cookie;
import java.util.*;

import static org.junit.Assert.*;

public class CookieUtilsTest {
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
    public void ensureValidForDomainAndPathNullCookieDomainAndPath() {
        HttpCookie cookie1 = new HttpCookie("name", "valuea", 1, null, null);
        HttpCookie cookie2 = CookieUtils.ensureValidForDomainAndPath(cookie1, "a.domain.com", null);
        assertEquals("Check valid", cookie1, cookie2);
    }

    @Test
    public void ensureValidForDomainAndPathNullTargetDomain() {
        final HttpCookie cookie = CookieUtils.ensureValidForDomainAndPath(new HttpCookie("foo", "bar", 1, "/originalPath", "originalDomain"), null, "/gatewayPath");
        assertEquals("originalDomain", cookie.getDomain());
        assertEquals("/gatewayPath", cookie.getPath());
    }

    @Test
    public void ensureValidForDomainAndPathNullTargetPath() {
        final HttpCookie cookie = CookieUtils.ensureValidForDomainAndPath(new HttpCookie("foo", "bar", 1, "/originalPath", "originalDomain"), "gatewayDomain", null);
        assertEquals("gatewayDomain", cookie.getDomain());
        assertEquals("/originalPath", cookie.getPath());
    }

    @Test
    public void ensureValidForDomainAndPathNullTargetDomainAndPath() {
        final HttpCookie cookie = CookieUtils.ensureValidForDomainAndPath(new HttpCookie("foo", "bar", 1, "/originalPath", "originalDomain"), null, null);
        assertEquals("originalDomain", cookie.getDomain());
        assertEquals("/originalPath", cookie.getPath());
    }

    /**
     * httpclient 4.2.5 doesn't support httpOnly.
     */
    @Test
    public void fromHttpClientCookieNotHttpOnly() {
        final BasicClientCookie clientCookie = new BasicClientCookie("foo", "bar");
        assertFalse(CookieUtils.fromHttpClientCookie(clientCookie, true).isHttpOnly());
    }

    @Test
    public void toServletCookieHttpOnly() {
        final HttpCookie httpOnlyCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        assertTrue(CookieUtils.toServletCookie(httpOnlyCookie).isHttpOnly());

        final HttpCookie notHttpOnlyCookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        assertFalse(CookieUtils.toServletCookie(notHttpOnlyCookie).isHttpOnly());
    }

    @Test
    public void fromServletCookieHttpOnly() {
        final Cookie httpOnlyCookie = new Cookie("foo", "bar");
        httpOnlyCookie.setHttpOnly(true);
        assertTrue(CookieUtils.fromServletCookie(httpOnlyCookie, true).isHttpOnly());

        assertFalse(CookieUtils.fromServletCookie(new Cookie("foo", "bar"), true).isHttpOnly());
    }

    @Test
    public void fromServletCookieNotNewCookie() {
        final Cookie servletCookie = new Cookie("foo", "bar");
        servletCookie.setHttpOnly(true);
        servletCookie.setSecure(true);
        servletCookie.setMaxAge(60);
        servletCookie.setComment("test");
        final HttpCookie cookie = CookieUtils.fromServletCookie(servletCookie, false);
        assertFalse(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals(-1, cookie.getMaxAge());
        assertNull(cookie.getComment());
    }

    @Test
    public void isTokenFalse() {
        // less than or equal to unicode 32
        for (int i = 0; i <= 32; i++) {
            final char c = (char) i;
            assertFalse(CookieUtils.isToken(String.valueOf(c)));
        }

        // greater than or equal to unicode 127
        char c = 127;
        assertFalse(CookieUtils.isToken(String.valueOf(c)));
        c = 128;
        assertFalse(CookieUtils.isToken(String.valueOf(c)));

        assertFalse(CookieUtils.isToken(","));
        assertFalse(CookieUtils.isToken(";"));
        assertFalse(CookieUtils.isToken(" "));
    }

    @Test
    public void isTokenTrue() {
        // greater than 33 and less than 127
        for (int i = 33; i < 127; i++) {
            if (i != 44 && i != 59) { // , and ; are not allowed
                final char c = (char) i;
                assertTrue(CookieUtils.isToken(String.valueOf(c)));
            }
        }
    }

    @Test
    public void isTokenNull() {
        assertTrue(CookieUtils.isToken(null));
    }

    @Test
    public void isTokenEmpty() {
        assertTrue(CookieUtils.isToken(""));
    }

    @Test
    public void testCookieHeader() {
        HttpCookie cookie1 = new HttpCookie("name", "value", 1, "/", ".layer7tech.com");
        String header1 = CookieUtils.getCookieHeader(Collections.singletonList(cookie1));
        assertEquals("Cookie header", "name=value", header1);

        HttpCookie cookie2 = new HttpCookie("name2", "value2", 1, "/", ".layer7tech.com");
        List cookieList2 = new ArrayList();
        cookieList2.add(cookie1);
        cookieList2.add(cookie2);
        String header2 = CookieUtils.getCookieHeader(cookieList2);
        assertEquals("Cookie header", "name=value; name2=value2", header2);

        HttpCookie cookie3 = new HttpCookie("name3", "value with \"spaces", 1, "/", ".layer7tech.com");
        List cookieList3 = new ArrayList();
        cookieList3.add(cookie1);
        cookieList3.add(cookie2);
        cookieList3.add(cookie3);
        String header3 = CookieUtils.getCookieHeader(cookieList3);
        assertEquals("Cookie header", "name=value; name2=value2; name3=\"value with \\\"spaces\"", header3);
    }

    @Test
    public void testQuoting() {
        HttpCookie cookie1 = new HttpCookie("name", "value with spaces", 1, "/some", ".domain.com");
        String header1 = CookieUtils.getV0CookieHeaderPart(cookie1);
        assertEquals("Correctly quoted", "name=\"value with spaces\"", header1);

        HttpCookie cookie2 = new HttpCookie("name", "value_with_;", 1, "/some", ".domain.com");
        String header2 = CookieUtils.getV0CookieHeaderPart(cookie2);
        assertEquals("Correctly quoted", "name=\"value_with_;\"", header2);

        HttpCookie cookie3 = new HttpCookie("name", "value_without_spaces", 1, "/some", ".domain.com");
        String header3 = CookieUtils.getV0CookieHeaderPart(cookie3);
        assertEquals("Correctly quoted", "name=value_without_spaces", header3);
    }

    @Test
    public void getCookieHeaderQuoteSpecialCharacters() {
        final Set<HttpCookie> cookies = new HashSet<>();
        cookies.add(new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true));
        cookies.add(new HttpCookie("specialChar", "DQAAAK…Eaem_vYg", 1, "/", "localhost", -1, false, null, false));
        assertEquals("specialChar=\"DQAAAK…Eaem_vYg\"; foo=bar", CookieUtils.getCookieHeader(cookies));
    }

    @Test
    public void getCookieHeaderDoNotQuoteSpecialCharacters() {
        final Set<HttpCookie> cookies = new HashSet<>();
        cookies.add(new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true));
        cookies.add(new HttpCookie("specialChar", "DQAAAK…Eaem_vYg", 1, "/", "localhost", -1, false, null, false));
        assertEquals("specialChar=DQAAAK…Eaem_vYg; foo=bar", CookieUtils.getCookieHeader(cookies, false));
    }

    @Test
    public void getV0CookieHeaderPart() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        assertEquals("foo=bar", CookieUtils.getV0CookieHeaderPart(cookie));
    }

    @Test
    public void getV0CookieHeaderPartQuoteSpecialCharacters() {
        final HttpCookie cookie = new HttpCookie("specialChar", "DQAAAK…Eaem_vYg", 1, "/", "localhost", -1, false, null, false);
        assertEquals("specialChar=\"DQAAAK…Eaem_vYg\"", CookieUtils.getV0CookieHeaderPart(cookie));
    }

    @Test
    public void getV0CookieHeaderPartDoNotQuoteSpecialCharacters() {
        final HttpCookie cookie = new HttpCookie("specialChar", "DQAAAK…Eaem_vYg", 1, "/", "localhost", -1, false, null, false);
        assertEquals("specialChar=DQAAAK…Eaem_vYg", CookieUtils.getV0CookieHeaderPart(cookie, false));
    }

    @Test
    public void getSetCookieHeader() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure; HttpOnly", CookieUtils.getSetCookieHeader(cookie));
    }

    @Test
    public void getSetCookieHeaderMinimal() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false);
        assertEquals("foo=bar; Version=0", CookieUtils.getSetCookieHeader(cookie));
    }
}
