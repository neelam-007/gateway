/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.util.List;

/**
 * Represents a collection of HTTP headers.
 */
public interface HttpHeaders {

    /**
     * Get the first value of the given header.
     *
     * @param name the name of the header to search for.  Must not be null.
     * @return the first value given for this header in this collection of HTTP headers, or null if it was not found.
     */
    String getFirstValue(String name);

    /**
     * Get the only value of the given header.
     *
     * @param name the name of the header to search for.  Must not be null.
     * @return the only value given for this header in this collection of HTTP headers, or null if it was not found.
     * @throws GenericHttpException if there was more than one value for this header.
     */
    String getOnlyOneValue(String name) throws GenericHttpException;

    /**
     * Get all values of the given header.
     *
     * @param name the name of the header to search for.  Must not be null.
     * @return the first value for this header in this collection of HTTP headers.  May be empty but never null.
     */
    List getValues(String name);

    /**
     * Get the complete array of headers in the original order.
     *
     * @return an array of {@link HttpHeader} instances.  May contain duplicates.  May be empty but never null.
     */
    HttpHeader[] toArray();

    /**
     * Get the complete list of headers rendered as a String, as might appear in an HTTP request or response.
     * Will include a trailing newline after each header, including the trailing newline after the final header,
     * but not including the extra newline that is required within an HTTP request or reply.
     * <p>
     * For example, if there were headers Content-Type: text/xml and Content-Length: 400, this method will return
     * "Content-Type: text/xml\nContent-Length: 400\n".
     *
     * @return the headers rendered as a single String.
     */
    String toExternalForm();
}
