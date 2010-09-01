/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.util.URIUtil;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * GenericHttpClient driver for the Apache Commons HTTP client.
 */
public class CommonsHttpClient implements RerunnableGenericHttpClient {
    private static final Logger logger = Logger.getLogger(CommonsHttpClient.class.getName());
    public static final String PROP_MAX_CONN_PER_HOST = CommonsHttpClient.class.getName() + ".maxConnectionsPerHost";
    public static final String PROP_MAX_TOTAL_CONN = CommonsHttpClient.class.getName() + ".maxTotalConnections";
    public static final String PROP_STALE_CHECKS = CommonsHttpClient.class.getName() + ".staleCheckCount";
    public static final String PROP_HTTP_EXPECT_CONTINUE = CommonsHttpClient.class.getName() + ".useExpectContinue";
    public static final String PROP_HTTP_DISABLE_KEEP_ALIVE = CommonsHttpClient.class.getName() + ".noKeepAlive";
    public static final String PROP_DEFAULT_CONNECT_TIMEOUT = CommonsHttpClient.class.getName() + ".defaultConnectTimeout";
    public static final String PROP_DEFAULT_READ_TIMEOUT = CommonsHttpClient.class.getName() + ".defaultReadTimeout";
    public static final String PROP_CREDENTIAL_CHARSET = CommonsHttpClient.class.getName() + ".credentialCharset";
    public static final String PROP_GZIP_STREAMING_THRESHOLD = CommonsHttpClient.class.getName() + ".gzipStreamThreshold";

    public static final String DEFAULT_CREDENTIAL_CHARSET = "ISO-8859-1"; // see bugzilla #5729
    public static final int DEFAULT_CONNECT_TIMEOUT = SyspropUtil.getInteger(PROP_DEFAULT_CONNECT_TIMEOUT, 30000);
    public static final int DEFAULT_READ_TIMEOUT = SyspropUtil.getInteger(PROP_DEFAULT_READ_TIMEOUT, 60000);
    public static final int DEFAULT_GZIP_STREAMING_THRESHOLD = Integer.MAX_VALUE;

    private static HttpParams httpParams;
    private static final Map<SSLSocketFactory, Protocol> protoBySockFac = Collections.synchronizedMap(new WeakHashMap<SSLSocketFactory, Protocol>());

    /**
     * This property was true in 5.1, switched to false in 5.2, URLs should be encoded by the caller (see bug 7598).
     */
    private static final boolean encodePath = SyspropUtil.getBoolean(CommonsHttpClient.class.getName() + ".encodePath", false);
    private static final int gzipThreshold = SyspropUtil.getInteger( PROP_GZIP_STREAMING_THRESHOLD, DEFAULT_GZIP_STREAMING_THRESHOLD );

    static {
        DefaultHttpParams.setHttpParamsFactory(new CachingHttpParamsFactory(new DefaultHttpParamsFactory()));

        HttpParams defaultParams = DefaultHttpParams.getDefaultParams();
        if (SyspropUtil.getString(PROP_HTTP_EXPECT_CONTINUE, null) != null) {
            defaultParams.setBooleanParameter("http.protocol.expect-continue", SyspropUtil.getBoolean(PROP_HTTP_EXPECT_CONTINUE));
        }
        if (SyspropUtil.getBoolean(PROP_HTTP_DISABLE_KEEP_ALIVE)) {
            defaultParams.setParameter("http.default-headers", Collections.singletonList(new Header("Connection", "close")));
        }
    }

    private final HttpConnectionManager cman;
    private final int timeout;
    private final Object identity;
    private final boolean isBindingManager;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;

    public CommonsHttpClient() {
        this(newConnectionManager(), -1, -1, null);
    }

    public CommonsHttpClient(HttpConnectionManager cman) {
        this(cman, -1, -1, null); // default timeouts
    }

    public CommonsHttpClient(HttpConnectionManager cman, int connectTimeout, int timeout) {
        this(cman, connectTimeout, timeout, null);
    }

    /**
     * Note: Do not use a proxy at the same time as identity-bound connections.
     *
     * @param proxyHost  hostname or string IP address of proxy server, or null to disable proxy.
     * @param proxyPort   proxy port, or -1 for default
     * @param proxyUsername  proxy username, or null for no proxy authentication
     * @param proxyPassword  proxy password
     */
    public CommonsHttpClient(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
        this(proxyHost, proxyPort, proxyUsername, proxyPassword, newConnectionManager(), -1, -1, null);
    }

    /**
     * Note: Do not use a proxy at the same time as identity-bound connections.
     *
     * @param proxyHost  hostname or string IP address of proxy server, or null to disable proxy.
     * @param proxyPort   proxy port, or -1 for default
     * @param proxyUsername  proxy username, or null for no proxy authentication
     * @param proxyPassword  proxy password
     * @param cman custom connection manager.  Required.
     */
    public CommonsHttpClient(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, HttpConnectionManager cman) {
        this(proxyHost, proxyPort, proxyUsername, proxyPassword, cman, -1, -1, null); // default timeouts
    }

    /**
     * Note: Do not use a proxy at the same time as identity-bound connections.
     *
     * @param proxyHost  hostname or string IP address of proxy server, or null to disable proxy.
     * @param proxyPort   proxy port, or -1 for default
     * @param proxyUsername  proxy username, or null for no proxy authentication
     * @param proxyPassword  proxy password
     * @param cman custom connection manager.  Required.
     * @param connectTimeout  connect timeout, or -1 for default
     * @param timeout   read timeout, or -1 for default
     */
    public CommonsHttpClient(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, HttpConnectionManager cman, int connectTimeout, int timeout) {
        this(proxyHost, proxyPort, proxyUsername, proxyPassword, cman, connectTimeout, timeout, null);
    }

    public CommonsHttpClient(HttpConnectionManager cman, int connectTimeout, int timeout, Object identity) {
        this(null, -1, null, null, cman, connectTimeout, timeout, identity);
    }

    /**
     * Note: Do not use a proxy at the same time as identity-bound connections.
     *
     * @param proxyHost  hostname or string IP address of proxy server, or null to disable proxy.  Must be null if identity is provided.
     * @param proxyPort   proxy port, or -1 for default
     * @param proxyUsername  proxy username, or null for no proxy authentication
     * @param proxyPassword  proxy password
     * @param cman custom connection manager.  Required.
     * @param connectTimeout  connect timeout, or -1 for default
     * @param timeout   read timeout, or -1 for default
     * @param identity  identity to bind, or null.  Must be null if a proxyHost is provided.
     */
    CommonsHttpClient(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, HttpConnectionManager cman, int connectTimeout, int timeout, Object identity) {
        this.cman = cman;
        int connectionTimeout = connectTimeout <= 0 ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        this.timeout = timeout <= 0 ? DEFAULT_READ_TIMEOUT : timeout;
        this.identity = identity;
        this.isBindingManager = cman instanceof IdentityBindingHttpConnectionManager;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        if (identity != null && proxyHost != null)
            throw new IllegalArgumentException("Unable to use a binding identity and a proxy host at the same time");

        HttpConnectionManagerParams params = cman.getParams();
        params.setConnectionTimeout(connectionTimeout);
        params.setSoTimeout(this.timeout);
    }

    public static MultiThreadedHttpConnectionManager newConnectionManager(int maxConnectionsPerHost, int maxTotalConnections) {
        MultiThreadedHttpConnectionManager hcm = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams hcmp = new SingleHostHttpConnectionManagerParams(maxConnectionsPerHost, maxTotalConnections);
        hcm.setParams(hcmp);

        return hcm;
    }

    public static MultiThreadedHttpConnectionManager newConnectionManager() {
        int maxConnPerHost = getDefaultMaxConnectionsPerHost();
        int maxTotalConnections = getDefaultMaxTotalConnections();
        return newConnectionManager(maxConnPerHost, maxTotalConnections);
    }

    public static int getDefaultMaxConnectionsPerHost() {
        return SyspropUtil.getIntegerCached(PROP_MAX_CONN_PER_HOST, 200);
    }

    public static int getDefaultMaxTotalConnections() {
        return SyspropUtil.getIntegerCached(PROP_MAX_TOTAL_CONN, 2000);
    }

    public static int getDefaultStaleCheckCount() {
        return SyspropUtil.getIntegerCached(PROP_STALE_CHECKS, 1);
    }

    @Override
    public GenericHttpRequest createRequest( final HttpMethod method,
                                             final GenericHttpRequestParams params )
            throws GenericHttpException
    {
        stampBindingIdentity();
        final HostConfiguration hconf;
        final URL targetUrl = params.getTargetUrl();
        final String targetProto = targetUrl.getProtocol();
        if ("https".equals(targetProto)) {
            final SSLSocketFactory sockFac = params.getSslSocketFactory();
            final HostnameVerifier hostVerifier = params.getHostnameVerifier();
            if (sockFac != null) {
                hconf = getHostConfig(targetUrl, sockFac, hostVerifier);
            } else
                hconf = null;
        } else
            hconf = null;

        final HttpClient client = new HttpClient(cman);

        final HttpClientParams clientParams = client.getParams();
        clientParams.setDefaults(getOrBuildCachingHttpParams(clientParams.getDefaults()));
        clientParams.setAuthenticationPreemptive(false);

        final boolean useHttp1_0 = params.getHttpVersion() == GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0;

        // Note that we only set if there is a non-default value specified
        // this allows the system wide default to be used for the bridge
        if (params.isUseExpectContinue() && !useHttp1_0) { // ignore expect-continue unless > HTTP 1.0
            clientParams.setBooleanParameter("http.protocol.expect-continue", Boolean.valueOf(params.isUseExpectContinue()));
        }
        if (!params.isUseKeepAlives() && !useHttp1_0) {
            // default is to persist so add close
            clientParams.setParameter("http.default-headers", Collections.singletonList(new Header("Connection", "close")));
        } else if (params.isUseKeepAlives() && useHttp1_0) {
            // default is to close so add keep-alive
            clientParams.setParameter("http.default-headers", Collections.singletonList(new Header("Connection", "keep-alive")));
        }

        final HttpState state = getHttpState(client, params);
        if (proxyUsername != null && proxyUsername.length() > 0)
            state.setProxyCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxyUsername, proxyPassword));

        // NOTE: Use the FILE part of the url here (path + query string), if we use the full URL then
        //       we end up with the default socket factory for the protocol
        final org.apache.commons.httpclient.HttpMethod httpMethod;
        switch (method) {
            case POST:
                httpMethod = new PostMethod(encodePathAndQuery(targetUrl.getFile()));
                break;
            case GET:
                httpMethod = new GetMethod(encodePathAndQuery(targetUrl.getFile()));
                break;
            case PUT:
                httpMethod = new PutMethod(encodePathAndQuery(targetUrl.getFile()));
                break;
            case DELETE:
                httpMethod = new DeleteMethod(encodePathAndQuery(targetUrl.getFile()));
                break;
            case HEAD:
                httpMethod = new HeadMethod(encodePathAndQuery(targetUrl.getFile()));
                break;
            default:
                throw new IllegalStateException("Method " + method + " not supported");
        }

        httpMethod.setFollowRedirects(params.isFollowRedirects());
        final HttpMethodParams methodParams = httpMethod.getParams();
        methodParams.setVersion(useHttp1_0 ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1);
        methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        methodParams.setSoTimeout(timeout);

        final PasswordAuthentication pw = params.getPasswordAuthentication();
        final NtlmAuthentication ntlm = params.getNtlmAuthentication();
        if (ntlm != null) {
            httpMethod.setDoAuthentication(true);
            state.setCredentials(AuthScope.ANY,
                new NTCredentials(
                    ntlm.getUsername(),
                    new String(ntlm.getPassword()),
                    ntlm.getHost(),
                    ntlm.getDomain()
                )
            );
        } else if (pw != null) {
            httpMethod.setDoAuthentication(true);
            String username = pw.getUserName();
            char[] password = pw.getPassword();
            state.setCredentials(AuthScope.ANY,
                                 new UsernamePasswordCredentials(username, new String(password)));
            clientParams.setAuthenticationPreemptive(params.isPreemptiveAuthentication());
            clientParams.setCredentialCharset(SyspropUtil.getStringCached(PROP_CREDENTIAL_CHARSET, DEFAULT_CREDENTIAL_CHARSET));
        }

        final Long contentLen = params.getContentLength();
        if ( (httpMethod instanceof PostMethod || httpMethod instanceof PutMethod) && contentLen != null) {
            if (contentLen > Integer.MAX_VALUE)
                throw new GenericHttpException("Content-Length is too long -- maximum supported is " + Integer.MAX_VALUE);
        }

        final List<HttpHeader> headers = params.getExtraHeaders();
        for (HttpHeader header : headers) {
            doBinding(header);
            httpMethod.addRequestHeader(header.getName(), header.getFullValue());
        }

        if ( params.isGzipEncode() ) {
            httpMethod.addRequestHeader( HttpConstants.HEADER_CONTENT_ENCODING, "gzip" );       
        }

        final ContentTypeHeader rct = params.getContentType();
        if (rct != null && (httpMethod instanceof PostMethod || httpMethod instanceof PutMethod)) {
            httpMethod.addRequestHeader(MimeUtil.CONTENT_TYPE, rct.getFullValue());
        }

        return new RerunnableHttpRequest() {
            private org.apache.commons.httpclient.HttpMethod method = httpMethod;
            private boolean requestEntitySet = false;

            @Override
            public void setInputStream(final InputStream bodyInputStream) {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");
                if (!(method instanceof PostMethod || method instanceof PutMethod))
                    throw new UnsupportedOperationException("Only POST or PUT requests require a body InputStream");
                if (requestEntitySet)
                    throw new IllegalStateException("Request entity already set!");
                requestEntitySet = true;

                final EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod)method;
                entityEnclosingMethod.setRequestEntity(
                        new CompressableRequestEntity(rct, contentLen, params.isGzipEncode(), bodyInputStream) );
            }

            @Override
            public void addParameter(String paramName, String paramValue) throws IllegalArgumentException, IllegalStateException {
                if (method == null) {
                    logger.warning("addParam is called before method is assigned");
                    throw new IllegalStateException("the http method object is not yet assigned");
                }
                if (method instanceof PostMethod) {
                    PostMethod post = (PostMethod)method;
                    post.addParameter(paramName, paramValue);
                } else {
                    logger.warning("addParam is called but the internal method is not post : " +
                                   method.getClass().getName());
                    throw new IllegalStateException("not a post");
                }
            }

            @Override
            public void setInputStreamFactory(final InputStreamFactory inputStreamFactory) {
                if (inputStreamFactory == null)
                    throw new IllegalArgumentException("inputStreamFactory must not be null");
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");
                if (!(method instanceof PostMethod || method instanceof PutMethod))
                    throw new UnsupportedOperationException("Only POST or PUT requests require a body InputStream");
                if (requestEntitySet)
                    throw new IllegalStateException("Request entity already set!");
                requestEntitySet = true;

                final EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod)method;
                entityEnclosingMethod.setRequestEntity(
                        new CompressableRequestEntity(rct, contentLen, params.isGzipEncode(), inputStreamFactory) );
            }

            @Override
            public GenericHttpResponse getResponse() throws GenericHttpException {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");

                stampBindingIdentity();
                final int status;
                final ContentTypeHeader contentType;
                final Long contentLength;
                try {
                    if (hconf == null) {
                        HostConfiguration hc = new HostConfiguration(client.getHostConfiguration());
                        hc.setHost(targetUrl.getHost(), targetUrl.getPort());
                        if (proxyHost != null)
                            hc.setProxy(proxyHost, proxyPort);
                        status = client.executeMethod(hc , method, state);
                    }
                    else {
                        status = client.executeMethod(hconf, method, state);
                    }
                    Header cth = method.getResponseHeader(MimeUtil.CONTENT_TYPE);
                    contentType = cth == null || cth.getValue() == null ? null : ContentTypeHeader.parseValue(cth.getValue());
                    Header clh = method.getResponseHeader(MimeUtil.CONTENT_LENGTH);
                    contentLength = clh == null || clh.getValue() == null ? null : MimeHeader.parseNumericValue(clh.getValue());
                } catch (IOException e) {
                    throw new GenericHttpException("Unable to obtain HTTP response from " + getTarget(method) + ": " + ExceptionUtils.getMessage(e), e);
                } catch (NumberFormatException e) {
                    throw new GenericHttpException("Unable to obtain HTTP response from " + getTarget(method) + ", invalid content length: " + ExceptionUtils.getMessage(e), e);
                }

                final GenericHttpResponse genericHttpResponse = new GenericHttpResponse() {
                    private org.apache.commons.httpclient.HttpMethod response = method; { method = null; } // Take ownership of the HttpMethod

                    @Override
                    public InputStream getInputStream() throws GenericHttpException {
                        if (response == null)
                            throw new IllegalStateException("This response has already been closed");
                        try {
                            return response.getResponseBodyAsStream();
                        } catch (IOException e) {
                            throw new GenericHttpException(e);
                        }
                    }

                    @Override
                    public int getStatus() {
                        return status;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        if (response == null)
                            throw new IllegalStateException("This response has already been closed");
                        Header[] in = response.getResponseHeaders();
                        List<HttpHeader> out = new ArrayList<HttpHeader>();
                        for (Header header : in) {
                            out.add(new GenericHttpHeader(header.getName(), header.getValue()));
                        }
                        return new GenericHttpHeaders(out.toArray(new HttpHeader[out.size()]));
                    }

                    @Override
                    public ContentTypeHeader getContentType() {
                        return contentType;
                    }

                    @Override
                    public Long getContentLength() {
                        return contentLength;
                    }

                    @Override
                    public void close() {
                        if (response != null) {
                            response.releaseConnection();
                            response = null;
                        }
                    }
                };
                method = null; // just in case
                return genericHttpResponse;
            }

            @Override
            public void close() {
                stampBindingIdentity();
                if (method != null) {
                    method.releaseConnection();
                    method = null;
                }
            }

            private String getTarget( final org.apache.commons.httpclient.HttpMethod method ) {
                String target = null;
                try {
                    target = method.getURI().toString();
                } catch ( URIException e1) {
                    logger.log( Level.WARNING, "cannot get URI", e1);
                }
                return target;
            }
        };
    }

    private String encodePathAndQuery( final String unencoded ) {
        String encoded = unencoded;

        if ( encodePath ) {
            try {
                encoded = URIUtil.encodePathQuery( unencoded );
            } catch ( URIException e ) {
                // if this occurs it means the default character set is not supported
                logger.log( Level.WARNING, "Error encoding URL path.", e );
            }
        }

        return encoded;
    }

    private void stampBindingIdentity() {
        if (isBindingManager) {
            IdentityBindingHttpConnectionManager bcm =
                    (IdentityBindingHttpConnectionManager) cman;

            bcm.setId(identity);
        }
    }

    private void doBinding(HttpHeader header) {
        if (isBindingManager) {
            IdentityBindingHttpConnectionManager bcm =
                    (IdentityBindingHttpConnectionManager) cman;

            if (HttpConstants.HEADER_AUTHORIZATION.equalsIgnoreCase(header.getName())) {
                String value = header.getFullValue();
                if(value!=null) {
                    value = value.trim();
                    if(!value.startsWith("Basic") && !value.startsWith("Digest")) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Binding authorization header '"+value+"'.");
                        }
                        bcm.bind();
                    }
                }
            }
        }
    }

    private HttpState getHttpState(HttpClient client, GenericHttpRequestParams params) {
        HttpState httpState;
        if(params.getState()==null) {
            // use per client http state (standard behaviour)
            httpState = client.getState();
        }
        else {
            // use caller managed http state scoping
            GenericHttpState genericState = params.getState();
            httpState = (HttpState) genericState.getStateObject();
        }
        return httpState;
    }

    private static HttpParams getOrBuildCachingHttpParams(final HttpParams params) {
        HttpParams defaultParams = httpParams;

        if (defaultParams == null) {
            defaultParams = new CachingHttpParams(params);
            httpParams = defaultParams;
        }

        return defaultParams;
    }

    private HostConfiguration getHostConfig(final URL targetUrl, final SSLSocketFactory sockFac, final HostnameVerifier hostVerifier) {
        HostConfiguration hconf;
        Protocol protocol = protoBySockFac.get(sockFac);
        if (protocol == null) {
            logger.finer("Creating new commons Protocol for https");
            protocol = new Protocol("https", (ProtocolSocketFactory) new SecureProtocolSocketFactory() {
                @Override
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return verify(sockFac.createSocket(socket, host, port, autoClose), host);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                    return verify(sockFac.createSocket(host, port, clientAddress, clientPort), host);
                }

                @Override
                public Socket createSocket(String host, int port) throws IOException {
                    return verify(sockFac.createSocket(host, port), host);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort, HttpConnectionParams httpConnectionParams) throws IOException {
                    Socket socket = sockFac.createSocket();
                    int connectTimeout = httpConnectionParams.getConnectionTimeout();

                    socket.bind(new InetSocketAddress(clientAddress, clientPort));

                    try {
                        socket.connect(new InetSocketAddress(host, port), connectTimeout);
                    }
                    catch(SocketTimeoutException ste) {
                        throw new ConnectTimeoutException("Timeout when connecting to host '"+host+"'.", ste);
                    }

                    return verify(socket, host);
                }

                private Socket verify(Socket socket, String host) throws IOException {
                    if (socket instanceof SSLSocket) {
                        configureEnabledProtocolsAndCiphers((SSLSocket) socket);

                        if (hostVerifier != null) {
                            SSLSocket sslSocket = (SSLSocket) socket;

                            // must start handshake or any exception can be lost when
                            // getSession() is called
                            sslSocket.startHandshake();

                            if (!hostVerifier.verify(host, sslSocket.getSession())) {
                                ResourceUtils.closeQuietly(socket);
                                throw new CausedIOException("Host name does not match certificate '" + host + "'.");
                            }
                        }
                    }
                    return socket;
                }
            }, 443);
            protoBySockFac.put(sockFac, protocol);
        }
        final HttpHost httpHost = new HttpHost(targetUrl.getHost(), targetUrl.getPort(), protocol);
        hconf = new HostConfiguration(){
            @Override
            public void setHost( final org.apache.commons.httpclient.URI uri ) {
                // This prevents our SSL settings being lost on redirects (bug 9063)
                if ( "https".equalsIgnoreCase( uri.getScheme() ) ) {
                    try {
                        super.setHost( new HttpHost(uri.getHost(), uri.getPort(), httpHost.getProtocol() ));
                    } catch(URIException e) {
                        // This is how HTTPClient handles this condition
                        throw new IllegalArgumentException(e.toString());
                    }
                } else {
                    super.setHost( uri );
                }
            }
        };
        hconf.setHost(httpHost);
        if (proxyHost != null)
            hconf.setProxy(proxyHost, proxyPort);
        return hconf;
    }

    private void configureEnabledProtocolsAndCiphers(SSLSocket s) {
        String[] prots = getCommaDelimitedSystemProperty("https.protocols");
        String[] suites = getCommaDelimitedSystemProperty("https.cipherSuites");
        if (prots != null)
            s.setEnabledProtocols(prots);
        if (suites != null)
            s.setEnabledCipherSuites(suites);
    }

    private static final Pattern commasWithWhitespace = Pattern.compile("\\s*,\\s*");

    private static String[] getCommaDelimitedSystemProperty(String propertyName) {
        String delimited = SyspropUtil.getStringCached(propertyName, null);
        return delimited == null || delimited.length() < 1 ? null : commasWithWhitespace.split(delimited);
    }

    /**
     * HTTP Client RequestEntity that supports compression.
     *
     * <p>If the request entity is within the threshold it will be encoded
     * before transmission and the content length set accordingly. If the
     * entity exceeds the threshold then the GZIP encoding will be applied
     * to the stream. In this case HTTP 1.1 will use a chunked transfer
     * encoding and HTTP 1.0 will fail with an exception.</p>
     */
    private static final class CompressableRequestEntity implements RequestEntity {
        private final ContentTypeHeader contentTypeHeader;
        private final boolean gzipCompress;
        private final Long uncompressedContentLength;
        private final InputStream inputStream;
        private final RerunnableHttpRequest.InputStreamFactory inputStreamFactory;
        private long requestContentLength;
        private byte[] compressedData;
        private IOException compressionException;

        CompressableRequestEntity( final ContentTypeHeader contentTypeHeader,
                                   final Long contentLength,
                                   final boolean gzipCompress,
                                   final InputStream inputStream ) {
            this.contentTypeHeader = contentTypeHeader;
            this.uncompressedContentLength = contentLength;
            this.gzipCompress = gzipCompress;
            this.inputStream = inputStream;
            this.inputStreamFactory = null;
        }

        CompressableRequestEntity( final ContentTypeHeader contentTypeHeader,
                                   final Long contentLength,
                                   final boolean gzipCompress,
                                   final RerunnableHttpRequest.InputStreamFactory inputStreamFactory ) {
            this.contentTypeHeader = contentTypeHeader;
            this.uncompressedContentLength = contentLength;
            this.gzipCompress = gzipCompress;
            this.inputStream = null;
            this.inputStreamFactory = inputStreamFactory;
        }

        @Override
        public boolean isRepeatable() {
            checkCompress();
            return inputStreamFactory != null || compressedData != null;
        }

        @Override
        public void writeRequest( final OutputStream outputStream ) throws IOException {
            checkCompress();
            if ( compressedData != null ) {
                IOUtils.copyStream( new ByteArrayInputStream(getCompressedData()), outputStream );
            } else if ( inputStreamFactory != null ) {
                InputStream inputStream = null;
                try {
                    inputStream = inputStreamFactory.getInputStream();
                    copyStream(inputStream, outputStream);
                } finally {
                    ResourceUtils.closeQuietly( inputStream );
                }
            } else {
                copyStream(inputStream, outputStream);
            }
        }

        @Override
        public long getContentLength() {
            checkCompress();
            return requestContentLength;
        }

        @Override
        public String getContentType() {
            return contentTypeHeader != null ? contentTypeHeader.getFullValue() : null;
        }

        /**
         * Compress on the fly if enabled.
         */
        private void copyStream( final InputStream inputStream,
                                 final OutputStream outputStream ) throws IOException {
            if ( gzipCompress ) {
                OutputStream compressOutputStream = null;
                try {
                    compressOutputStream = new GZIPOutputStream(new NonCloseableOutputStream(outputStream));
                    IOUtils.copyStream(inputStream, compressOutputStream);
                    compressOutputStream.flush();
                } finally {
                    ResourceUtils.closeQuietly( compressOutputStream );
                }
            } else {
                IOUtils.copyStream(inputStream, outputStream);
            }
        }

        private byte[] getCompressedData() throws IOException {
            if ( compressionException != null ) throw compressionException;
            return compressedData;
        }

        /**
         * Set compression related values and perform compression if the
         * content is within the threshold.
         */
        private void checkCompress() {
            if ( gzipCompress ) {
                if ( compressedData == null &&
                     uncompressedContentLength != null &&
                      uncompressedContentLength <= gzipThreshold ) {
                    try {
                        if ( inputStreamFactory != null ) {
                            InputStream inputStream = null;
                            try {
                                inputStream = inputStreamFactory.getInputStream();
                                compressedData = IOUtils.compressGzip( inputStream );
                            } finally {
                                ResourceUtils.closeQuietly( inputStream );
                            }
                        } else {
                            compressedData = IOUtils.compressGzip( inputStream );
                        }
                        requestContentLength = compressedData.length;
                    } catch ( IOException ioe ) {
                        compressedData = new byte[0];
                        compressionException = ioe;
                        requestContentLength = uncompressedContentLength;
                    }
                } else if ( compressedData == null ) {
                    requestContentLength = -1; // Chunked
                }
            } else {
                requestContentLength = uncompressedContentLength == null ? -1 : uncompressedContentLength;
            }
        }
    }
}
