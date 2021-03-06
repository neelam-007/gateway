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
        ServletUtils.loadHeaders(request, message);
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
        ServletUtils.loadHeaders(request, message);
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
        ServletUtils.loadHeaders(request, message);
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
        ServletUtils.loadHeaders(request, message);
        final Collection<Header> knobHeaders = knob.getHeaders();
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), knobHeaders.size());
        for (final Header knobHeader : knobHeaders) {
            assertFalse(knobHeader.isPassThrough());
        }
    }

    @Test
    public void loadHeadersAndCookies() {
        request.addHeader("foo", "bar");
        request.addHeader("Cookie", "1=one; 2=two");

        ServletUtils.loadHeaders(request, message);

        final Map<String, HttpCookie> knobCookies = toCookiesMap(message.getHttpCookiesKnob().getCookies());
        assertEquals(2, knobCookies.size());
        final HttpCookie cookie1 = knobCookies.get("1");
        assertEquals("1", cookie1.getCookieName());
        assertEquals("one", cookie1.getCookieValue());
        final HttpCookie cookie2 = knobCookies.get("2");
        assertEquals("2", cookie2.getCookieName());
        assertEquals("two", cookie2.getCookieValue());

        final Map<String, List<Header>> knobHeaders = toHeadersMap(message.getHeadersKnob().getHeaders());
        assertEquals(2, knobHeaders.size());
        final Header header1 = knobHeaders.get("foo").iterator().next();
        assertEquals("foo", header1.getKey());
        assertEquals("bar", header1.getValue());
        final List<Header> cookieHeaders = knobHeaders.get("Cookie");
        assertEquals(1, cookieHeaders.size());
        assertEquals("1=one; 2=two", cookieHeaders.get(0).getValue());
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
