package com.l7tech.message;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static org.junit.Assert.*;

public class HttpCookiesKnobImplTest {
    private HttpCookiesKnob setCookieKnob;
    private HttpCookiesKnob cookieKnob;
    private HeadersKnob headersKnob;

    @Before
    public void setup() {
        headersKnob = new HeadersKnobSupport();
        setCookieKnob = new HttpCookiesKnobImpl(headersKnob, HttpConstants.HEADER_SET_COOKIE);
        cookieKnob = new HttpCookiesKnobImpl(headersKnob, HttpConstants.HEADER_COOKIE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNotCookieOrSetCookie() {
        new HttpCookiesKnobImpl(headersKnob, "NotCookieOrSetCookie");
    }

    @Test
    public void addSetCookieHttpOnlyAndSecure() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", true);
        setCookieKnob.addCookie(cookie);
        assertEquals(1, setCookieKnob.getCookies().size());
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP).length);
        assertEquals(1, headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP).length);
        assertEquals(cookie, setCookieKnob.getCookies().iterator().next());
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure; HttpOnly",
                headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP)[0]);
        assertTrue(cookieKnob.getCookies().isEmpty());
    }

    @Test
    public void addSetCookieNotHttpOnlyOrSecure() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false);
        setCookieKnob.addCookie(cookie);
        assertEquals(1, setCookieKnob.getCookies().size());
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP).length);
        assertEquals(1, headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP).length);
        assertEquals(cookie, setCookieKnob.getCookies().iterator().next());
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60",
                headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void addSetCookieDifferentDomain() {
        final HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/", "different", 60, false, "test", false);
        setCookieKnob.addCookie(cookie1);
        final HttpCookie cookie2 = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false);
        setCookieKnob.addCookie(cookie2);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(cookies.contains(cookie1));
        assertTrue(cookies.contains(cookie2));
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP).length);
        assertEquals(2, headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP).length);
        final List<String> setCookieValues = Arrays.asList(headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60"));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=different; Path=/; Comment=test; Max-Age=60"));
    }

    @Test
    public void addSetCookieDifferentPath() {
        final HttpCookie cookie1 = new HttpCookie("foo", "bar", 1, "/different", "localhost", 60, false, "test", false);
        setCookieKnob.addCookie(cookie1);
        final HttpCookie cookie2 = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false);
        setCookieKnob.addCookie(cookie2);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(cookies.contains(cookie1));
        assertTrue(cookies.contains(cookie2));
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP).length);
        assertEquals(2, headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP).length);
        final List<String> setCookieValues = Arrays.asList(headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60"));
        assertTrue(setCookieValues.contains("foo=bar; Version=1; Domain=localhost; Path=/different; Comment=test; Max-Age=60"));
    }

    @Test
    public void addSetCookieSameNameDomainAndPath() {
        final HttpCookie cookie1 = new HttpCookie("foo", "existing", 1, "/", "localhost", 60, false, "existing", false);
        setCookieKnob.addCookie(cookie1);
        final HttpCookie cookie2 = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie2);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals(cookie2, cookies.iterator().next());
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP).length);
        assertEquals(1, headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP).length);
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure",
                headersKnob.getHeaderValues("Set-Cookie", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void addCookie() {
        cookieKnob.addCookie(new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test", false));
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP).length);
        assertEquals(1, headersKnob.getHeaderValues("Cookie", HEADER_TYPE_HTTP).length);
        assertEquals("foo=bar; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60",
                headersKnob.getHeaderValues("Cookie", HEADER_TYPE_HTTP)[0]);
        assertTrue(setCookieKnob.getCookies().isEmpty());
    }

    @Test
    public void getCookiesFromSetCookieHeaderV0AllAttributes() {
        headersKnob.addHeader("Set-Cookie", "1=allAttributesV0; expires=Fri, 20-Dec-2013 00:00:00 PST; domain=netscape; path=/netscape; secure;", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "2=allAttributesV0; expires=Friday, 20-Dec-13 00:00:00 PST; domain=netscape; path=/netscape; secure;", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "3=allAttributesV0; expires=Fri, 20 Dec 2013 00:00:00 PST; domain=netscape; path=/netscape; secure;", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "4=allAttributesV0; expires=Fri, 20 Dec 13 00:00:00 PST; domain=netscape; path=/netscape; secure;", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "5=allAttributesV0; expires=Fri Dec 20 00:00:00 2013; domain=netscape; path=/netscape; secure;", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "6=allAttributesV0; expires=Fri Dec 20 00:00:00 2013 PST; domain=netscape; path=/netscape; secure;", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "7=allAttributesV0NoWhitespace;expires=Fri Dec 20 00:00:00 2013 PST;domain=netscape;path=/netscape;secure;", HEADER_TYPE_HTTP);

        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertTrue(containsCookie(cookies, new HttpCookie("1", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null, false), false));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null, false), false));
        assertTrue(containsCookie(cookies, new HttpCookie("3", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null, false), false));
        assertTrue(containsCookie(cookies, new HttpCookie("4", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null, false), false));
        assertTrue(containsCookie(cookies, new HttpCookie("5", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null, false), false));
        assertTrue(containsCookie(cookies, new HttpCookie("6", "allAttributesV0", 0, "/netscape", "netscape", -1, true, null, false), false));
        assertTrue(containsCookie(cookies, new HttpCookie("7", "allAttributesV0NoWhitespace", 0, "/netscape", "netscape", -1, true, null, false), false));
        for (final HttpCookie cookie : cookies) {
            if (cookie.getMaxAge() == -1) {
                fail("Cookie with name " + cookie.getCookieName() + " does not have a supported expires date format.");
            }
        }
    }

    @Test
    public void getCookiesFromSetCookieHeaderV0MinimalAttributes() {
        headersKnob.addHeader("Set-Cookie", "foo=bar", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false)));
    }

    @Test
    public void getCookiesFromSetCookieHeaderV1() {
        headersKnob.addHeader("Set-Cookie", "1=allAttributesV1; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure; HttpOnly", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "2=minimumV1; Version=1", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "3=noWhiteSpace;Version=1;Domain=localhost;Path=/;Comment=test;Max-Age=60;Secure;HttpOnly", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(3, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "allAttributesV1", 1, "/", "localhost", 60, true, "test", true)));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "minimumV1", 1, null, null, -1, false, null, false)));
        assertTrue(containsCookie(cookies, new HttpCookie("3", "noWhiteSpace", 1, "/", "localhost", 60, true, "test", true)));
    }

    @Test
    public void getCookiesFromInvalidSetCookieHeader() {
        headersKnob.addHeader("Set-Cookie", "invalid", HEADER_TYPE_HTTP);
        assertTrue(setCookieKnob.getCookies().isEmpty());
    }

    @Test
    public void getCookiesFromSetCookieHeaderCommentContainsWhitespace() {
        headersKnob.addHeader("Set-Cookie", "foo=bar; Version=1; Comment=test comment", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("foo", "bar", 1, null, null, -1, false, "test comment", false)));
    }

    @Test
    public void getCookiesFromCookieHeaderV0() {
        headersKnob.addHeader("Cookie", "foo=bar", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false)));
    }

    @Test
    public void getCookiesFromCookieHeaderV1() {
        headersKnob.addHeader("Cookie", "1=allAttributesV1; $Version=1; $Path=/; $Domain=localhost", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Cookie", "2=minimumV1; $Version=1", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Cookie", "3=allAttributesV1NoWhitespace;$Version=1;$Path=/;$Domain=localhost", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(3, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "allAttributesV1", 1, "/", "localhost", -1, false, null, false)));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "minimumV1", 1, null, null, -1, false, null, false)));
        assertTrue(containsCookie(cookies, new HttpCookie("3", "allAttributesV1NoWhitespace", 1, "/", "localhost", -1, false, null, false)));
    }

    @Test
    public void getCookiesFromInvalidCookieHeader() {
        headersKnob.addHeader("Cookie", "invalid", HEADER_TYPE_HTTP);
        assertTrue(cookieKnob.getCookies().isEmpty());
    }

    @Test
    public void getCookiesMultipleInCookieHeaderV0() {
        headersKnob.addHeader("Cookie", "1=one; 2=two", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "one", 0, null, null, -1, false, null, false)));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "two", 0, null, null, -1, false, null, false)));
    }

    @Test
    public void getCookiesMultipleInCookieHeaderV1() {
        headersKnob.addHeader("Cookie", "1=one; $Version=1; 2=two; $Version=1", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "one", 1, null, null, -1, false, null, false)));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "two", 1, null, null, -1, false, null, false)));
    }

    @Test
    public void getCookiesMultipleInCookieHeaderV1VersionFirst() {
        headersKnob.addHeader("Cookie", "$Version=1; 1=one; $Version=1; 2=two", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(2, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "one", 1, null, null, -1, false, null, false)));
        assertTrue(containsCookie(cookies, new HttpCookie("2", "two", 1, null, null, -1, false, null, false)));
    }

    /**
     * Set-Cookie header should not contain multiple cookies.
     */
    @Test
    public void getCookiesMultipleInSetCookieHeader() {
        headersKnob.addHeader("Set-Cookie", "1=one; 2=two", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("1", "one", 0, null, null, -1, false, null, false)));
    }

    @Test
    public void getCookiesMixedHeaderNames() {
        headersKnob.addHeader("Cookie", "cookieName=cookieValue", HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "setCookieName=setCookieValue", HEADER_TYPE_HTTP);
        final Set<HttpCookie> cookies = cookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertTrue(containsCookie(cookies, new HttpCookie("cookieName", "cookieValue", 0, null, null, -1, false, null, false)));
        final Set<HttpCookie> setCookies = setCookieKnob.getCookies();
        assertEquals(1, setCookies.size());
        assertTrue(containsCookie(setCookies, new HttpCookie("setCookieName", "setCookieValue", 0, null, null, -1, false, null, false)));
    }

    @Test
    public void deleteSetCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertEquals(1, setCookieKnob.getCookies().size());
        setCookieKnob.deleteCookie(cookie);
        assertTrue(setCookieKnob.getCookies().isEmpty());
    }

    @Test
    public void deleteSetCookieNameMismatch() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertEquals(1, setCookieKnob.getCookies().size());
        setCookieKnob.deleteCookie(new HttpCookie("nomatch", "bar", 1, "/", "localhost", 60, true, "test", false));
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals("foo", cookies.iterator().next().getCookieName());
    }

    @Test
    public void deleteSetCookieDomainMismatch() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertEquals(1, setCookieKnob.getCookies().size());
        setCookieKnob.deleteCookie(new HttpCookie("foo", "bar", 1, "/", "mismatch", 60, true, "test", false));
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals("localhost", cookies.iterator().next().getDomain());
    }

    @Test
    public void deleteSetCookiePathMismatch() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertEquals(1, setCookieKnob.getCookies().size());
        setCookieKnob.deleteCookie(new HttpCookie("foo", "bar", 1, "/mismatch", "localhost", 60, true, "test", false));
        final Set<HttpCookie> cookies = setCookieKnob.getCookies();
        assertEquals(1, cookies.size());
        assertEquals("/", cookies.iterator().next().getPath());
    }

    @Test
    public void deleteSetCookieNotSetCookieHeader() {
        headersKnob.addHeader("Cookie", "foo=bar", HEADER_TYPE_HTTP);
        assertEquals(1, cookieKnob.getCookies().size());
        assertTrue(setCookieKnob.getCookies().isEmpty());
        setCookieKnob.deleteCookie(new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false));
        assertEquals(1, cookieKnob.getCookies().size());
        assertTrue(setCookieKnob.getCookies().isEmpty());
    }

    @Test
    public void deleteSetCookieInvalidCookieHeaderIgnored() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        // having an invalid cookie header should not affect the result
        headersKnob.addHeader("Set-Cookie", "invalidCookieHeader", HEADER_TYPE_HTTP);
        assertEquals(1, setCookieKnob.getCookies().size());
        setCookieKnob.deleteCookie(cookie);
        assertTrue(setCookieKnob.getCookies().isEmpty());
    }

    @Test
    public void deleteCookieNotCookieHeader() {
        headersKnob.addHeader("Set-Cookie", "foo=bar", HEADER_TYPE_HTTP);
        assertEquals(1, setCookieKnob.getCookies().size());
        assertTrue(cookieKnob.getCookies().isEmpty());
        cookieKnob.deleteCookie(new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false));
        assertEquals(1, setCookieKnob.getCookies().size());
        assertTrue(cookieKnob.getCookies().isEmpty());
    }

    @Test
    public void containsSetCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertTrue(setCookieKnob.containsCookie("foo", "localhost", "/"));
        assertFalse(cookieKnob.containsCookie("foo", "localhost", "/"));
    }

    @Test
    public void containsSetCookieNullDomain() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", null, 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertTrue(setCookieKnob.containsCookie("foo", null, "/"));
    }

    @Test
    public void containsSetCookieNullPath() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, null, "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertTrue(setCookieKnob.containsCookie("foo", "localhost", null));
    }

    @Test
    public void doesNotContainSetCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        setCookieKnob.addCookie(cookie);
        assertFalse(setCookieKnob.containsCookie("test", null, null));
        assertFalse(setCookieKnob.containsCookie("foo", "localhost", null));
        assertFalse(setCookieKnob.containsCookie("foo", null, "/"));
    }

    @Test
    public void containsCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, true, "test", false);
        cookieKnob.addCookie(cookie);
        assertTrue(cookieKnob.containsCookie("foo", "localhost", "/"));
        assertFalse(setCookieKnob.containsCookie("foo", "localhost", "/"));
    }

    @Test
    public void getCookiesAsHeadersNone() {
        assertTrue(cookieKnob.getCookiesAsHeaders().isEmpty());
    }

    @Test
    public void getCookiesAsHeaders() {
        cookieKnob.addCookie(new HttpCookie("1", "a", 1, "/", "localhost", 60, true, "test", false));
        cookieKnob.addCookie(new HttpCookie("2", "b", 1, "/", "localhost", 60, true, "test", false));
        headersKnob.addHeader("Cookie", "invalidCookie", HeadersKnob.HEADER_TYPE_HTTP);
        headersKnob.addHeader("Set-Cookie", "foo=bar", HeadersKnob.HEADER_TYPE_HTTP);

        final Set<String> cookiesAsHeaders = cookieKnob.getCookiesAsHeaders();
        assertEquals(3, cookiesAsHeaders.size());
        assertTrue(cookiesAsHeaders.contains("invalidCookie"));
        assertTrue(cookiesAsHeaders.contains("1=a; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure"));
        assertTrue(cookiesAsHeaders.contains("2=b; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure"));
    }

    @Test
    public void getCookiesAsHeadersMultipleInOneHeader() {
        headersKnob.addHeader("Cookie", "1=a; 2=b", HeadersKnob.HEADER_TYPE_HTTP);
        final Set<String> cookiesAsHeaders = cookieKnob.getCookiesAsHeaders();
        assertEquals(2, cookiesAsHeaders.size());
        assertTrue(cookiesAsHeaders.contains("1=a"));
        assertTrue(cookiesAsHeaders.contains("2=b"));
    }

    @Test
    public void getSetCookiesAsHeaders() {
        setCookieKnob.addCookie(new HttpCookie("1", "a", 1, "/", "localhost", 60, true, "test", false));
        setCookieKnob.addCookie(new HttpCookie("2", "b", 1, "/", "localhost", 60, true, "test", false));
        headersKnob.addHeader("Set-Cookie", "invalidCookie", HeadersKnob.HEADER_TYPE_HTTP);
        headersKnob.addHeader("Cookie", "foo=bar", HeadersKnob.HEADER_TYPE_HTTP);

        final Set<String> cookiesAsHeaders = setCookieKnob.getCookiesAsHeaders();
        assertEquals(3, cookiesAsHeaders.size());
        assertTrue(cookiesAsHeaders.contains("invalidCookie"));
        assertTrue(cookiesAsHeaders.contains("1=a; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure"));
        assertTrue(cookiesAsHeaders.contains("2=b; Version=1; Domain=localhost; Path=/; Comment=test; Max-Age=60; Secure"));
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
                cookie1.isSecure() == cookie2.isSecure() && cookie1.isHttpOnly() == cookie2.isHttpOnly() &&
                (!compareMaxAge || cookie1.getMaxAge() == cookie2.getMaxAge());
    }
}
