/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;

/**
 * Interface that provides basic information about a generic HTTP response.
 * See {@link GenericHttpResponseParamsImpl} for a beany implementation.
 */
public interface GenericHttpResponseParams {
    /**
     * Get the HTTP status that was returned.  If a request succeeded, this should be an HTTP status
     * like 200, 401, 500 etc, but the caller should not assume this value will always make sense.
     *
     * @return the HTTP status, or 0 if not available.  Might be any integral value.
     */
    int getStatus();

    /**
     * Get the HTTP headers that were returned.  This will include the Content-Type and Content-Length headers.
     *
     * @return the HTTP headers.  May be empty but never null.
     */
    HttpHeaders getHeaders();

    /**
     * Get the content-type of the response, if one was present.
     *
     * @return the content-type header in this response, or null if there wasn't one.
     */
    ContentTypeHeader getContentType();

    /**
     * Get the content-length of the response, if one was present.  This will be the content-length returned
     * by the underlying HTTP client and thus should not be assumed to be a nonnegative integer.
     *
     * @return the value of hte content-length in this response, or null there wasn't one.
     */
    Long getContentLength();
}
