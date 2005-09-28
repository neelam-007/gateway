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
 * Bean that provides information about a pending HTTP request. 
 */
public class GenericHttpRequestParams {
    protected URL targetUrl;
    protected GenericHttpState state;
    protected PasswordAuthentication passwordAuthentication = null;
    protected SSLSocketFactory sslSocketFactory = null;
    protected ContentTypeHeader contentType = null;
    protected Long contentLength = null;
    private HttpHeader[] extraHeaders = new HttpHeader[0];
    protected boolean preemptiveAuthentication = true;
    protected boolean followRedirects = false;

    /**
     * Create a new request description that does not yet have a target URL set.
     */
    public GenericHttpRequestParams() {
    }

    /**
     * Create a request description pointing at the specified target URL.
     *
     * @param targetUrl the target URL of this request
     */
    public GenericHttpRequestParams(URL targetUrl) {
        this();
        this.targetUrl = targetUrl;
    }

    public GenericHttpRequestParams(URL targetUrl, GenericHttpState state) {
        this(targetUrl);
        this.state = state;
    }

    public GenericHttpState getState() {
        return state;        
    }

    /**
     * Get the target URL of this request.
     *
     * @return the target URL, or null if this request is not yet completely configured.
     */
    public URL getTargetUrl() {
        return targetUrl;
    }

    /**
     * Set the target URL of this request.
     *
     * @param targetUrl the target URL.  Should not be null.
     */
    public void setTargetUrl(URL targetUrl) {
        this.targetUrl = targetUrl;
    }

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
    public PasswordAuthentication getPasswordAuthentication() {
        return passwordAuthentication;
    }

    /**
     * Set the credentials to present all the time or if challenged by the server.
     *
     * @param passwordAuthentication the credentials to present if challenged, or null to allow challeges to fail.
     * @see #getPasswordAuthentication
     */
    public void setPasswordAuthentication(PasswordAuthentication passwordAuthentication) {
        this.passwordAuthentication = passwordAuthentication;
    }

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
    public boolean isPreemptiveAuthentication() {
        return preemptiveAuthentication;
    }

    /**
     * Set whether to request preemptive authentication be performed with the request.
     *
     * @see #isPreemptiveAuthentication()
     * @param preemptiveAuthentication
     */
    public void setPreemptiveAuthentication(boolean preemptiveAuthentication) {
        this.preemptiveAuthentication = preemptiveAuthentication;
    }

    /**
     * Get the socket factory to use for an SSL request.
     *
     * @return the socket factory, or null if the default will be used.
     */
    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Set the socket factory to use for an SSL request.
     *
     * @param sslSocketFactory the socket factory, or null if the default should be used.
     */
    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * Get the content-type header that will be attached to the request body if a POST is done.
     *
     * @return the {@link com.l7tech.common.mime.ContentTypeHeader} that will be sent with the request, or null.
     */
    public ContentTypeHeader getContentType() {
        return contentType;
    }

    /**
     * Set the content-type header that will be attached to the request body if a POST is done.
     *
     * @param contentType the {@link com.l7tech.common.mime.ContentTypeHeader} that will be sent with the request, or null.
     */
    public void setContentType(ContentTypeHeader contentType) {
        this.contentType = contentType;
    }

    /**
     * Set the content-length header that will be attached to the request body if a POST is done.
     * <p>
     * Regardless of this setting, unless a particular HTTP client implementation promises differently, it should be
     * assumed that the entire request body might be read and buffered by the HTTP client before the headers
     * are sent to the server.
     *
     * @return the value of the content-length header that will be sent with the request, or null if unspecified.
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * Get the content-length header that will be attached to the request body if a POST is done.
     *
     * @param contentLength the value of the content-length header to send, or null to leave unspecified.
     * @see #getContentLength
     */
    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * Get the extra HTTP headers that will be sent with this request.
     * <p>
     * If conflicting values for the same header are providing (ie, by including a "Content-Type" header in
     * extraHeaders that disagrees with the setting of {@link #getContentType}),
     * it is not defined which value takes precedence unless a particular HTTP client implementation promises differently.
     *
     * @return the array of extra HTTP headers to include with the request.  May be empty but never null.
     */
    public HttpHeader[] getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * @return true if this request should follow any redirects it might receive from the server
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * @param followRedirects if true, redirects will be followed automatically.
     * @see {@link #isFollowRedirects()}
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Set the extra HTTP headers to send with this request.
     *
     * @param extraHeaders the array of extra HTTP headers to include with this request, or null to set an empty array.
     * @see #getExtraHeaders
     */
    public void setExtraHeaders(HttpHeader[] extraHeaders) {
        this.extraHeaders = extraHeaders != null ? extraHeaders : new HttpHeader[0];
    }
}
