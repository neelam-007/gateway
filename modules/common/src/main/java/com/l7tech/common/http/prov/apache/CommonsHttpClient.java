/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger(PROP_DEFAULT_CONNECT_TIMEOUT, 30000).intValue();
    public static final int DEFAULT_READ_TIMEOUT = Integer.getInteger(PROP_DEFAULT_READ_TIMEOUT, 60000).intValue();

    private static HttpParams httpParams;
    private static final Map protoBySockFac = Collections.synchronizedMap(new WeakHashMap());

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
    private final int connectionTimeout;
    private final int timeout;
    private final Object identity;
    private final boolean isBindingManager;

    public CommonsHttpClient() {
        this(newConnectionManager(), -1, -1, null);
    }

    public CommonsHttpClient(HttpConnectionManager cman) {
        this(cman, -1, -1, null); // default timeouts
    }

    public CommonsHttpClient(HttpConnectionManager cman, int connectTimeout, int timeout) {
        this(cman, connectTimeout, timeout, null);
    }

    public CommonsHttpClient(HttpConnectionManager cman, int connectTimeout, int timeout, Object identity) {
        this.cman = cman;
        this.connectionTimeout = connectTimeout <= 0 ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        this.timeout = timeout <= 0 ? DEFAULT_READ_TIMEOUT : timeout;
        this.identity = identity;
        this.isBindingManager = cman instanceof IdentityBindingHttpConnectionManager;

        HttpConnectionManagerParams params = cman.getParams();
        params.setConnectionTimeout(this.connectionTimeout);
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
        int maxConnPerHost = SyspropUtil.getInteger(PROP_MAX_CONN_PER_HOST, 200).intValue();
        return maxConnPerHost;
    }

    public static int getDefaultMaxTotalConnections() {
        int maxTotalConnections = SyspropUtil.getInteger(PROP_MAX_TOTAL_CONN, 2000).intValue();
        return maxTotalConnections;
    }

    public static int getDefaultStaleCheckCount() {
        int maxTotalConnections = SyspropUtil.getInteger(PROP_STALE_CHECKS, 1).intValue();
        return maxTotalConnections;
    }

    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params)
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

        HttpClientParams clientParams = client.getParams();
        clientParams.setDefaults(getOrBuildCachingHttpParams(clientParams.getDefaults()));
        clientParams.setAuthenticationPreemptive(false);

        boolean useHttp1_0 = params.getHttpVersion() == GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0;

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

        // NOTE: Use the FILE part of the url here (path + query string), if we use the full URL then
        //       we end up with the default socket factory for the protocol
        HttpMethod httpMethod = null;
        if (method == POST) {
            httpMethod = new PostMethod(targetUrl.getFile());
        } else if (method == GET) {
            httpMethod = new GetMethod(targetUrl.getFile());
        } else if (method == PUT) {
            httpMethod = new PutMethod(targetUrl.getFile());
        } else if (method == DELETE) {
            httpMethod = new DeleteMethod(targetUrl.getFile());
        } else {
            throw new IllegalStateException("Method " + method + " not supported");
        }

        httpMethod.setFollowRedirects(params.isFollowRedirects());
        HttpMethodParams methodParams = httpMethod.getParams();
        methodParams.setVersion(useHttp1_0 ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1);
        methodParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        methodParams.setSoTimeout(timeout);

        PasswordAuthentication pw = params.getPasswordAuthentication();
        NtlmAuthentication ntlm = params.getNtlmAuthentication();
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
        }

        final Long contentLen = params.getContentLength();
        if (httpMethod instanceof PostMethod && contentLen != null) {
            final long clen = contentLen.longValue();
            if (clen > Integer.MAX_VALUE)
                throw new GenericHttpException("Content-Length is too long -- maximum supported is " + Integer.MAX_VALUE);
        }

        List headers = params.getExtraHeaders();
        for (Iterator i = headers.iterator(); i.hasNext();) {
            HttpHeader header = (HttpHeader)i.next();
            doBinding(header);
            httpMethod.addRequestHeader(header.getName(), header.getFullValue());
        }

        final ContentTypeHeader rct = params.getContentType();
        if (rct != null && (httpMethod instanceof PostMethod || httpMethod instanceof PutMethod)) {
            httpMethod.addRequestHeader(MimeUtil.CONTENT_TYPE, rct.getFullValue());
        }

        final HttpMethod fmtd = httpMethod;
        return new RerunnableHttpRequest() {
            private HttpMethod method = fmtd;
            private boolean requestEntitySet = false;

            public void setInputStream(final InputStream bodyInputStream) {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");
                if (!(method instanceof PostMethod || method instanceof PutMethod))
                    throw new UnsupportedOperationException("Only POST or PUT requests require a body InputStream");
                if (requestEntitySet)
                    throw new IllegalStateException("Request entity already set!");
                requestEntitySet = true;

                if (method instanceof PostMethod) {
                    PostMethod postMethod = (PostMethod)method;
                    postMethod.setRequestEntity(new RequestEntity(){
                        public long getContentLength() {
                            return contentLen != null ? contentLen.longValue() : -1;
                        }

                        public String getContentType() {
                            return rct != null ? rct.getFullValue() : null;
                        }

                        public boolean isRepeatable() {
                            return false;
                        }

                        public void writeRequest(OutputStream outputStream) throws IOException {
                            IOUtils.copyStream(bodyInputStream, outputStream);
                        }
                    });
                } else {
                    PutMethod putMethod = (PutMethod)method;
                    putMethod.setRequestEntity(new RequestEntity(){
                        public long getContentLength() {
                            return contentLen != null ? contentLen.longValue() : -1;
                        }

                        public String getContentType() {
                            return rct != null ? rct.getFullValue() : null;
                        }

                        public boolean isRepeatable() {
                            return false;
                        }

                        public void writeRequest(OutputStream outputStream) throws IOException {
                            IOUtils.copyStream(bodyInputStream, outputStream);
                        }
                    });
                }
            }

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

                if (method instanceof PostMethod) {
                    PostMethod postMethod = (PostMethod)method;
                    postMethod.setRequestEntity(new RequestEntity(){
                        public long getContentLength() {
                            return contentLen != null ? contentLen.longValue() : -1;
                        }

                        public String getContentType() {
                            return rct != null ? rct.getFullValue() : null;
                        }

                        public boolean isRepeatable() {
                            return true;
                        }

                        public void writeRequest(OutputStream outputStream) throws IOException {
                            InputStream inputStream = null;
                            try {
                                inputStream = inputStreamFactory.getInputStream();
                                IOUtils.copyStream(inputStream, outputStream);
                            } finally {
                                if (inputStream != null) try { inputStream.close(); }catch(IOException ioe){ /*ok*/ }
                            }
                        }
                    });
                } else {
                    PutMethod putMethod = (PutMethod)method;
                    putMethod.setRequestEntity(new RequestEntity(){
                        public long getContentLength() {
                            return contentLen != null ? contentLen.longValue() : -1;
                        }

                        public String getContentType() {
                            return rct != null ? rct.getFullValue() : null;
                        }

                        public boolean isRepeatable() {
                            return true;
                        }

                        public void writeRequest(OutputStream outputStream) throws IOException {
                            InputStream inputStream = null;
                            try {
                                inputStream = inputStreamFactory.getInputStream();
                                IOUtils.copyStream(inputStream, outputStream);
                            } finally {
                                if (inputStream != null) try { inputStream.close(); }catch(IOException ioe){ /*ok*/ }
                            }
                        }
                    });
                }
            }

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
                        status = client.executeMethod(hc , method, state);
                    }
                    else {
                        status = client.executeMethod(hconf, method, state);
                    }
                    Header cth = method.getResponseHeader(MimeUtil.CONTENT_TYPE);
                    contentType = cth == null || cth.getValue() == null ? null : ContentTypeHeader.parseValue(cth.getValue());
                    Header clh = method.getResponseHeader(MimeUtil.CONTENT_LENGTH);
                    contentLength = clh == null || clh.getValue() == null ? null : new Long(Long.parseLong(clh.getValue()));
                } catch (IOException e) {
                    String target = null;
                    try {
                        target = method.getURI().toString();
                    } catch (URIException e1) {
                        logger.log(Level.WARNING, "cannot get URI", e1);
                    }
                    throw new GenericHttpException("Unable to obtain HTTP response from " + target + ": " + ExceptionUtils.getMessage(e), e);
                }

                final GenericHttpResponse genericHttpResponse = new GenericHttpResponse() {
                    private HttpMethod response = method; { method = null; } // Take ownership of the HttpMethod

                    public InputStream getInputStream() throws GenericHttpException {
                        if (response == null)
                            throw new IllegalStateException("This response has already been closed");
                        try {
                            return response.getResponseBodyAsStream();
                        } catch (IOException e) {
                            throw new GenericHttpException(e);
                        }
                    }

                    public int getStatus() {
                        return status;
                    }

                    public HttpHeaders getHeaders() {
                        if (response == null)
                            throw new IllegalStateException("This response has already been closed");
                        Header[] in = response.getResponseHeaders();
                        List out = new ArrayList();
                        for (int i = 0; i < in.length; i++) {
                            Header header = in[i];
                            out.add(new GenericHttpHeader(header.getName(), header.getValue()));
                        }
                        return new GenericHttpHeaders((HttpHeader[])out.toArray(new HttpHeader[0]));
                    }

                    public ContentTypeHeader getContentType() {
                        return contentType;
                    }

                    public Long getContentLength() {
                        return contentLength;
                    }

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

            public void close() {
                stampBindingIdentity();
                if (method != null) {
                    method.releaseConnection();
                    method = null;
                }
            }
        };
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
        Protocol protocol = (Protocol)protoBySockFac.get(sockFac);
        if (protocol == null) {
            logger.finer("Creating new commons Protocol for https");
            protocol = new Protocol("https", (ProtocolSocketFactory) new SecureProtocolSocketFactory() {
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return verify(sockFac.createSocket(socket, host, port, autoClose), host);
                }

                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                    return verify(sockFac.createSocket(host, port, clientAddress, clientPort), host);
                }

                public Socket createSocket(String host, int port) throws IOException {
                    return verify(sockFac.createSocket(host, port), host);
                }

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
                    if (hostVerifier != null && socket instanceof SSLSocket) {
                        SSLSocket sslSocket = (SSLSocket) socket;

                        // must start handshake or any exception can be lost when
                        // getSession() is called
                        sslSocket.startHandshake();

                        if(!hostVerifier.verify(host, sslSocket.getSession())) {
                            try{
                                socket.close(); // close socket since we're not returning it
                            } catch (IOException ioe) { }
                            throw new CausedIOException("Host name does not match certificate '" + host + "'.");
                        }
                    }
                    return socket;
                }
            }, 443);
            protoBySockFac.put(sockFac, protocol);
        }
        hconf = new HostConfiguration();
        HttpHost httpHost = new HttpHost(targetUrl.getHost(), targetUrl.getPort(), protocol);
        hconf.setHost(httpHost);
        return hconf;
    }
}
