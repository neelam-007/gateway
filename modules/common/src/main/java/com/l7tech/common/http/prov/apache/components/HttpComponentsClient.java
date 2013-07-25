package com.l7tech.common.http.prov.apache.components;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.http.prov.apache.IdentityBindingHttpConnectionManager2;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.common.io.SocketWrapper;
import com.l7tech.common.io.UnsupportedTlsVersionsException;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.*;
import org.apache.http.*;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.*;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 4/3/13
 *
 */
public class HttpComponentsClient implements RerunnableGenericHttpClient{
    private static final Logger logger = Logger.getLogger(HttpComponentsClient.class.getName());
    private static final Logger traceLogger = Logger.getLogger( "com.l7tech.server.routing.http.trace");
    private static final Logger traceSecureLogger = Logger.getLogger("com.l7tech.server.routing.https.trace");
    public static final String PROP_MAX_CONN_PER_HOST = CommonsHttpClient.class.getName() + ".maxConnectionsPerHost";
    public static final String PROP_MAX_TOTAL_CONN = CommonsHttpClient.class.getName() + ".maxTotalConnections";
    public static final String PROP_HTTP_EXPECT_CONTINUE = CommonsHttpClient.class.getName() + ".useExpectContinue";
    public static final String PROP_HTTP_DISABLE_KEEP_ALIVE = CommonsHttpClient.class.getName() + ".noKeepAlive";
    public static final String PROP_DEFAULT_CONNECT_TIMEOUT = CommonsHttpClient.class.getName() + ".defaultConnectTimeout";
    public static final String PROP_DEFAULT_READ_TIMEOUT = CommonsHttpClient.class.getName() + ".defaultReadTimeout";
    public static final String PROP_CREDENTIAL_CHARSET = CommonsHttpClient.class.getName() + ".credentialCharset";
    public static final String PROP_GZIP_STREAMING_THRESHOLD = CommonsHttpClient.class.getName() + ".gzipStreamThreshold";

    public static final String DEFAULT_CREDENTIAL_CHARSET = "ISO-8859-1"; // see bugzilla #5729
    public static final int DEFAULT_CONNECT_TIMEOUT = ConfigFactory.getIntProperty( PROP_DEFAULT_CONNECT_TIMEOUT, 30000 );
    public static final int DEFAULT_READ_TIMEOUT = ConfigFactory.getIntProperty( PROP_DEFAULT_READ_TIMEOUT, 60000 );
    public static final int DEFAULT_GZIP_STREAMING_THRESHOLD = Integer.MAX_VALUE;

    private static final String PROTOCOL_HTTPS = "https";
    private static final String PROTOCOL_HTTP = "http";

    public static final Pattern INTERNET_PATTERN = Pattern.compile("([a-zA-Z0-9._%-]+)@([a-zA-Z0-9.-]+(\\\\.[a-zA-Z0-9.-])*)");
    public static final Pattern NETBIOS_PATTERN = Pattern.compile("([^\\*/?\":|+]+)\\\\([^\\*/?\":|+]+)");

//    private static final Map<SSLSocketFactory, Protocol> protoBySockFac = Collections.synchronizedMap(new WeakHashMap<SSLSocketFactory, Protocol>());

    //TODO: use http cache to cache parameters instead of static field
    private static HttpParams defaultHttpParams;

    /**
     * This property was true in 5.1, switched to false in 5.2, URLs should be encoded by the caller (see bug 7598).
     */
    private static final boolean encodePath = ConfigFactory.getBooleanProperty( CommonsHttpClient.class.getName() + ".encodePath", false );
    private static final int gzipThreshold = ConfigFactory.getIntProperty(PROP_GZIP_STREAMING_THRESHOLD, DEFAULT_GZIP_STREAMING_THRESHOLD);
    private static final boolean enableTrace = ConfigFactory.getBooleanProperty(CommonsHttpClient.class.getName() + ".enableTrace", true);

    private static final SchemeSocketFactory traceSocketFactory = new TraceSocketFactory();

    static{
        defaultHttpParams = new BasicHttpParams();
        DefaultHttpClient.setDefaultHttpParams(defaultHttpParams);//set initial defaults  such as Protocol version, user-agent, etc.

        if ( ConfigFactory.getProperty( PROP_HTTP_EXPECT_CONTINUE ) != null) {
            HttpProtocolParams.setUseExpectContinue(defaultHttpParams, ConfigFactory.getBooleanProperty(PROP_HTTP_EXPECT_CONTINUE, false));
            //defaultParams.setBooleanParameter( "http.protocol.expect-continue", ConfigFactory.getBooleanProperty( PROP_HTTP_EXPECT_CONTINUE, false ) );
        }
        if ( ConfigFactory.getBooleanProperty( PROP_HTTP_DISABLE_KEEP_ALIVE, false ) ) {
            defaultHttpParams.setParameter(ClientPNames.DEFAULT_HEADERS, Collections.singletonList(new BasicHeader("Connection", "close")));
            //defaultParams.setParameter( "http.default-headers", Collections.singletonList( new Header( "Connection", "close" ) ) );
        }
    }

    private final ClientConnectionManager cman;
    private final int connectionTimeout;
    private final int timeout;
    private final Object identity;
    private final boolean isBindingManager;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;

    public static ClientConnectionManager newConnectionManager(int maxConnectionsPerHost, int maxTotalConnections) {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        cm.setDefaultMaxPerRoute(maxConnectionsPerHost);
        cm.setMaxTotal(maxTotalConnections);
        return cm;
    }

    public static ClientConnectionManager newConnectionManager() {
        int maxConnPerHost = getDefaultMaxConnectionsPerHost();
        int maxTotalConnections = getDefaultMaxTotalConnections();
        return newConnectionManager(maxConnPerHost, maxTotalConnections);
    }

    public HttpComponentsClient(ClientConnectionManager cman, int connectionTimeout, int timeout) {
        this(cman, null, connectionTimeout, timeout, null, -1, null, null);
    }

    public HttpComponentsClient() {
        this(newConnectionManager(), -1, -1, null);
    }
    public HttpComponentsClient(int connectionTimeout, int timeout, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword){
        this(ClientConnectionManagerFactory.getInstance().createConnectionManager(ClientConnectionManagerFactory.ConnectionManagerType.POOLING, 1, 1000), null, connectionTimeout, timeout, proxyHost, proxyPort, proxyUsername, proxyPassword);
    }

    public HttpComponentsClient(ClientConnectionManager cman, int connectTimeout, int timeout, Object identity) {
        this(cman, identity, connectTimeout, timeout, null, -1, null, null);
    }

    public HttpComponentsClient(ClientConnectionManager cman, Object identity, int connectionTimeout, int timeout, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
        this.cman = cman;
        this.identity = identity;
        this.isBindingManager = cman instanceof IdentityBindingHttpConnectionManager2;
        this.connectionTimeout = connectionTimeout <= 0 ? DEFAULT_CONNECT_TIMEOUT : connectionTimeout;
        this.timeout = timeout <= 0 ? DEFAULT_READ_TIMEOUT : timeout;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    private void configureSocketFactories(HttpClient hc, URL url, GenericHttpRequestParams params) {
        if(PROTOCOL_HTTPS.equalsIgnoreCase(url.getProtocol())){
            final HostnameVerifier hostVerifier = params.getHostnameVerifier();
            Scheme scheme = new Scheme(PROTOCOL_HTTPS, url.getPort() > 0? url.getPort():url.getDefaultPort(), buildSSLSocketFactory(params.getSslSocketFactory(), hostVerifier));
            hc.getConnectionManager().getSchemeRegistry().register(scheme);
        } else {
            if (enableTrace) {
                Scheme scheme = new Scheme(PROTOCOL_HTTP, url.getPort() > 0? url.getPort():url.getDefaultPort(), traceSocketFactory);
                hc.getConnectionManager().getSchemeRegistry().register(scheme);
            }
        }
    }

    private static final class TraceSocketFactory implements SchemeSocketFactory {
        private final SchemeSocketFactory delegate = PlainSocketFactory.getSocketFactory();

        @Override
        public Socket createSocket(HttpParams params) throws IOException {
            return wrapSocket(delegate.createSocket(params), "http", traceLogger);
        }

        @Override
        public Socket connectSocket(Socket sock, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
            return wrapSocket(delegate.connectSocket(sock, remoteAddress, localAddress, params), "http", traceLogger);
        }

        @Override
        public boolean isSecure(Socket sock) throws IllegalArgumentException {
            return delegate.isSecure(sock);
        }
    }


    //TODO: throw exception if sslContext is null
    private SSLSocketFactory buildSSLSocketFactory(javax.net.ssl.SSLSocketFactory socketFactory, final HostnameVerifier verifier) {
        SSLSocketFactory sf = null;

        /* create wraper for hostnameVerifier  */
        X509HostnameVerifier x509HostnameVerifier = new X509HostnameVerifier() {
            @Override
            public void verify(String s, SSLSocket sslSocket) throws IOException {
                // must start handshake or any exception can be lost when
                // getSession() is called
               sslSocket.startHandshake();
                if (verifier != null) {
                    if(!verifier.verify(s, sslSocket.getSession())) {
                        throw new IOException("SSL verification failed!");
                    }
                }
            }

            @Override
            public void verify(String s, X509Certificate x509Certificate) throws SSLException {
                //we do not use this one
            }

            @Override
            public void verify(String s, String[] strings, String[] strings1) throws SSLException {
                //we do not use this one
            }

            @Override
            public boolean verify(String hostname, SSLSession session) {
                if (verifier != null) {
                    return verifier.verify(hostname, session);
                } else {
                  return true;
                }
            }
        };
        if(enableTrace){
            sf = new SecureTraceDirectSocketFactory(socketFactory, x509HostnameVerifier);
        } else {
            sf = new SecureDirectSocketFactory(socketFactory, x509HostnameVerifier);
        }

        return sf;
    }

    /**
     * Create an HTTP request.
     *
     * @param method the request method to use.  May be one of {@link com.l7tech.common.http.HttpMethod#GET} or {@link com.l7tech.common.http.HttpMethod#POST}.
     * @param params the request params.  Must not be null.
     * @return the HTTP request object, ready to proceed.  Never null.
     * @throws com.l7tech.common.http.GenericHttpException
     *          if there is a configuration, network, or HTTP problem.
     */
    @Override
    public GenericHttpRequest createRequest(HttpMethod method, final GenericHttpRequestParams params) throws GenericHttpException {
        stampBindingIdentity();
        final URL targetUrl = params.getTargetUrl();
        final String virtualHost = params.getVirtualHost();
        final DefaultHttpClient client = new DefaultHttpClient(cman);

        final HttpParams clientParams = new DefaultedHttpParams(client.getParams(), defaultHttpParams);
        //TODO:implement caching
        //client.setParams(getOrBuildCachingHttpParams(clientParams));
        client.setParams(clientParams);


        final boolean useHttp1_0 = params.getHttpVersion() == GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0;
        if(useHttp1_0) {
            HttpProtocolParams.setVersion(clientParams, HttpVersion.HTTP_1_0);
        }

        // Note that we only set if there is a non-default value specified
        // this allows the system wide default to be used for the bridge
        if (params.isUseExpectContinue() && !useHttp1_0) { // ignore expect-continue unless > HTTP 1.0
            clientParams.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.valueOf(params.isUseExpectContinue()));//"http.protocol.expect-continue"
        }
        if (!params.isUseKeepAlives() && !useHttp1_0) {
            // default is to persist so add close
            clientParams.setParameter(ClientPNames.DEFAULT_HEADERS, Collections.singletonList(new BasicHeader("Connection", "close")));//"http.default-headers"
        } else if (params.isUseKeepAlives() && useHttp1_0) {
            // default is to close so add keep-alive
            clientParams.setParameter(ClientPNames.DEFAULT_HEADERS, Collections.singletonList(new BasicHeader("Connection", "keep-alive")));//"http.default-headers"
        }
        //TODO: get the stored HttpContext. Potentially we can cache the context instead of storing it in the generic params
        //final HttpState state = getHttpState(client, params);
        final HttpContext state = getHttpState(params);

        String methodString = params.getMethodAsString();
        if (HttpMethod.OTHER.equals(method) && (methodString == null || methodString.trim().length() < 1))
            throw new GenericHttpException("Method name string must be provided for HTTP method OTHER");

        final HttpRequestBase httpMethod = getClientMethod(method, methodString, params, targetUrl);

        configureParameters( clientParams, state, client, httpMethod, params );

        final HttpParams methodParams = httpMethod.getParams();
        methodParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, useHttp1_0 ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1);
        methodParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        if (virtualHost != null && virtualHost.length() > 0) {
            methodParams.setParameter(ClientPNames.VIRTUAL_HOST, virtualHost);
            //if this doesn't work we need to try different approach
            /*
            client.addRequestInterceptor(new HttpRequestInterceptor() {

                public void process(
                        final HttpRequest request,
                        final HttpContext context) throws HttpException, IOException {
                    request.setHeader(ClientPNames.VIRTUAL_HOST, virtualHost);
                }
            });
            in this case virtual host can contain the port that is overridden unlike the previous version
           */

        }

        final Long contentLen = params.getContentLength();
        if ( (httpMethod instanceof HttpPost || httpMethod instanceof HttpPut) && contentLen != null) {
            if (contentLen > (long) Integer.MAX_VALUE )
                throw new GenericHttpException("Content-Length is too long -- maximum supported is " + Integer.MAX_VALUE);
        }

        final List<HttpHeader> headers = params.getExtraHeaders();
        for (HttpHeader header : headers) {
            doBinding(header, state);
            httpMethod.addHeader(header.getName(), header.getFullValue());
        }

        if ( params.isGzipEncode() ) {
            httpMethod.addHeader(HttpConstants.HEADER_CONTENT_ENCODING, "gzip");
        }

        final ContentTypeHeader rct = params.getContentType();
        if (rct != null && (httpMethod instanceof HttpPost || httpMethod instanceof HttpPut)) {
            httpMethod.addHeader(MimeUtil.CONTENT_TYPE, rct.getFullValue());
        }
        //this is where we configure ssl handling
        configureSocketFactories(client, targetUrl, params);

        return new RerunnableHttpRequest() {
            private HttpRequestBase method = httpMethod;
            private boolean requestEntitySet = false;

            @Override
            public void setInputStream(final InputStream bodyInputStream) {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");
                if (!(method instanceof HttpPost || method instanceof HttpPut))
                    throw new UnsupportedOperationException("Only POST or PUT requests require a body InputStream");
                if (requestEntitySet)
                    throw new IllegalStateException("Request entity already set!");
                requestEntitySet = true;

                final HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
                entityEnclosingMethod.setEntity(
                        new CompressableRequestEntity(rct, contentLen, params.isGzipEncode(), bodyInputStream));
            }

            @Override
            public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                if (method == null) {
                    logger.warning("addParam is called before method is assigned");
                    throw new IllegalStateException("the http method object is not yet assigned");
                }
                if (method instanceof HttpPost) {
                    HttpPost post = (HttpPost) method;
                    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
                    for (String[] parameter : parameters) {
                        formParams.add(new BasicNameValuePair(parameter[0], parameter[1]));
                    }
                    try {
                        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams);
                        post.setEntity(entity);
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalArgumentException(e);
                    }
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
                if (!(method instanceof HttpPost || method instanceof HttpPut))
                    throw new UnsupportedOperationException("Only POST or PUT requests require a body InputStream");
                if (requestEntitySet)
                    throw new IllegalStateException("Request entity already set!");
                requestEntitySet = true;

                final HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
                entityEnclosingMethod.setEntity(
                        new CompressableRequestEntity(rct, contentLen, params.isGzipEncode(), inputStreamFactory));
            }

            @Override
            public GenericHttpResponse getResponse() throws GenericHttpException {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");

                HttpComponentsUtils.checkUrl(targetUrl);
                stampBindingIdentity();
                final ContentTypeHeader contentType;
                final Long contentLength;
                try {
                    final HttpResponse httpResponse = client.execute(method, state);
                    Header cth = httpResponse.getFirstHeader(MimeUtil.CONTENT_TYPE);
                    contentType = cth == null || cth.getValue() == null ? null : ContentTypeHeader.create(cth.getValue());
                    Header clh = httpResponse.getFirstHeader(MimeUtil.CONTENT_LENGTH);
                    contentLength = clh == null || clh.getValue() == null ? null : MimeHeader.parseNumericValue(clh.getValue());
                    final GenericHttpResponse genericHttpResponse = new GenericHttpResponse() {
                        private HttpResponse response = httpResponse;
                        private HttpRequestBase requestMethod = method;

                        @Override
                        public InputStream getInputStream() throws GenericHttpException {
                            if (response == null)
                                throw new IllegalStateException("This response has already been closed");
                            try {
                                if (response.getEntity() == null) return null;

                                InputStream rawStream = response.getEntity().getContent();

                                if (useSsljTruncationAttackWorkaround()) {
                                    rawStream = new TruncationAttackSwallowingInputStream(rawStream);
                                }

                                return rawStream;
                            } catch (IOException e) {
                                throw new GenericHttpException(e);
                            }
                        }

                        @Override
                        public int getStatus() {
                            return response.getStatusLine().getStatusCode();
                        }

                        @Override
                        public HttpHeaders getHeaders() {
                            if (response == null)
                                throw new IllegalStateException("This response has already been closed");
                            Header[] in = response.getAllHeaders();
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
                            stampBindingIdentity();
                            if (requestMethod != null) {
                                requestMethod.reset();//release connection
                                requestMethod = null;
                            }
                        }
                    };
                    method = null;//TODO: do we really need this?
                    return genericHttpResponse;

                } catch (UnsupportedTlsVersionsException e) {
                    throw new GenericHttpException("Unable to obtain HTTP response" +  " from " + httpMethod.getURI() + ": " + ExceptionUtils.getMessage(e), e);
                } catch ( SocketTimeoutException e){
                    throw new GenericHttpException("Unable to obtain HTTP response" +" from " + httpMethod.getURI() + ": " + ExceptionUtils.getMessageWithCause(e) + ". Timed out at "+ method.getParams().getLongParameter(CoreConnectionPNames.SO_TIMEOUT, 0L) +"ms", e);
                } catch (ClientProtocolException e) {
                    String error = ValidationUtils.isValidUriString(targetUrl.getFile());
                    if (error == null) error = "";
                    throw new GenericHttpException("Unable to obtain HTTP response" +" from " + httpMethod.getURI() + ": " + ExceptionUtils.getMessageWithCause(e) + ". " + error, e);
                } catch (IOException e) {
                    throw new GenericHttpException("Unable to obtain HTTP response" + " from " + httpMethod.getURI() + ": " + ExceptionUtils.getMessageWithCause(e), e);
                } catch (NumberFormatException e) {
                    throw new GenericHttpException("Unable to obtain HTTP response" + " from " + httpMethod.getURI() + ", invalid content length: " + ExceptionUtils.getMessage(e), e);
                }

            }

            @Override
            public void close() {
                if (method != null) {
                    //Making the connection reusable
                    method.releaseConnection();
                    method = null;
                }
            }
        };
    }

    public static int getDefaultMaxConnectionsPerHost() {
        return ConfigFactory.getIntProperty(PROP_MAX_CONN_PER_HOST, 200);
    }

    public static int getDefaultMaxTotalConnections() {
        return ConfigFactory.getIntProperty(PROP_MAX_TOTAL_CONN, 2000);
    }

    private void stampBindingIdentity() {
        if (isBindingManager) {
            IdentityBindingHttpConnectionManager2 bcm =
                    (IdentityBindingHttpConnectionManager2) cman;

            bcm.setId(identity);
        }
    }

    private void doBinding(HttpHeader header, HttpContext context) {
        if (isBindingManager) {
            if (HttpConstants.HEADER_AUTHORIZATION.equalsIgnoreCase(header.getName())) {
                String value = header.getFullValue();
                if(value!=null) {
                    value = value.trim();
                    if(!value.startsWith("Basic") && !value.startsWith("Digest")) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Binding authorization header '"+value+"'.");
                        }
                        IdentityBindingHttpConnectionManager2 bcm =
                                (IdentityBindingHttpConnectionManager2) cman;
                        bcm.bind(context);
                        context.setAttribute(ClientContext.USER_TOKEN, identity);
                    }
                }
            }
        }
    }

    private void configureProxyAuthentication(CredentialsProvider proxyCredProvider, HttpParams clientParams, String host, int port, String username, String password) {

        // authentication schemes are ordered according to priorities
        List<String> authpref = new ArrayList<String>();
        authpref.add(AuthPolicy.NTLM);
        authpref.add(AuthPolicy.DIGEST);
        authpref.add(AuthPolicy.BASIC);
        clientParams.setParameter(AuthPNames.PROXY_AUTH_PREF, authpref);
        if(proxyHost != null) {
            final HttpHost proxyHost = new HttpHost(host, port);
            clientParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
            proxyCredProvider.setCredentials(new AuthScope(proxyHost,AuthScope.ANY_REALM,"basic"), new UsernamePasswordCredentials(username, password));
            //TODO: add digest authentication
            NTCredentials ntCredentials =  buildNTCredentials(username, password);
            if(ntCredentials != null) {
                proxyCredProvider.setCredentials(new AuthScope(proxyHost, AuthScope.ANY_REALM, "ntlm"),ntCredentials);
            }
        }
        else {
            proxyCredProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        }

    }

    private void configureParameters(HttpParams clientParams, HttpContext state, DefaultHttpClient client, HttpUriRequest httpMethod, GenericHttpRequestParams params) {
        boolean proxyConfigured = false;
        CredentialsProvider proxyCredProvider = new BasicCredentialsProvider();
        if ( params.getProxyHost() != null ) {
            final PasswordAuthentication proxyAuthentication = params.getProxyAuthentication();
            if ( proxyAuthentication != null ) {
                configureProxyAuthentication(proxyCredProvider, clientParams, params.getProxyHost(), params.getProxyPort(), proxyAuthentication.getUserName(), new String(proxyAuthentication.getPassword()));
                client.setCredentialsProvider(proxyCredProvider);
                proxyConfigured = true;
                /*HttpHost proxy = new HttpHost(params.getProxyHost(), params.getProxyPort());
                clientParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                // Choose BASIC over DIGEST for proxy authentication
                List<String> authpref = new ArrayList<String>();
                authpref.add(AuthPolicy.BASIC);
                authpref.add(AuthPolicy.DIGEST);
                clientParams.setParameter(AuthPNames.PROXY_AUTH_PREF,authpref );

                proxyCredProvider.setCredentials(
                        new AuthScope(params.getProxyHost(), params.getProxyPort()),
                        new UsernamePasswordCredentials(proxyAuthentication.getUserName(), new String(proxyAuthentication.getPassword())));*/
            }
        } else if (proxyUsername != null && proxyUsername.length() > 0) {
            configureProxyAuthentication(proxyCredProvider, clientParams, proxyHost, proxyPort, proxyUsername, proxyPassword);
            client.setCredentialsProvider(proxyCredProvider);
            proxyConfigured = true;
            /*           HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        clientParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

        // Choose BASIC over DIGEST for proxy authentication
        List<String> authpref = new ArrayList<String>();
        authpref.add(AuthPolicy.BASIC);
        authpref.add(AuthPolicy.DIGEST);
        clientParams.setParameter(AuthPNames.PROXY_AUTH_PREF,authpref );
        proxyCredProvider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(proxyUsername, proxyPassword));*/
        }

        clientParams.setParameter(ClientPNames.HANDLE_REDIRECTS, params.isFollowRedirects());
//        final HttpMethodParams methodParams = httpMethod.getParams();
        clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, params.getReadTimeout()>=0 ? params.getReadTimeout() : timeout);
//        methodParams.setSoTimeout(params.getReadTimeout()>=0 ? params.getReadTimeout() : timeout);
        clientParams.setParameter(ClientPNames.CONN_MANAGER_TIMEOUT, (long) (params.getConnectionTimeout() >= 0 ? params.getConnectionTimeout() : connectionTimeout));
 //       clientParams.setConnectionManagerTimeout( (long) (params.getConnectionTimeout() >= 0 ? params.getConnectionTimeout() : connectionTimeout) );
        if (params.getMaxRetries() >= 0) {
            client.setHttpRequestRetryHandler( new DefaultHttpRequestRetryHandler(params.getMaxRetries(), false ));
//            methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler( params.getMaxRetries(), false ));
        }

        final PasswordAuthentication pw = params.getPasswordAuthentication();
        final NtlmAuthentication ntlm = params.getNtlmAuthentication();
        if (ntlm != null) {
            NTCredentials creds = new NTCredentials(
                    ntlm.getUsername(),
                    new String(ntlm.getPassword()),
                    ntlm.getHost(),
                    ntlm.getDomain()
            );
            client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
/*            httpMethod.setDoAuthentication(true);
            state.setCredentials(AuthScope.ANY,
                    new NTCredentials(
                            ntlm.getUsername(),
                            new String(ntlm.getPassword()),
                            ntlm.getHost(),
                            ntlm.getDomain()
                    )
            );*/
            //http client does not support preemptive authentication out of the box
//            clientParams.setAuthenticationPreemptive(false);
        } else if (pw != null) {
//            httpMethod.setDoAuthentication(true);
            String username = pw.getUserName();
            char[] password = pw.getPassword();
            UsernamePasswordCredentials creds =  new UsernamePasswordCredentials(username, new String(password));
            client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
/*            state.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, new String(password)));*/
            if(params.isPreemptiveAuthentication()) { //set preemptive authentication
                // Create AuthCache instance
                AuthCache authCache = new BasicAuthCache();
                // Generate BASIC scheme object and add it to the local auth cache
                BasicScheme basicAuth = new BasicScheme();
                HttpHost targetHost = new HttpHost(httpMethod.getURI().getHost(), httpMethod.getURI().getPort(), httpMethod.getURI().getScheme());
                authCache.put(targetHost, basicAuth);
                // Add AuthCache to the execution context
                state.setAttribute(ClientContext.AUTH_CACHE, authCache);
            }
//            clientParams.setAuthenticationPreemptive(params.isPreemptiveAuthentication());
            clientParams.setParameter(AuthPNames.CREDENTIAL_CHARSET, ConfigFactory.getProperty(PROP_CREDENTIAL_CHARSET, DEFAULT_CREDENTIAL_CHARSET));
        } else if ( !proxyConfigured ) {
//            httpMethod.setDoAuthentication(false);
            client.getCredentialsProvider().clear();
            state.removeAttribute(ClientContext.AUTH_CACHE);
//            state.clearCredentials();
//            clientParams.setAuthenticationPreemptive(false);
        }
    }

    private HttpRequestBase getClientMethod(final HttpMethod method, final String methodAsString, final GenericHttpRequestParams params, URL targetUrl) throws GenericHttpException{
        HttpUriRequest httpRequest = null;
        HttpMethod methodType = HttpMethod.POST;
        if(method != null) methodType = method;
        URI uri = null;
        try {
            uri = new URI(targetUrl.getProtocol(), targetUrl.getAuthority(), targetUrl.getPath(), targetUrl.getQuery(), null);
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Invalid URI " + e.getMessage());
            throw new GenericHttpException("Invalid URI " + targetUrl, e);
        }

        if (params.isForceIncludeRequestBody() && params.needsRequestBody(method) &&
                (!HttpMethod.POST.equals(method) && !HttpMethod.PUT.equals(method)))
        {
            // This is a method like GET, DELETE, or HEAD that would not normally be transmitted with a request body, but
            // that is being forced to include one anyway. We will force use of PostMethod (with overridden verb) for this request (Bug #12168)
            return new HttpPost(uri) {
                @Override
                public String getMethod() {
                    return method.getProtocolName();
                }
            };
        }
        switch (methodType) {
            case GET:
                return new HttpGet(uri);
            case POST:
                return new HttpPost(uri);
            case PUT:
                return new HttpPut(uri);
            case DELETE:
                return new HttpDelete(uri);
            case HEAD:
                return new HttpHead(uri);
            case OPTIONS:
                return new HttpOptions(uri);
            //TODO: confirm that patch method does not follow redirects
            case PATCH:
                //RFC 5789 HTTP Patch method
                return new HttpPatch(uri);
            case OTHER:
                if (methodAsString == null)
                    throw new IllegalStateException("Method " + method + " not supported with null methodAsString");
                return new HttpPost(uri) {
                    @Override
                    public String getMethod() {
                        return methodAsString;
                    }
                };
            //TODO: add HttpTrace method
            default:
                throw new IllegalStateException("Method " + method + " not supported");
        }
    }

    @SuppressWarnings("Unused")
    private HttpParams getOrBuildCachingHttpParams(HttpParams params) {
        HttpParams defaultParams = defaultHttpParams;

        if (defaultParams == null) {
            defaultParams = new BasicHttpParams();
            DefaultHttpClient.setDefaultHttpParams(defaultParams);
            defaultHttpParams = defaultParams;
        }

        return defaultParams;
    }

    private HttpContext getHttpState(GenericHttpRequestParams params) {
        HttpContext httpState;
        if(params.getState()==null) {
            // use per client http state (standard behaviour)
            httpState = new BasicHttpContext();
        }
        else {
            // use caller managed http state scoping
            GenericHttpState genericState = params.getState();
            httpState = (HttpContext) genericState.getStateObject();
        }
        return httpState;
    }

    private boolean useSsljTruncationAttackWorkaround() {
        return ConfigFactory.getBooleanProperty("io.https.response.truncationProtection.disable", false);
    }

    private static void configureEnabledProtocolsAndCiphers(SSLSocket s) {
        String[] prots = getCommaDelimitedSystemProperty("https.protocols");
        String[] suites = getCommaDelimitedSystemProperty("https.cipherSuites");
        if (prots != null)
            s.setEnabledProtocols(prots);
        if (suites != null)
            s.setEnabledCipherSuites(suites);
    }

    private static final Pattern commasWithWhitespace = Pattern.compile("\\s*,\\s*");

    private static String[] getCommaDelimitedSystemProperty(String propertyName) {
        String delimited = ConfigFactory.getProperty(propertyName, null);
        return delimited == null || delimited.length() < 1 ? null : commasWithWhitespace.split(delimited);
    }

    private static Socket wrapSocket( final Socket accepted,
                                      final String prefix,
                                      final Logger traceLogger ) {
        if (accepted instanceof SSLSocket) {
            return new SSLSocketWrapper((SSLSocket) accepted) {
                private final SocketWrapper.TraceSupport ts = new SocketWrapper.TraceSupport(accepted, prefix, traceLogger);

                @Override
                public InputStream getInputStream() throws IOException {
                    return ts.getInputStream();
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return ts.getOutputStream();
                }
            };
        } else {
            return new SocketWrapper(accepted) {
                private final TraceSupport ts = new TraceSupport(accepted, prefix, traceLogger);

                @Override
                public InputStream getInputStream() throws IOException {
                    return ts.getInputStream();
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return ts.getOutputStream();
                }
            };
        }
    }

    private static final class CompressableRequestEntity extends AbstractHttpEntity {
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
        public boolean isChunked() {
            return uncompressedContentLength != null && uncompressedContentLength > (long) gzipThreshold;
        }

        @Override
        public void writeTo( final OutputStream outputStream ) throws IOException {
            checkCompress();
            if ( compressedData != null ) {
                IOUtils.copyStream(new ByteArrayInputStream(getCompressedData()), outputStream);
            } else if ( inputStreamFactory != null ) {
                InputStream inputStream = null;
                try {
                    inputStream = inputStreamFactory.getInputStream();
                    copyStream(inputStream, outputStream);
                } finally {
                    ResourceUtils.closeQuietly(inputStream);
                }
            } else {
                copyStream(inputStream, outputStream);
            }
        }

        @Override
        public boolean isStreaming() {
            checkCompress();
            return inputStreamFactory != null || compressedData != null;
        }

        @Override
        public long getContentLength() {
            checkCompress();
            return requestContentLength;
        }

        public Header getContentType() {
            return contentTypeHeader != null ? new BasicHeader(contentTypeHeader.getName(), contentTypeHeader.getFullValue()) : null;
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            return inputStream;
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
                        uncompressedContentLength <= (long) gzipThreshold ) {
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
                        requestContentLength = (long) compressedData.length;
                        setContentEncoding("gzip");
                    } catch ( IOException ioe ) {
                        compressedData = new byte[0];
                        compressionException = ioe;
                        requestContentLength = uncompressedContentLength;
                    }
                } else if ( compressedData == null ) {
                    requestContentLength = -1L; // Chunked
                }
            } else {
                requestContentLength = uncompressedContentLength == null ? -1L : uncompressedContentLength;
            }
        }

    }

    private class TruncationAttackSwallowingInputStream extends FilterInputStream {
        public TruncationAttackSwallowingInputStream(InputStream rawStream) {
            super(rawStream);
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (IOException e) {
                return maybeSwallowIOException(e);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return super.read(b, off, len);
            } catch (IOException e) {
                return maybeSwallowIOException(e);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } catch (IOException e) {
                maybeSwallowIOException(e);
            }
        }

        private int maybeSwallowIOException(IOException e) throws IOException {
            String msg = e.getMessage();
            if (msg == null || !msg.contains("possible truncation attack?"))
                throw e;

            logger.log(Level.INFO, "Ignoring possible response truncation attack");
            return -1;
        }
    }

    private static class SecureTraceDirectSocketFactory extends SecureDirectSocketFactory {

        private SecureTraceDirectSocketFactory(javax.net.ssl.SSLSocketFactory socketfactory, X509HostnameVerifier hostnameVerifier) {
            super(socketfactory, hostnameVerifier);
        }

        @Override
        public Socket createSocket(HttpParams params) throws IOException {
            return wrapSocket(super.createSocket(params), "https", traceSecureLogger);
        }

        @Override
        public Socket connectSocket(Socket socket, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
            return wrapSocket(super.connectSocket(socket, remoteAddress, localAddress, params), "https", traceSecureLogger);
        }

        @Override
        public Socket createLayeredSocket(Socket socket, String host, int port, HttpParams params) throws IOException, UnknownHostException {
            return wrapSocket(super.createLayeredSocket(socket, host, port, params), "https", traceSecureLogger);
        }
    }



    private static class SecureDirectSocketFactory extends SSLSocketFactory {

        private SecureDirectSocketFactory(javax.net.ssl.SSLSocketFactory socketfactory, X509HostnameVerifier hostnameVerifier) {
            super(socketfactory, hostnameVerifier);
        }

        protected void prepareSocket(SSLSocket socket) throws IOException {
            if(socket != null) {
                configureEnabledProtocolsAndCiphers(socket);
            }
        }

    }

    /**
     * builds NTCredentials from account name and password
     * @param acctname
     * @param password
     * @return
     */
    private static NTCredentials buildNTCredentials(String acctname, String password) {
        //TODO: get proper domain from configuration
        Matcher m = INTERNET_PATTERN.matcher(acctname);
        if(m.find()){
            String accountName = m.group(1);
            String domain = m.group(2);
            return new NTCredentials(accountName, password, "", domain);
        }
        else {
            m = NETBIOS_PATTERN.matcher(acctname);
            if(m.find()) {
                String domain = m.group(1);
                String accountName = m.group(2);
                return new NTCredentials(accountName, password, "", domain);
            }
        }
        return null;
    }
}
