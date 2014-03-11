package com.l7tech.server.util;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.message.Header;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ServletUtilsTest {
    private MockHttpServletRequest request;
    private HeadersKnob knob;
    private Message message;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        message = new Message();
        knob = message.getHeadersKnob();
    }

    @Test
    public void loadRequestHeaders() {
        request.addHeader("foo", "bar");
        ServletUtils.loadHeadersAndCookies(request, message);
        assertEquals(1, knob.getHeaderNames().length);
        assertEquals("foo", knob.getHeaderNames()[0]);
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
        assertTrue(knob.getHeaders().iterator().next().isPassThrough());
    }

    @Test
    public void loadRequestHeadersMultipleValuesForHeader() {
        request.addHeader("foo", "bar");
        request.addHeader("foo", "bar2");
        ServletUtils.loadHeadersAndCookies(request, message);
        assertEquals(1, knob.getHeaderNames().length);
        assertEquals("foo", knob.getHeaderNames()[0]);
        assertEquals(2, knob.getHeaderValues("foo").length);
        final List<String> values = Arrays.asList(knob.getHeaderValues("foo"));
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("bar2"));
        for (final Header knobHeader : knob.getHeaders()) {
            assertTrue(knobHeader.isPassThrough());
        }
    }

    @Test
    public void loadRequestHeadersFilters() {
        for (final String shouldNotBeForwarded : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            request.addHeader(shouldNotBeForwarded, "testValue");
        }
        ServletUtils.loadHeadersAndCookies(request, message);
        final Collection<Header> knobHeaders = knob.getHeaders();
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), knobHeaders.size());
        for (final Header knobHeader : knobHeaders) {
            assertFalse(knobHeader.isPassThrough());
        }
    }

    @Test
    public void loadRequestHeadersFiltersCaseInsensitive() {
        for (final String shouldNotBeForwarded : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            request.addHeader(shouldNotBeForwarded.toUpperCase(), "testValue");
        }
        ServletUtils.loadHeadersAndCookies(request, message);
        final Collection<Header> knobHeaders = knob.getHeaders();
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), knobHeaders.size());
        for (final Header knobHeader : knobHeaders) {
            assertFalse(knobHeader.isPassThrough());
        }
    }

    @Test
    public void loadCookies() {
        request.setCookies(new Cookie("1", "one"), new Cookie("2", "two"));
        ServletUtils.loadHeadersAndCookies(request, message);
        final Set<HttpCookie> knobCookies = message.getHttpCookiesKnob().getCookies();
        assertEquals(2, knobCookies.size());
        final Iterator<HttpCookie> iterator = knobCookies.iterator();
        final HttpCookie cookie1 = iterator.next();
        assertEquals("1", cookie1.getCookieName());
        assertEquals("one", cookie1.getCookieValue());
        final HttpCookie cookie2 = iterator.next();
        assertEquals("2", cookie2.getCookieName());
        assertEquals("two", cookie2.getCookieValue());
    }

    @Test
    public void loadHeadersAndCookies() {
        request.addHeader("foo", "bar");
        request.addHeader("Cookie", "1=one");
        request.setCookies(new Cookie("2", "two"));

        ServletUtils.loadHeadersAndCookies(request, message);

        final Map<String, HttpCookie> knobCookies = toCookiesMap(message.getHttpCookiesKnob().getCookies());
        assertEquals(2, knobCookies.size());
        final HttpCookie cookie1 = knobCookies.get("1");
        assertEquals("1", cookie1.getCookieName());
        assertEquals("one", cookie1.getCookieValue());
        final HttpCookie cookie2 = knobCookies.get("2");
        assertEquals("2", cookie2.getCookieName());
        assertEquals("two", cookie2.getCookieValue());

        final Map<String, List<Header>> knobHeaders = toHeadersMap(message.getHeadersKnob().getHeaders());
        assertEquals(3, knobHeaders.size());
        final Header header1 = knobHeaders.get("foo").iterator().next();
        assertEquals("foo", header1.getKey());
        assertEquals("bar", header1.getValue());
        final Header header2 = knobHeaders.get("Cookie").iterator().next();
        assertEquals("1=one", header2.getValue());
        // cookies on the request (not as a header) are stored as Set-Cookie headers
        final Header header3 = knobHeaders.get("Set-Cookie").iterator().next();
        assertEquals("2=two; Version=0", header3.getValue());
    }

    @Test
    public void loadHeadersAndCookiesDoesNotDuplicateCookies() {
        request.addHeader("Cookie", "1=one");
        request.setCookies(new Cookie("1", "one"));

        ServletUtils.loadHeadersAndCookies(request, message);
        final Set<HttpCookie> knobCookies = message.getHttpCookiesKnob().getCookies();
        assertEquals(1, knobCookies.size());
        final Iterator<HttpCookie> iterator = knobCookies.iterator();
        final HttpCookie cookie = iterator.next();
        assertEquals("1", cookie.getCookieName());
        assertEquals("one", cookie.getCookieValue());
        final Collection<Header> knobHeaders = message.getHeadersKnob().getHeaders();
        assertEquals(1, knobHeaders.size());
        final Header header = knobHeaders.iterator().next();
        assertEquals("Cookie", header.getKey());
        assertEquals("1=one", header.getValue());
    }

    private Map<String, HttpCookie> toCookiesMap(final Collection<HttpCookie> cookies) {
        final Map<String, HttpCookie> map = new HashMap<>();
        for (final HttpCookie cookie : cookies) {
            map.put(cookie.getCookieName(), cookie);
        }
        return map;
    }

    private Map<String, List<Header>> toHeadersMap(final Collection<Header> headers) {
        final Map<String, List<Header>> map = new HashMap<>();
        for (final Header header : headers) {
            if (!map.containsKey(header.getKey())) {
                map.put(header.getKey(), new ArrayList<Header>());
            }
            final List<Header> value = map.get(header.getKey());
            value.add(header);
            map.put(header.getKey(), value);
        }
        return map;
    }
}
