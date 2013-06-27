package com.l7tech.common.http;

import org.junit.Test;

import static com.l7tech.common.http.HttpHeaderUtil.acceptsGzipResponse;
import static com.l7tech.common.http.HttpHeaderUtil.searchHeaderValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test case for {@link HttpHeaderUtil}.
 */
public class HttpHeaderUtilTest {

    @Test
    public void testAcceptsGzipResponse() {
        assertFalse(acceptsGzipResponse(null));
        assertTrue(acceptsGzipResponse("*"));
        assertTrue(acceptsGzipResponse("  * "));
        assertFalse(acceptsGzipResponse("*;q=0"));
        assertFalse(acceptsGzipResponse(" *;q=0  "));
        assertTrue(acceptsGzipResponse("gzip"));
        assertTrue(acceptsGzipResponse("  gzip  "));
        assertTrue(acceptsGzipResponse("gzip,deflate"));
        assertTrue(acceptsGzipResponse("gziP,deflate"));
        assertTrue(acceptsGzipResponse("  gzip , deflate "));
        assertFalse(acceptsGzipResponse("  gzipp , deflate "));
        assertTrue(acceptsGzipResponse("deflate,gzip"));
        assertFalse(acceptsGzipResponse("asdf,gzip;q=0,deflate"));
        assertFalse(acceptsGzipResponse("asdf,gzip;q=0"));
        assertFalse(acceptsGzipResponse("gzip;q=0,deflate"));
        assertFalse(acceptsGzipResponse("gzip;q=0.5,deflate,identity"));
        assertFalse(acceptsGzipResponse("*;q=0.0"));
        assertFalse(acceptsGzipResponse("deflate,gzip;q=0.000"));

        // The following edge case values should technically return true, but for now are handled conservatively
        assertFalse(acceptsGzipResponse("gzip;q=0.5,deflate;q=1,otherthing"));
        assertFalse(acceptsGzipResponse("*;q=0.2"));
        assertFalse(acceptsGzipResponse("gzip;q=0.44"));
        assertFalse(acceptsGzipResponse("bzip2,gzip;q=0.5,deflate;q=0.3,identity;q=0.4"));
    }

    @Test
    public void testOneHeader_first() throws Exception {
        final HttpHeader[] headers = {new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8"),
                new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;encoding=ascii")};
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(headers);
        assertEquals("text/xml;encoding=utf8", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "first"));
        assertEquals("text/xml;encoding=utf8", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "0"));
    }

    @Test
    public void testOneHeader_last() throws Exception {
        final HttpHeader[] headers = {new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8"),
                new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;encoding=ascii")};
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(headers);
        assertEquals("text/plain;encoding=ascii", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "last "));
        assertEquals("text/plain;encoding=ascii", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, " 1 "));
    }

    @Test
    public void testOneHeader_secondLast() throws Exception {
        final HttpHeader[] headers = {new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8"),
                new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;encoding=ascii"),
                new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain")};
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(headers);
        assertEquals("text/plain;encoding=ascii", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, " -1 "));
        assertEquals("text/plain;encoding=ascii", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "1"));
    }

    @Test(expected = GenericHttpException.class)
    public void testOneHeader_headerNotFound() throws GenericHttpException {
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8")});
       searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "333");
    }

    @Test(expected = GenericHttpException.class)
    public void testOneHeader_invalidSearchRule() throws GenericHttpException {
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8")});
        searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "somevalue");
    }

    @Test
    public void testOneHeader_emptyHeaders() throws Exception {
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{});
        assertTrue(searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "333") == null);
    }

    @Test
    public void testOneHeader_off() throws Exception {
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8")});
        assertEquals("text/xml;encoding=utf8", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "off "));
        assertEquals("text/xml;encoding=utf8", searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, null));
    }

    @Test(expected = GenericHttpException.class)
    public void testOneHeader_multipleNotAllowed() throws Exception {
        final HttpHeader[] headers = {new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/xml;encoding=utf8"),
                new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;encoding=ascii")};
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(headers);
        searchHeaderValue(responseHeaders, HttpConstants.HEADER_CONTENT_TYPE, "off");
    }
}
