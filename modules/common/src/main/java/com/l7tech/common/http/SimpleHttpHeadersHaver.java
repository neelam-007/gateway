package com.l7tech.common.http;

/**
 * An HttpHeadersHaver that simply.. has some HttpHeaders.
 */
public class SimpleHttpHeadersHaver implements HttpHeadersHaver {
    private final HttpHeaders headers;

    public SimpleHttpHeadersHaver(HttpHeaders headers) {
        if (headers == null)
            throw new NullPointerException();
        this.headers = headers;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }
}
