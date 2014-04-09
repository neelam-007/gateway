package com.l7tech.common.http;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;

import javax.servlet.http.Cookie;

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
}
