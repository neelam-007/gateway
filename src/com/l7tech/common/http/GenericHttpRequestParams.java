/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;

import javax.net.ssl.SSLSocketFactory;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Interface that provides information about a pending HTTP request.  See {@link GenericHttpRequestParamsImpl} for
 * a beany implementation.
 */
public interface GenericHttpRequestParams {
    /**
     * Get the target URL of this request.
     *
     * @return the target URL, or null if this request is not yet completely configured.
     */
    URL getTargetUrl();

    /**
     * Get the credentials to present all the time or if challenged by the server.
     * <p>
     * If null, the request will fail if challenged.
     * <p>
     * If credentials are present, the client might send them in plaintext with the initial request, and
     * will attempt to use them to respond to an HTTP challenge from the server.
     * <p>
     * Unless a particular HTTP client implementation promises differently, it should be assumed that if credentials
     * are specified here, they might be transmitted in plaintext with the initial request regardless of whether an
     * HTTP challenge actually occurs, and regardless of the setting of {@link #isPreemptiveAuthentication()}.
     *
     * @return the credentials that will be presented, or null.
     */
    PasswordAuthentication getPasswordAuthentication();

    /**
     * Check the hint for whether to present credentials preemptively, with the initial request, rather than
     * waiting to be challenged.  This setting is only meaningful if {@link #getPasswordAuthentication()} returns non-null.
     * <p>
     * Unless a particular HTTP client implementation promises differently, it should be assumed that
     * this setting will be ignored.
     *
     * @return true if the password should be sent in the clear with the request, or false if the password should only
     *         be disclosed if challenged.
     */
    boolean isPreemptiveAuthentication();

    /**
     * Get the socket factory to use for an SSL request.
     *
     * @return the socket factory, or null if the default will be used.
     */
    SSLSocketFactory getSslSocketFactory();

    /**
     * Get the content-type header that will be attached to the request body if a POST is done.
     *
     * @return the {@link com.l7tech.common.mime.ContentTypeHeader} that will be sent with the request, or null.
     */
    ContentTypeHeader getContentType();

    /**
     * Set the content-length header that will be attached to the request body if a POST is done.
     * <p>
     * Regardless of this setting, unless a particular HTTP client implementation promises differently, it should be
     * assumed that the entire request body might be read and buffered by the HTTP client before the headers
     * are sent to the server.
     *
     * @return the value of the content-length header that will be sent with the request, or null if unspecified.
     */
    Long getContentLength();

    /**
     * Get the extra HTTP headers that will be sent with this request.
     * <p>
     * If conflicting values for the same header are providing (ie, by including a "Content-Type" header in
     * extraHeaders that disagrees with the setting of {@link #getContentType}),
     * it is not defined which value takes precedence unless a particular HTTP client implementation promises differently.
     *
     * @return the array of extra HTTP headers to include with the request.  May be empty but never null.
     */
    HttpHeader[] getExtraHeaders();

    /**
     * @return true if this request should follow any redirects it might receive from the server
     */
    boolean isFollowRedirects();
}
