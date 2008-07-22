/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HostnameVerifier;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;

/**
 * Bean that provides information about a pending HTTP request. 
 */
public class GenericHttpRequestParams {

    public static enum HttpVersion {
        HTTP_VERSION_1_0,
        HTTP_VERSION_1_1,
    }

    private URL targetUrl;
    private GenericHttpState state;
    private PasswordAuthentication passwordAuthentication = null;
    private NtlmAuthentication ntlmAuthentication = null;
    private SSLSocketFactory sslSocketFactory = null;
    private HostnameVerifier hostnameVerifier = null;
    private ContentTypeHeader contentType = null;
    private Long contentLength = null;
    private List<HttpHeader> extraHeaders = null;
    private boolean preemptiveAuthentication = true;
    private boolean followRedirects = false;
    private boolean useKeepAlives = true;
    private boolean useExpectContinue = false;
    private HttpVersion httpVersion = HttpVersion.HTTP_VERSION_1_1;

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

    /**
     * Create a new request description that has the same properties as the
     * given GenericHttpRequestParams.
     *
     * @param template a template to copy from
     */
    public GenericHttpRequestParams(GenericHttpRequestParams template) {
        targetUrl = template.targetUrl;
        state = template.state;
        passwordAuthentication = template.passwordAuthentication;
        sslSocketFactory = template.sslSocketFactory;
        contentType = template.contentType;
        contentLength = template.contentLength;
        extraHeaders = template.extraHeaders == null ? null : new ArrayList<HttpHeader>(template.extraHeaders);
        preemptiveAuthentication = template.preemptiveAuthentication;
        followRedirects = template.followRedirects;
        useKeepAlives = template.useKeepAlives;
        useExpectContinue = template.useExpectContinue;
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


    public NtlmAuthentication getNtlmAuthentication() {
        return ntlmAuthentication;
    }

    public void setNtlmAuthentication(NtlmAuthentication ntlmAuthentication) {
        this.ntlmAuthentication = ntlmAuthentication;
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
     * @param preemptiveAuthentication true to enable preemptive authentication, or false to disable it
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
     * Get the hostname verifier to use.
     *
     * @return The hostname verifier or null if not set.
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Set the hostname verifier to use.
     *
     * @param hostnameVerifier The verifier (may be null)
     */
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
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
     * @return the list of extra HttpHeader to include with the request.  May be empty but never null.
     */
    public List<HttpHeader> getExtraHeaders() {
        return extraHeaders == null ? Collections.<HttpHeader>emptyList() : extraHeaders;
    }

    /**
     * @return true if this request should follow any redirects it might receive from the server
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * @param followRedirects if true, redirects will be followed automatically.
     * @see #isFollowRedirects()
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Should an Expect/Continue handshake be performed.
     *
     * @return True if expect/continue is to be used.
     */
    public boolean isUseExpectContinue() {
        return useExpectContinue;
    }

    /**
     * Set use of Expect/Continue handshake
     *
     * @param useExpectContinue True for expect/continue
     */
    public void setUseExpectContinue(boolean useExpectContinue) {
        this.useExpectContinue = useExpectContinue;
    }

    /**
     * Should Keep-Alive connections be used.
     *
     * @return True if Keep-Alive is to be used.
     */
    public boolean isUseKeepAlives() {
        return useKeepAlives;
    }

    /**
     * Set use of Keep-Alive connections
     *
     * @param useKeepAlives True for Keep-Alive
     */
    public void setUseKeepAlives(boolean useKeepAlives) {
        this.useKeepAlives = useKeepAlives;
    }

    /**
     * Get the HTTP protocol version to use.
     *
     * @return The HTTP version
     */
    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * Set the HTTP protocol version.
     *
     * @param httpVersion The HTTP version
     */
    public void setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * Set the extra HTTP headers to send with this request, setting them as an array.
     *
     * @param extraHeaders the array of extra HTTP headers to include with this request, or null to set an empty array.
     * @see #getExtraHeaders
     */
    public void setExtraHeaders(HttpHeader[] extraHeaders) {
        this.extraHeaders = extraHeaders != null ? new ArrayList<HttpHeader>(Arrays.asList(extraHeaders)) : null;
    }

    /**
     * Add an extra HTTP header to send.  It will be added to the end of the extraHeaders.
     * Be warned that this may not be very fast.
     * @param extraHeader an extra header to include in the request
     */
    public void addExtraHeader(HttpHeader extraHeader) {
        if (extraHeaders == null) extraHeaders = new ArrayList<HttpHeader>();
        extraHeaders.add(extraHeader);
    }

    /**
     * Remove any existing instances of the specified header and add it to the end of the list.
     * Be warned that this may not be very fast.
     *
     * @param extraHeader the header to add.  Must not be null.
     */
    public void replaceExtraHeader(HttpHeader extraHeader) {
        if (extraHeaders == null || extraHeaders.size() < 1) {
            addExtraHeader(extraHeader);
            return;
        }

        // First remove any existing ones
        final String name = extraHeader.getName();
        List<HttpHeader> keepers = new ArrayList<HttpHeader>();
        for (HttpHeader header : extraHeaders) {
            if (!header.getName().equalsIgnoreCase(name))
                keepers.add(header);
        }

        keepers.add(extraHeader);
        extraHeaders = keepers;
    }
}
