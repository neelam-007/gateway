package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.util.Pair;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class NettyHttpRequestKnobTest {
    private NettyHttpRequestKnob knob;
    private Collection<Pair<String, Object>> headers;
    private DefaultHttpRequest request;

    @Before
    public void setup() {
        headers = new ArrayList<>();
        request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "");
        knob = new NettyHttpRequestKnob(request, null, null, null);
    }

    @Test
    public void getCookiesHttpOnly() {
        request.addHeader("Cookie", "foo=bar; HttpOnly");
        final HttpCookie[] cookies = knob.getCookies();
        assertEquals(1, cookies.length);
        assertTrue(cookies[0].isHttpOnly());
    }

    @Test
    public void getCookiesNotHttpOnly() {
        request.addHeader("Cookie", "foo=bar");
        final HttpCookie[] cookies = knob.getCookies();
        assertEquals(1, cookies.length);
        assertFalse(cookies[0].isHttpOnly());
    }
}
