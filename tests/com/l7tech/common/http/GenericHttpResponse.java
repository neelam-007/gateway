/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.io.InputStream;

/**
 * Provides a client-independant, generic interface to an HTTP response.
 */
public interface GenericHttpResponse extends GenericHttpResponseParams {
    /**
     * Get the InputStream from which the response body can be read.
     *
     * @return the InputStream of the response body.  Never null.
     * @throws GenericHttpException if there is a network problem or protocol violation.
     */
    InputStream getInputStream() throws GenericHttpException;

    /**
     * Free all resources used by this response.
     * <p>
     * After close() has been called, the behaviour of this class is not defined.
     * <p>
     * Might throw unavoidable Errors (ie, OutOfMemoryError), but will never throw runtime exceptions.
     */
    void close();
}
