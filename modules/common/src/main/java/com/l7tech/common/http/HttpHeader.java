/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

/**
 * Simple interface representing a single HTTP header.
 * See {@link com.l7tech.common.mime.MimeHeader} for a full-featured implementation
 * and {@link GenericHttpHeader} for a lightweight immutable implementation.
 */
public interface HttpHeader {
    /**
     * Get the name of this header.
     *
     * @return the name of this header, ie "Content-Length".
     */
    String getName();

    /**
     * Get the full value of this header, as it might appear in an HTTP request or response.
     *
     * @return The complete value of this header, including all parameters.  Never null.
     *         This will return the complete value even for headers with a predefined value format.
     *         (that is, it will return "text/xml; charset=utf-8" rather than just "text/xml").
     */
    String getFullValue();
}
