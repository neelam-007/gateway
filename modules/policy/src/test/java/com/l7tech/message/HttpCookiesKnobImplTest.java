package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class HttpCookiesKnobImplTest {
    private HttpCookiesKnob cookiesKnob;
    private HeadersKnob headersKnob;

    @Before
    public void setup() {
        headersKnob = new HeadersKnobSupport();
        cookiesKnob = new HttpCookiesKnobImpl(headersKnob);
    }

    @Test
    public void addCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test");
        cookiesKnob.addCookie(cookie);
        assertEquals(1, cookiesKnob.getCookies().size());
        assertEquals(1, headersKnob.getHeaderNames().length);
        assertEquals(1, headersKnob.getHeaderValues("Set-Cookie").length);
        assertEquals(cookie, cookiesKnob.getCookies().iterator().next());
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60", headersKnob.getHeaderValues("Set-Cookie")[0]);
    }

    @Test
    public void addCookieDifferentDomain() {
        final HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/", "different", 60, false, "test");
        cookiesKnob.addCookie(cookie1);
        final HttpCookie cookie2 = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test");
        cookiesKnob.addCookie(cookie2);
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(cookies.contains(cookie1));
        assertTrue(cookies.contains(cookie2));
        assertEquals(1, headersKnob.getHeaderNames().length);
        assertEquals(2, headersKnob.getHeaderValues("Set-Cookie").length);
        final List<String> setCookieValues = Arrays.asList(headersKnob.getHeaderValues("Set-Cookie"));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60"));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=different; Path=/; Comment=test; Max-Age=60"));
    }

    @Test
    public void addCookieDifferentPath() {
        final HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/different", "localhost", 60, false, "test");
        cookiesKnob.addCookie(cookie1);
        final HttpCookie cookie2 = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test");
        cookiesKnob.addCookie(cookie2);
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(cookies.contains(cookie1));
        assertTrue(cookies.contains(cookie2));
        assertEquals(1, headersKnob.getHeaderNames().length);
        assertEquals(2, headersKnob.getHeaderValues("Set-Cookie").length);
        final List<String> setCookieValues = Arrays.asList(headersKnob.getHeaderValues("Set-Cookie"));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60"));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=localhost; Path=/different; Comment=test; Max-Age=60"));
    }

    @Test
    public void addCookieSameNameDomainAndPath() {
        final HttpCookie cookie1 = new HttpCookie("foo", "existing", 1, "/", "localhost", 60, false, "existing");
        cookiesKnob.addCookie(cookie1);
        final HttpCookie cookie2 = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test");
        cookiesKnob.addCookie(cookie2);
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals(cookie2, cookies.iterator().next());
        assertEquals(1, headersKnob.getHeaderNames().length);
        assertEquals(1, headersKnob.getHeaderValues("Set-Cookie").length);
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure", headersKnob.getHeaderValues("Set-Cookie")[0]);
    }

    @Test
    public void getCookiesFromSetCookieHeaderV0AllAttributes() {
        headersKnob.addHeader("Set-Cookie", "1=allAttributesV0; expires=Fri, 20-Dec-2013 00:00:00 PST; domain=netscape; path=/netscape; secure;");
        headersKnob.addHeader("Set-Cookie", "2=allAttributesV0; expires=Friday, 20-Dec-13 00:00:00 PST; domain=netscape; path=/netscape; secure;");
        headersKnob.addHeader("Set-Cookie", "3=allAttributesV0; expires=Fri, 20 Dec 2013 00:00:00 PST; domain=netscape; path=/netscape; secure;");
        headersKnob.addHeader("Set-Cookie", "4=allAttributesV0; expires=Fri, 20 Dec 13 00:00:00 PST; domain=netscape; path=/netscape; secure;");
        headersKnob.addHeader("Set-Cookie", "5=allAttributesV0; expires=Fri Dec 20 00:00:00 2013; domain=netscape; path=/netscape; secure;");
        headersKnob.addHeader("Set-Cookie", "6=allAttributesV0; expires=Fri Dec 20 00:00:00 2013 PST; domain=netscape; path=/netscape; secure;");
        headersKnob.addHeader("Set-Cookie", "7=allAttributesV0NoWhitespace;expires=Fri Dec 20 00:00:00 2013 PST;domain=netscape;path=/netscape;secure;");

        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertTrue(containsCookie(cookies, new HttpCookie("1", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null), false));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null), false));
        assertTrue(containsCookie(cookies, new HttpCookie("3", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null), false));
        assertTrue(containsCookie(cookies, new HttpCookie("4", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null), false));
        assertTrue(containsCookie(cookies, new HttpCookie("5", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null), false));
        assertTrue(containsCookie(cookies, new HttpCookie("6", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null), false));
        assertTrue(containsCookie(cookies, new HttpCookie("7", "allAttributesV0NoWhitespace", 0, "/netscape", "netscape", -1, true, null), false));
        for (final HttpCookie cookie : cookies) {
            if (cookie.getMaxAge() == -1) {
                fail("Cookie with name " + cookie.getCookieName() + " does not have a supported expires date format.");
            }
        }
    }

    @Test
    public void getCookiesFromSetCookieHeaderV0MinimalAttributes() {
        headersKnob.addHeader("Set-Cookie", "foo=bar");
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("foo", "bar", 0, null, null, -1, false, null)));
    }

    @Test
    public void getCookiesFromSetCookieHeaderV1() {
        headersKnob.addHeader("Set-Cookie", "1=allAttributesV1; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure");
        headersKnob.addHeader("Set-Cookie", "2=minimumV1; Version=1");
        headersKnob.addHeader("Set-Cookie", "3=noWhiteSpace;Version=1;Domain=localhost;Path=/;Comment=test;Max-Age=60;Secure");
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(3, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "allAttributesV1", 1, "/", "localhost", 60, true, "test")));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "minimumV1", 1, null, null, -1, false, null)));
        assertTrue(containsCookie(cookies, new HttpCookie("3", "noWhiteSpace", 1, "/", "localhost", 60, true, "test")));
    }

    @Test
    public void getCookiesFromInvalidSetCookieHeader() {
        headersKnob.addHeader("Set-Cookie", "invalid");
        assertTrue(cookiesKnob.getCookies().isEmpty());
    }

    @Test
    public void getCookiesFromCookieHeaderV0() {
        headersKnob.addHeader("Cookie", "foo=bar");
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("foo", "bar", 0, null, null, -1, false, null)));
    }

    @Test
    public void getCookiesFromCookieHeaderV1() {
        headersKnob.addHeader("Cookie", "1=allAttributesV1; $Version=1; $Path=/; $Domain=localhost");
        headersKnob.addHeader("Cookie", "2=minimumV1; $Version=1");
        headersKnob.addHeader("Cookie", "3=allAttributesV1NoWhitespace;$Version=1;$Path=/;$Domain=localhost");
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(3, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "allAttributesV1", 1, "/", "localhost", -1, false, null)));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "minimumV1", 1, null, null, -1, false, null)));
        assertTrue(containsCookie(cookies, new HttpCookie("3", "allAttributesV1NoWhitespace", 1, "/", "localhost", -1, false, null)));
    }

    @Test
    public void getCookiesFromInvalidCookieHeader() {
        headersKnob.addHeader("Cookie", "invalid");
        assertTrue(cookiesKnob.getCookies().isEmpty());
    }

    @Test
    public void deleteCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test");
        cookiesKnob.addCookie(cookie);
        assertEquals(1, cookiesKnob.getCookies().size());
        cookiesKnob.deleteCookie(cookie);
        assertTrue(cookiesKnob.getCookies().isEmpty());
    }

    @Test
    public void deleteCookieNameMismatch() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test");
        cookiesKnob.addCookie(cookie);
        assertEquals(1, cookiesKnob.getCookies().size());
        cookiesKnob.deleteCookie(new HttpCookie("nomatch", "bar", 1, "/", "localhost", 60, true, "test"));
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals("foo", cookies.iterator().next().getCookieName());
    }

    @Test
    public void deleteCookieDomainMismatch() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test");
        cookiesKnob.addCookie(cookie);
        assertEquals(1, cookiesKnob.getCookies().size());
        cookiesKnob.deleteCookie(new HttpCookie("foo", "bar", 1, "/", "mismatch", 60, true, "test"));
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals("localhost", cookies.iterator().next().getDomain());
    }

    @Test
    public void deleteCookiePathMismatch() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test");
        cookiesKnob.addCookie(cookie);
        assertEquals(1, cookiesKnob.getCookies().size());
        cookiesKnob.deleteCookie(new HttpCookie("foo", "bar", 1, "/mismatch", "localhost", 60, true, "test"));
        final Set<HttpCookie> cookies = cookiesKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals("/", cookies.iterator().next().getPath());
    }

    @Test
    public void deleteCookieNotSetCookieHeader() {
        headersKnob.addHeader("Cookie", "foo=bar");
        assertEquals(1, cookiesKnob.getCookies().size());
        cookiesKnob.deleteCookie(new HttpCookie("foo", "bar", 0, null, null, -1, false, null));
        assertTrue(cookiesKnob.getCookies().isEmpty());
    }

    @Test
    public void deleteCookieInvalidCookieHeaderIgnored() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test");
        cookiesKnob.addCookie(cookie);
        // having an invalid cookie header should not affect the result
        headersKnob.addHeader("Set-Cookie", "invalidCookieHeader");
        assertEquals(1, cookiesKnob.getCookies().size());
        cookiesKnob.deleteCookie(cookie);
        assertTrue(cookiesKnob.getCookies().isEmpty());
    }

    private boolean containsCookie(final Set<HttpCookie> toSearch, final HttpCookie toFind) {
        return containsCookie(toSearch, toFind, true);
    }

    private boolean containsCookie(final Set<HttpCookie> toSearch, final HttpCookie toFind, final boolean compareMaxAge) {
        boolean found = false;
        for (final HttpCookie cookie : toSearch) {
            if (cookieAttributesEqual(cookie, toFind, compareMaxAge)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private boolean cookieAttributesEqual(final HttpCookie cookie1, final HttpCookie cookie2, final boolean compareMaxAge) {
        return ObjectUtils.equals(cookie1.getCookieName(), cookie2.getCookieName()) && ObjectUtils.equals(cookie1.getCookieValue(), cookie2.getCookieValue()) &&
                cookie1.getVersion() == cookie2.getVersion() && ObjectUtils.equals(cookie1.getDomain(), cookie2.getDomain()) &&
                ObjectUtils.equals(cookie1.getPath(), cookie2.getPath()) && ObjectUtils.equals(cookie1.getComment(), cookie2.getComment()) &&
                (compareMaxAge ? cookie1.getMaxAge() == cookie2.getMaxAge() : true);
    }
}
