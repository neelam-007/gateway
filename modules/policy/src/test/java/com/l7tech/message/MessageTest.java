package com.l7tech.message;

import com.l7tech.common.http.HttpConstants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.*;

public class MessageTest {
    private Message message;

    @Before
    public void setup() {
        message = new Message();
    }

    @Test
    public void attachHeadersKnob() {
        message.attachKnob(HeadersKnob.class, new HeadersKnobSupport());
        assertTrue(message.getHeadersKnob().getHeaders().isEmpty());
        assertEquals(message.getHeadersKnob(), message.getKnob(HeadersKnob.class));
    }

    @Test
    public void getHeadersKnob() {
        final HeadersKnob headersKnob = message.getHeadersKnob();
        assertNotNull(headersKnob);
        assertTrue(headersKnob.getHeaders().isEmpty());
        assertEquals(headersKnob, message.getKnob(HeadersKnob.class));
    }

    @Test
    public void getHttpCookiesKnobRequest() {
        message.attachHttpRequestKnob(new HttpServletRequestKnob(new MockHttpServletRequest()));
        final HttpCookiesKnobImpl cookiesKnob = (HttpCookiesKnobImpl) message.getHttpCookiesKnob();
        assertEquals(HttpConstants.HEADER_COOKIE, cookiesKnob.getCookieHeaderName());
    }

    @Test
    public void getHttpCookiesKnobResponse() {
        message.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        final HttpCookiesKnobImpl cookiesKnob = (HttpCookiesKnobImpl) message.getHttpCookiesKnob();
        assertEquals(HttpConstants.HEADER_SET_COOKIE, cookiesKnob.getCookieHeaderName());
    }

    @Test
    public void getHttpCookiesKnobNeitherRequestOrResponse() {
        assertFalse(message.isHttpRequest());
        assertFalse(message.isHttpResponse());
        final HttpCookiesKnob cookiesKnob = message.getHttpCookiesKnob();
        assertNotNull(cookiesKnob);
        assertTrue(cookiesKnob.getCookies().isEmpty());
        assertEquals(cookiesKnob, message.getKnob(HttpCookiesKnob.class));
        assertEquals(HttpConstants.HEADER_COOKIE, ((HttpCookiesKnobImpl) cookiesKnob).getCookieHeaderName());
    }

    @Test
    public void findHeadersKnobNotAttached() {
        assertNull(message.getKnob(HeadersKnob.class));
    }

    @Test
    public void findHttpCookiesKnobNotAttached() {
        assertNull(message.getKnob(HttpCookiesKnob.class));
    }
}
