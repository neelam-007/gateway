package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Bean that provides information about a pending HTTP request.
 */
public class GenericHttpRequestParams {

    public static enum HttpVersion {
        HTTP_VERSION_1_0("1.0"),
        HTTP_VERSION_1_1("1.1");

        private String value;
        HttpVersion(String value) {
            this.value = value;
        }
        public String getValue() {
            return this.value;
        }
    }

    // NOTE: Add any new fields to the copy constructor
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
    private boolean gzipEncode = false;
    private HttpVersion httpVersion = HttpVersion.HTTP_VERSION_1_1;
    private String virtualHost;
    private int connectionTimeout = -1;
    private int readTimeout = -1;
    private int maxRetries = -1;
    private String proxyHost;
    private int proxyPort;
    private PasswordAuthentication proxyAuthentication;
    private boolean forceIncludeRequestBody;
    private SSLContext sslContext;
    private String methodAsString;

    // NOTE: Add any new fields to the copy constructor

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
    public GenericHttpRequestParams(final URL targetUrl) {
        this();
        this.targetUrl = targetUrl;
    }

    public GenericHttpRequestParams( final URL targetUrl,
                                     final GenericHttpState state) {
        this(targetUrl);
        this.state = state;
    }

    /**
     * Create a new request description that has the same properties as the
     * given GenericHttpRequestParams.
     *
     * @param template a template to copy from
     */
    public GenericHttpRequestParams( final GenericHttpRequestParams template ) {
        targetUrl = template.targetUrl;
        state = template.state;
        passwordAuthentication = template.passwordAuthentication;
        ntlmAuthentication = template.ntlmAuthentication;
        sslSocketFactory = template.sslSocketFactory;
        hostnameVerifier = template.hostnameVerifier;
        contentType = template.contentType;
        contentLength = template.contentLength;
        extraHeaders = template.extraHeaders == null ? null : new ArrayList<HttpHeader>(template.extraHeaders);
        preemptiveAuthentication = template.preemptiveAuthentication;
        followRedirects = template.followRedirects;
        useKeepAlives = template.useKeepAlives;
        useExpectContinue = template.useExpectContinue;
        gzipEncode = template.gzipEncode;
        httpVersion = template.httpVersion;
        connectionTimeout = template.connectionTimeout;
        readTimeout = template.readTimeout;
        maxRetries = template.maxRetries;
        proxyHost = template.proxyHost;
        proxyPort = template.proxyPort;
        proxyAuthentication = template.proxyAuthentication;
        forceIncludeRequestBody = template.forceIncludeRequestBody;
        sslContext = template.sslContext;
        methodAsString = template.methodAsString;
    }

    public GenericHttpState getState() {
        return state;
    }

    public void setState(GenericHttpState state) {
        this.state = state;
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
    public void setTargetUrl(final URL targetUrl) {
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
    public void setPasswordAuthentication(final PasswordAuthentication passwordAuthentication) {
        this.passwordAuthentication = passwordAuthentication;
    }


    public NtlmAuthentication getNtlmAuthentication() {
        return ntlmAuthentication;
    }

    public void setNtlmAuthentication(final NtlmAuthentication ntlmAuthentication) {
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
    public void setPreemptiveAuthentication(final boolean preemptiveAuthentication) {
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
    public void setSslSocketFactory(final SSLSocketFactory sslSocketFactory) {
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
    public void setHostnameVerifier(final HostnameVerifier hostnameVerifier) {
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
    public void setContentType(final ContentTypeHeader contentType) {
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
    public void setContentLength(final Long contentLength) {
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
    public void setFollowRedirects(final boolean followRedirects) {
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
    public void setUseExpectContinue(final boolean useExpectContinue) {
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
    public void setUseKeepAlives(final boolean useKeepAlives) {
        this.useKeepAlives = useKeepAlives;
    }

    /**
     * Should the body be GZIP compressed.
     *
     * @return True to use GZIP compression.
     */
    public boolean isGzipEncode() {
        return gzipEncode;
    }

    /**
     * Should the body be GZIP compressed.
     *
     * @param gzipEncode True to use GZIP compression.
     */
    public void setGzipEncode( final boolean gzipEncode ) {
        this.gzipEncode = gzipEncode;
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
    public void setHttpVersion(final HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * The connection timeout to use if possible.
     *
     * @return The timeout or -1 for not specified.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout( final int connectionTimeout ) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get the read timeout to use if possible.
     *
     * @return The timeout or -1 if not specified.
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout( final int readTimeout ) {
        this.readTimeout = readTimeout;
    }

    /**
     * Get the maximum number of times the request may be retried.
     *
     * <p>This is not for redirects, only for retries on (for example)
     * connection failure.</p>
     *
     * @return The maximum retry count (0 for no retries).
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Set the maximum number of times a request can be retried.
     *
     * @param maxRetries
     */
    public void setMaxRetries( final int maxRetries ) {
        this.maxRetries = maxRetries;
    }

    /**
     * Get the HTTP proxy host to use.
     *
     * @return The proxy host or null.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost( final String proxyHost ) {
        this.proxyHost = proxyHost;
    }

    /**
     * Get the HTTP proxy port to use.
     *
     * @return The port or 0 for none.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort( final int proxyPort ) {
        this.proxyPort = proxyPort;
    }

    /**
     * Get the HTTP proxy authentication to use.
     *
     * @return The authentication or null.
     */
    public PasswordAuthentication getProxyAuthentication() {
        return proxyAuthentication;
    }

    public void setProxyAuthentication( final PasswordAuthentication proxyAuthentication ) {
        this.proxyAuthentication = proxyAuthentication;
    }

    /**
     * Get the HTTP virtual host.
     *
     * @return the virtual host value
     */
    public String getVirtualHost() {
        return virtualHost;
    }

    /**
     * Set the HTTP virtual host value
     *
     * @param virtualHost the virtual Host value
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    /**
     * Set the extra HTTP headers to send with this request, setting them as an array.
     *
     * @param extraHeaders the array of extra HTTP headers to include with this request, or null to set an empty array.
     * @see #getExtraHeaders
     */
    public void setExtraHeaders(final HttpHeader[] extraHeaders) {
        this.extraHeaders = extraHeaders != null ? new ArrayList<HttpHeader>(Arrays.asList(extraHeaders)) : null;
    }

    /**
     * Add an extra HTTP header to send.  It will be added to the end of the extraHeaders.
     * Be warned that this may not be very fast.
     * @param extraHeader an extra header to include in the request
     */
    public void addExtraHeader(final HttpHeader extraHeader) {
        if (extraHeaders == null) extraHeaders = new ArrayList<HttpHeader>();
        extraHeaders.add(extraHeader);
    }

    /**
     * Remove any existing instances of the specified header and add it to the end of the list.
     * Be warned that this may not be very fast.
     *
     * @param extraHeader the header to add.  Must not be null.
     */
    public void replaceExtraHeader(final HttpHeader extraHeader) {
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

    /**
     * Remove any existing extra HttpHeader instances with the specified name.
     * Be warned that this may not be very fast.
     *
     * @param headerName the name of the headers to remove.  Required.
     * @return true iff. any headers were removed.
     */
    public boolean removeExtraHeader(final String headerName) {
        if (extraHeaders == null || extraHeaders.size() < 1)
            return false;

        boolean removed = false;
        List<HttpHeader> keepers = new ArrayList<HttpHeader>();
        for (HttpHeader header : extraHeaders) {
            if (header.getName().equalsIgnoreCase(headerName)) {
                removed = true;
            } else {
                keepers.add(header);
            }
        }
        extraHeaders = keepers;
        return removed;
    }

    /**
     * Resolve the GenericHttpRequestParams applicable for the given url.
     *
     * <p>Parameters may be resolved when an HTTP redirect occurs to obtain new
     * configuration settings for the redirect URL.</p>
     *
     * <p>NOTE: The target url for the resolved parameters may not match the
     * given url.</p>
     *
     * @param url The URL to resolve
     * @return The params for the URL (which may be the current params)
     */
    public GenericHttpRequestParams resolve( final URL url ) {
        return this;
    }

    /**
     * Check whether the request should attempt to include a request body even if the method is one
     * that normally would not include a request body (eg, DELETE).
     *
     * @return true if we want to forcibly include a body with this request.
     */
    public boolean isForceIncludeRequestBody() {
        return forceIncludeRequestBody;
    }

    public void setForceIncludeRequestBody(boolean forceIncludeRequestBody) {
        this.forceIncludeRequestBody = forceIncludeRequestBody;
    }

    /**
     * Check if the specified method should include a request body, given the current parameters.
     *
     * @param method the method to examine.  Required.
     * @return true if the method normally supports a request body, or if the current parameters are configured
     *          to force including a request body and the method allows this to be forced.
     */
    public boolean needsRequestBody(@NotNull HttpMethod method) {
        return method.needsRequestBody() || (isForceIncludeRequestBody() && method.canForceIncludeRequestBody());
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * Get a method string to use for HttpMethod.OTHER.
     *
     * @return a custom method string, or null.
     */
    public String getMethodAsString() {
        return methodAsString;
    }

    /**
     * Set a method string to use for HttpMethod.OTHER.
     *
     * @param methodAsString a custom method string, or null.
     */
    public void setMethodAsString(String methodAsString) {
        this.methodAsString = methodAsString;
    }
}
