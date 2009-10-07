package com.l7tech.common.http;

/**
 * Interface implemented by objects that are capable of returning an {@link HttpHeaders} object.
 */
public interface HttpHeadersHaver {
    /**
     * Get the HTTP headers that were returned.  This will include the Content-Type and Content-Length headers.
     *
     * @return the HTTP headers.  May be empty but never null.
     */
    HttpHeaders getHeaders();
}
