package com.l7tech.common.http;

import com.l7tech.test.BugId;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;

import javax.servlet.http.Cookie;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class CookieUtilsTest {
    @Test
    public void testEnsurePathDomain() {
        HttpCookie cookie1 = new HttpCookie("name", "valuea", 1, "/some", ".domain.com");
        String cookieOneHeader = CookieUtils.getSetCookieHeader(cookie1);
        assertFalse("Check modified domain", cookieOneHeader.equals(CookieUtils.replaceCookieDomainAndPath(cookieOneHeader, "b.bdomain.com", "/some/path")));
        assertFalse("Check modified path", cookieOneHeader.equals(CookieUtils.replaceCookieDomainAndPath(cookieOneHeader, "a.domain.com", "/otherpath")));
        assertEquals("Check unmodified path", cookieOneHeader, CookieUtils.replaceCookieDomainAndPath(cookieOneHeader, "a.domain.com", null));
    }

    @Test
    public void replaceCookieDomainAndPathNullCookieDomainPathMismatch() {
        assertEquals("name=valuea; Path=/gatewayPath; Domain=gatewayDomain", CookieUtils.replaceCookieDomainAndPath("name=valuea; Path=/originalPath", "gatewayDomain", "/gatewayPath"));
    }

    @Test
    public void replaceCookieDomainAndPathNullCookiePathDomainMismatch() {
        assertEquals("name=valuea; Domain=gatewayDomain; Path=/gatewayPath", CookieUtils.replaceCookieDomainAndPath("name=valuea; Domain=originalDomain", "gatewayDomain", "/gatewayPath"));
    }

    @Test
    public void replaceCookieDomainAndPathNullCookieDomainAndPath() {
        assertEquals("name=valuea; version=1", CookieUtils.replaceCookieDomainAndPath("name=valuea; version=1", "a.domain.com", "/otherPath"));
    }

    @Test
    public void replaceCookieDomainAndPathAlreadyMatch() {
        final String headerValue = "name=valuea; path=/originalPath; domain=originalDomain";
        assertEquals(headerValue, CookieUtils.replaceCookieDomainAndPath(headerValue, "originalDomain", "/originalPath"));
    }

    @Test
    public void replaceCookieDomainAndPathNullTargetDomain() throws Exception {
        HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/originalPath", "originalDomain");
        String cookieOneValue = CookieUtils.getSetCookieHeader(cookie1);
        HttpCookie modifiedCookie = new HttpCookie(CookieUtils.replaceCookieDomainAndPath(cookieOneValue, null, "/gatewayPath"));
        assertEquals("originalDomain", modifiedCookie.getDomain());
        assertEquals("/gatewayPath", modifiedCookie.getPath());
    }

    @Test
    public void replaceCookieDomainAndPathNullTargetPath() throws Exception {
        HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/originalPath", "originalDomain");
        String cookieOneValue = CookieUtils.getSetCookieHeader(cookie1);
        HttpCookie modifiedCookie = new HttpCookie(CookieUtils.replaceCookieDomainAndPath(cookieOneValue, "gatewayDomain", null));
        assertEquals("gatewayDomain", modifiedCookie.getDomain());
        assertEquals("/originalPath", modifiedCookie.getPath());
    }

    @Test
    public void replaceCookieDomainAndPathNullTargetDomainAndPath() throws Exception {
        HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/originalPath", "originalDomain");
        String cookieOneValue = CookieUtils.getSetCookieHeader(cookie1);
        HttpCookie modifiedCookie = new HttpCookie(CookieUtils.replaceCookieDomainAndPath(cookieOneValue, null, null));
        assertEquals("originalDomain", modifiedCookie.getDomain());
        assertEquals("/originalPath", modifiedCookie.getPath());
    }

    /**
     * This behaviour has existed since changeset 11919.
     */
    @BugId("SSG-8733")
    @Test
    public void replaceCookieDomainAndPathAlwaysReplacePathIfDomainIsReplaced() throws Exception {
        HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/", "galactica.l7tech.com");
        String cookieOneValue = CookieUtils.getSetCookieHeader(cookie1);
        HttpCookie modifiedCookie = new HttpCookie(CookieUtils.replaceCookieDomainAndPath(cookieOneValue, "gatewayDomain", "/gatewayPath"));
        assertEquals("gatewayDomain", modifiedCookie.getDomain());
        assertEquals("/gatewayPath", modifiedCookie.getPath());
    }

    @Test
    public void replaceCookieDomainAndPathDomainIsReplacedButPathIsNull() {
        assertEquals("foo=bar; Domain=gatewayDomain", CookieUtils.replaceCookieDomainAndPath("foo=bar; Domain=localhost", "gatewayDomain", null));
    }

    @Test
    public void replaceCookieDomainAndPathSubDomain() {
        final String headerValue = "name=valuea; path=/originalPath; domain=originalDomain";
        assertEquals(headerValue, CookieUtils.replaceCookieDomainAndPath(headerValue, "sub.originalDomain", "/originalPath"));
    }

    @Test
    public void replaceCookieDomainAndPathSubPath() {
        final String headerValue = "name=valuea; path=/originalPath; domain=originalDomain";
        assertEquals(headerValue, CookieUtils.replaceCookieDomainAndPath(headerValue, "originalDomain", "/originalPath/sub"));
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

        HttpCookie cookie3 = new HttpCookie("name3", "\"value with spaces\"", 1, "/", ".layer7tech.com");
        List cookieList3 = new ArrayList();
        cookieList3.add(cookie1);
        cookieList3.add(cookie2);
        cookieList3.add(cookie3);
        String header3 = CookieUtils.getCookieHeader(cookieList3);
        assertEquals("Cookie header", "name=value; name2=value2; name3=\"value with spaces\"", header3);
    }

    @Test
    public void getCookieHeaderDoNotQuoteSpecialCharacters() {
        final Set<HttpCookie> cookies = new HashSet<>();
        cookies.add(new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true));
        cookies.add(new HttpCookie("specialChar", "DQAAAK…Eaem_vYg", 1, "/", "localhost", -1, false, null, false));
        assertEquals("specialChar=DQAAAK…Eaem_vYg; foo=bar", CookieUtils.getCookieHeader(cookies));
    }

    @Test
    public void getV0CookieHeaderPart() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        assertEquals("foo=bar", CookieUtils.getV0CookieHeaderPart(cookie));
    }

    @Test
    public void getV0CookieHeaderPartDoNotQuoteSpecialCharacters() {
        final HttpCookie cookie = new HttpCookie("specialChar", "DQAAAK…Eaem_vYg", 1, "/", "localhost", -1, false, null, false);
        assertEquals("specialChar=DQAAAK…Eaem_vYg", CookieUtils.getV0CookieHeaderPart(cookie));
    }

    @Test
    public void getSetCookieHeader() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure; HttpOnly", CookieUtils.getSetCookieHeader(cookie));
    }

    @Test
    public void getSetCookieHeaderMinimal() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false);
        assertEquals("foo=bar", CookieUtils.getSetCookieHeader(cookie));
    }

    @Test
    public void getFullValueFromParsedCookie() throws Exception {
        assertEquals("1=a", new HttpCookie("1=a").getFullValue());
        assertEquals("2=b", new HttpCookie("localhost", "/", "2=b").getFullValue());
        assertEquals("3=c", new HttpCookie(new URL("http", "localhost", 8080, "foo"), "3=c").getFullValue());
    }

    @Test
    public void getFullValueFromNonParsedCookie() throws Exception {
        assertNull(new HttpCookie("1", "a", 1, "/", "localhost", 60, false, "test", false).getFullValue());
        assertNull(new HttpCookie("2", "b", 1, "/", "localhost").getFullValue());
        assertNull(new HttpCookie(new HttpCookie("1=a"), "localhost", "/").getFullValue()); // copied
    }

    @Test
    public void shouldPreserveCookieDomainAndPath() throws Exception {
        String testValue = "foo=\"abc\" ;domain=\".com\";path=\\; secure; Expires=Wed 01-01-01";
        String newDomain = "domain=.mydomain.com";
        String newPath = "Path=\"/path\"";
        String expected = "foo=\"abc\" ;domain=.mydomain.com;Path=\"/path\"; secure; Expires=Wed 01-01-01";
        assertEquals("Compare replaced cookie", testValue, CookieUtils.addCookieDomainAndPath(testValue, newDomain, newPath));
    }

    @Test
    public void shouldAddDomainAndPath() throws Exception {
        String newDomain = "Domain=.mydomain.com";
        String newPath = "Path=\"/path\"";
        String testValue = "foo=bar";
        String expected = "foo=bar; Domain=.mydomain.com; Path=\"/path\"";
        assertEquals("Compare replaced cookie", expected, CookieUtils.addCookieDomainAndPath(testValue, newDomain, newPath));
    }

    @Test
    public void shouldAddDomainOnly() throws Exception {
        String testValue = "foo=\"abc\" ;path=\\; secure; Expires=Wed 01-01-01";
        String newDomain = "Domain=.mydomain.com";
        String expected = "foo=\"abc\" ;path=\\; secure; Expires=Wed 01-01-01; Domain=.mydomain.com";
        assertEquals("Compare replaced cookie", expected, CookieUtils.addCookieDomainAndPath(testValue, newDomain, null));
    }

    @Test
    public void shouldAddCookiePathOnly() throws Exception {
        String newPath = "Path=\"/path\"";
        String testValue = "foo=\"%5%X412345456767\"; Domain=\".domain\";Secure";
        String expected = "foo=\"%5%X412345456767\"; Domain=\".domain\";Secure; Path=\"/path\"";
        assertEquals("Compare replaced cookie", expected, CookieUtils.addCookieDomainAndPath(testValue, null, newPath));
    }

    @Test
    public void testCookieParser() throws Exception {
        String testValue = "foo=\"abc;domain=.something;  \" ;Domain=\"blah....\"   ;Path=\"\\\"; Secure; Expires=Wed, 28-May-2014 01:58:59 GMT; bar=cdfe; Path=/;Max-Age=something;httpOnly;secure ;mycookie = \"value  \"";
        List<String> actual = CookieUtils.splitCookieHeader(testValue);
        assertEquals("Compare first cookie", "foo=\"abc;domain=.something;  \"; Domain=\"blah....\"; Path=\"\\\"; Secure; Expires=Wed, 28-May-2014 01:58:59 GMT", actual.get(0));
        assertEquals("Compare second cookie", "bar=cdfe; Path=/; Max-Age=something; httpOnly; secure", actual.get(1));
        assertEquals("Compare thirds cookie", "mycookie = \"value  \"", actual.get(2));
    }

    @Test
    public void testCookieParserWhenCookieValueIsNull() throws Exception {
        List<String> actual = CookieUtils.splitCookieHeader(null);
        assertTrue(actual.size() == 0);
    }

    @Test
    public void addCookieDomainIfNotExists() throws Exception {
        String testValue = "foo=\"abc;Domain=.something;  \"    ; Path=\"\\\"; Secure; Expires=Wed, 28-May-2014 01:58:59 GMT; bar=cdfe; Domain=\"blah....\"; Path=/; Max-Age=something; httpOnly; Secure";
        String expected = "foo=\"abc;Domain=.something;  \"; Path=\"\\\"; Secure; Expires=Wed, 28-May-2014 01:58:59 GMT; Domain=.mydomain.com; bar=cdfe; Domain=\"blah....\"; Path=/; Max-Age=something; httpOnly; Secure";
        String newDomain = CookieUtils.DOMAIN + CookieUtils.EQUALS + ".mydomain.com";
        String newPath = CookieUtils.PATH + CookieUtils.EQUALS + "/path";
        StringBuffer modifiedCookie = new StringBuffer();
        List<String> cookies = CookieUtils.splitCookieHeader(testValue);
        for (int i = 0; i < cookies.size(); i++) {
            if (i > 0) modifiedCookie.append(CookieUtils.ATTRIBUTE_DELIMITER);
            modifiedCookie.append(CookieUtils.addCookieDomainAndPath(cookies.get(i), newDomain, newPath));
        }
        String actual = modifiedCookie.toString();
        assertEquals("Compare Set-Cookie header value", expected, actual);
    }

    @Test
    public void getNameAndValue() {
        assertEquals("foo=bar", CookieUtils.getNameAndValue("foo=bar; domain=localhost; path=/; version=1; comment=test; max-age=60; secure; httpOnly"));
        assertEquals("foo=bar", CookieUtils.getNameAndValue("foo=bar;domain=localhost;path=/;version=1;comment=test;max-age=60;secure;httpOnly"));
        assertEquals("foo=bar", CookieUtils.getNameAndValue("foo=bar; secure; httpOnly; domain=localhost; path=/; version=1; comment=test; max-age=60"));
        assertEquals("foo=bar", CookieUtils.getNameAndValue("foo=bar"));
        assertEquals("foo=bar", CookieUtils.getNameAndValue("foo=bar; $Domain=localhost; $Path=/; $Version=1"));
    }

    @Test
    public void getNameAndValueVersionFirst() {
        assertEquals("foo=bar", CookieUtils.getNameAndValue("$Version=1; foo=bar; $Domain=localhost; $Path=/"));
    }

    @Test
    public void getNameAndValueNoValue() {
        assertNull(CookieUtils.getNameAndValue("foo"));
    }
}
