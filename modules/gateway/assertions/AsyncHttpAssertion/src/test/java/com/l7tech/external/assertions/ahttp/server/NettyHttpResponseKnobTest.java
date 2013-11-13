package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.util.Pair;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class NettyHttpResponseKnobTest {
    private NettyHttpResponseKnob knob;
    private Collection<Pair<String, Object>> headers;
    private DefaultHttpResponse response;

    @Before
    public void setup() {
        headers = new ArrayList<>();
    }

    @Test
    public void beginResponse() throws Exception {
        response = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.PROCESSING);
        knob = new NettyHttpResponseKnob(response);
        knob.setStatus(200);
        knob.addCookie(new HttpCookie("http://localhost:8080", "/", "choc=chip"));
        headers.add(new Pair<String, Object>("foo", "bar"));
        headers.add(new Pair<String, Object>("foo", "bar2"));
        headers.add(new Pair<String, Object>("date", new GregorianCalendar(2013, 11, 13, 0, 0, 0).getTimeInMillis()));

        knob.beginResponse(headers);

        assertEquals(HttpResponseStatus.OK, response.getStatus());
        assertEquals(3, response.getHeaderNames().size());
        final List<String> fooHeaderValues = response.getHeaders("foo");
        assertEquals(2, fooHeaderValues.size());
        assertTrue(fooHeaderValues.contains("bar"));
        assertTrue(fooHeaderValues.contains("bar2"));
        final List<String> dateHeaderValues = response.getHeaders("date");
        assertEquals(1, dateHeaderValues.size());
        assertEquals("2013-12-13T08:00:00.000Z", dateHeaderValues.get(0));
        final List<String> setCookieValues = response.getHeaders(HttpHeaders.Names.SET_COOKIE);
        assertEquals(1, setCookieValues.size());
        assertTrue(setCookieValues.get(0).contains("choc=chip"));
    }
}
