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
 * Bean that holds information about a pending HTTP request.
 */
public class GenericHttpRequestParamsImpl implements GenericHttpRequestParams {
    private URL targetUrl;
    private PasswordAuthentication passwordAuthentication = null;
    private SSLSocketFactory sslSocketFactory = null;
    private ContentTypeHeader contentType = null;
    private Long contentLength = null;
    private HttpHeader[] extraHeaders = new HttpHeader[0];
    private boolean preemptiveAuthentication = true;
    private boolean followRedirects = false;

    /**
     * Create a new request description that does not yet have a target URL set.
     */
    public GenericHttpRequestParamsImpl() {
    }

    /**
     * Create a request description pointing at the specified target URL.
     *
     * @param targetUrl the target URL of this request
     */
    public GenericHttpRequestParamsImpl(URL targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * Create a request description pointing at the specified target URL and with the specified parameters.
     *
     * @param targetUrl the target URL of this request
     * @param passwordAuthentication {@see #getPasswordAuthentication}
     * @param sslSocketFactory {@see #getSslSocketFactory}
     * @param contentType {@see #getContentType}
     * @param contentLength {@see #getContentLength}
     * @param extraHeaders {@see #getExtraHeaders}
     */
    public GenericHttpRequestParamsImpl(URL targetUrl,
                                    PasswordAuthentication passwordAuthentication,
                                    SSLSocketFactory sslSocketFactory,
                                    ContentTypeHeader contentType,
                                    Long contentLength,
                                    HttpHeader[] extraHeaders) {
        this(targetUrl);
        this.sslSocketFactory = sslSocketFactory;
        this.passwordAuthentication = passwordAuthentication;
        this.contentType = contentType;
        this.contentLength = contentLength;
        setExtraHeaders(extraHeaders);
    }

    /**
     * Create a mutable copy of the specified GenericHttpRequestParams.
     *
     * @param original the original to be copied.  Must not be null.
     */
    public GenericHttpRequestParamsImpl(GenericHttpRequestParams original) {
        this(original.getTargetUrl(),
             original.getPasswordAuthentication(),
             original.getSslSocketFactory(),
             original.getContentType(),
             original.getContentLength(),
             original.getExtraHeaders());
    }

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

    public ContentTypeHeader getContentType() {
        return contentType;
    }

    /**
     * Set the content-type header that will be attached to the request body if a POST is done.
     *
     * @param contentType the {@link ContentTypeHeader} that will be sent with the request, or null.
     */
    public void setContentType(ContentTypeHeader contentType) {
        this.contentType = contentType;
    }

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

    public HttpHeader[] getExtraHeaders() {
        return extraHeaders;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

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
