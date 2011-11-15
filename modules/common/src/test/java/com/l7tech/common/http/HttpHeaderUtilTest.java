package com.l7tech.common.http;

import org.junit.Test;

import static com.l7tech.common.http.HttpHeaderUtil.acceptsGzipResponse;
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
}
