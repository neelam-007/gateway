/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.io.InputStream;

/**
 * Represents a generic HTTP request.
 * If it's a POST, user should write the post body to the output stream and then call getResponse().
 * If this is a GET request, user should just immediately call getResponse().
 */
public interface GenericHttpRequest {
    /**
     * Set the InputStream from which the request body will be read, if applicable.  Will only be read if this
     * is a POST request.
     *
     * @param bodyInputStream the InputStream for the request body, or null to send an empty body.
     * @throws UnsupportedOperationException if this is not a POST or PUT request.
     * @throws IllegalStateException if getResponse() has already been called.
     */
    void setInputStream(InputStream bodyInputStream);

    /**
     * Commit the request and begin receiving the response.  This is the pivot point of the request.
     * <p>
     * If this is a POST request and no request body has yet been written, and empty request body will be transmitted.
     * <p>
     * Regardless of whether getResponse() succeeds or fails, the request will always be closed afterward as if by
     * a call to {@link #close}.
     *
     * @return the response object.  Never null.
     * @throws GenericHttpException if the request fails due to network trouble or an HTTP protocol violation.
     * @throws IllegalStateException if getResponse() has already been called.
     */
    GenericHttpResponse getResponse() throws GenericHttpException;

    /**
     * Release all resources used by an in-progress request.  If {@link #getResponse} has been called,
     * the response will still need to be closed seperately.  To reiterate, closing the request will not affect
     * the usability of the GenericHttpResponse, if a response has already been obtained.
     * <p>
     * After close() has been called, the behaviour of this class is not defined.
     * <p>
     * Might throw unavoidable Errors (ie, OutOfMemoryError), but will never throw runtime exceptions.
     */
    void close();
}
