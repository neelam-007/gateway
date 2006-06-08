/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;

import java.io.InputStream;
import java.io.IOException;

/**
 * Provides a client-independant, generic interface to an HTTP response.
 */
public abstract class GenericHttpResponse implements GenericHttpResponseParams {
    /**
     * Get the InputStream from which the response body can be read.  Some implementations may close the
     * entire HTTP response when this InputStream is closed.
     *
     * @return the InputStream of the response body.  Never null.
     * @throws GenericHttpException if there is a network problem or protocol violation.
     */
    public abstract InputStream getInputStream() throws GenericHttpException;

    /**
     * Get the entire HTTP response body as a String, if the returned HTTP status was 200.
     *
     * @return a String with HTTP-ly-correct encoding (default ISO8859-1 if not declared).  Never null.
     * @throws IOException  if the status isn't 200
     * @throws java.io.UnsupportedEncodingException if we can't handle the declared character encoding
     */
    public String getAsString() throws IOException {
        if (getStatus() != HttpConstants.STATUS_OK)
            throw new IOException("HTTP status was " + getStatus());
        ContentTypeHeader ctype = getContentType();
        String encoding = ctype == null ? ContentTypeHeader.DEFAULT_HTTP_ENCODING : ctype.getEncoding();
        return new String(HexUtils.slurpStreamLocalBuffer(getInputStream()), encoding);
    }

    /**
     * Free all resources used by this response.
     * <p>
     * After close() has been called, the behaviour of this class is not defined.  Any InputStream obtained
     * from {@link #getInputStream} should no longer be used.
     * <p>
     * However, any {@link HttpHeaders} object returned from {@link #getHeaders} is guaranteed to remain useable even
     * after this request has been closed.    
     * <p>
     * Might throw unavoidable Errors (ie, OutOfMemoryError), but will never throw runtime exceptions.
     */
    public abstract void close();
}
