package com.l7tech.message;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

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

    @Test
    @BugId("DE338158")
    public void checkContentLengthInIsXml() throws IOException{
        //Set up
        final MimeKnob mknob = message.getMimeKnob();
        mknob.getFirstPart().setContentType(ContentTypeHeader.XML_DEFAULT);
        final HeadersKnob hknob = message.getHeadersKnob();
        hknob.setHeader("content-length", 0, HeadersKnob.HEADER_TYPE_HTTP);

        // Test content-length:0, no message body means it's not XML
        assertFalse(message.isXml());

        // Test no content-length header present
        message.initialize(ContentTypeHeader.XML_DEFAULT, new byte[]{});
        hknob.removeHeader("content-length", HeadersKnob.HEADER_TYPE_HTTP);
        assertTrue(message.isXml());
    }
}
