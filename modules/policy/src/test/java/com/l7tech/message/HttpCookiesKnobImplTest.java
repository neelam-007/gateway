package com.l7tech.message;

import com.l7tech.common.http.HttpCookie;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class HttpCookiesKnobImplTest {
    private HttpCookiesKnob cookiesKnob;

    @Before
    public void setup() {
        cookiesKnob = new HttpCookiesKnobImpl();
    }

    @Test
    public void addCookie() {
        final HttpCookie cookie = new HttpCookie("foo", "bar", 1, "/", "localhost", 60, false, "test");
        cookiesKnob.addCookie(cookie);
        assertEquals(1, cookiesKnob.getCookies().size());
        assertEquals(cookie, cookiesKnob.getCookies().iterator().next());
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

    private boolean cookieAttributesEqual(final HttpCookie cookie1, final HttpCookie cookie2, final boolean compareMaxAge) {
        return ObjectUtils.equals(cookie1.getCookieName(), cookie2.getCookieName()) && ObjectUtils.equals(cookie1.getCookieValue(), cookie2.getCookieValue()) &&
                cookie1.getVersion() == cookie2.getVersion() && ObjectUtils.equals(cookie1.getDomain(), cookie2.getDomain()) &&
                ObjectUtils.equals(cookie1.getPath(), cookie2.getPath()) && ObjectUtils.equals(cookie1.getComment(), cookie2.getComment()) &&
                (compareMaxAge ? cookie1.getMaxAge() == cookie2.getMaxAge() : true);
    }
}
