package com.l7tech.message;

import org.junit.Before;
import org.junit.Test;

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
    public void getHttpCookiesKnob() {
        final HttpCookiesKnob cookiesKnob = message.getHttpCookiesKnob();
        assertNotNull(cookiesKnob);
        assertTrue(cookiesKnob.getCookies().isEmpty());
        assertEquals(cookiesKnob, message.getKnob(HttpCookiesKnob.class));
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
