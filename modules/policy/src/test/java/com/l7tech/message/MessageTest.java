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
    public void testNoContentLengthHeaderInIsXml() throws IOException{
        //Set up
        setUpMessageWithReqServlet(false);
        // Test no content-length header present
        assertTrue(message.isXml());
    }

    @Test
    @BugId("DE338158")
    public void testAllowContentLengthZeroInIsXml() throws IOException{
        //Set up
        setUpMessageWithReqServlet(true);
        // Test allow content-length 0
        assertTrue(message.isXml(true));
    }

    @Test
    @BugId("DE338158")
    public void testDoNotAllowContentLengthZeroInIsXml() throws IOException{
        //Set up
        setUpMessageWithReqServlet(true);
        // Test allow content-length 0
        assertFalse(message.isXml());
    }

    private void setUpMessageWithReqServlet(final boolean withContentLength) throws IOException {
        final MockHttpServletRequest mockRequestServlet = new MockHttpServletRequest();
        message.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequestServlet));
        message.initialize(ContentTypeHeader.XML_DEFAULT, new byte[]{});
        if (withContentLength) {
            mockRequestServlet.addHeader(HttpConstants.HEADER_CONTENT_LENGTH, 0);
        }
    }
}
