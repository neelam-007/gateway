/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.DefaultHttpParamsFactory;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * GenericHttpClient driver for the Apache Commons HTTP client.
 */
public class CommonsHttpClient implements GenericHttpClient {
    private static final Logger logger = Logger.getLogger(CommonsHttpClient.class.getName());
    public static final String PROP_MAX_CONN_PER_HOST = CommonsHttpClient.class.getName() + ".maxConnectionsPerHost";
    public static final String PROP_MAX_TOTAL_CONN = CommonsHttpClient.class.getName() + ".maxTotalConnections";
    public static final String PROP_STALE_CHECKS = CommonsHttpClient.class.getName() + ".staleCheckCount";

    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    public static final int DEFAULT_READ_TIMEOUT = 60000;

    private static HttpParams httpParams;
    private static final Map protoBySockFac = Collections.synchronizedMap(new WeakHashMap());

    static {
        DefaultHttpParams.setHttpParamsFactory(new CachingHttpParamsFactory(new DefaultHttpParamsFactory()));
    }

    private final HttpConnectionManager cman;
    private final int connectionTimeout;
    private final int timeout;
    private final Object identity;
    private final boolean isBindingManager;

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

        final HttpState state = getHttpState(client, params);

        // NOTE: Use the FILE part of the url here (path + query string), if we use the full URL then
        //       we end up with the default socket factory for the protocol
        final HttpMethod httpMethod = method == POST ? (HttpMethod) new PostMethod(targetUrl.getFile())
                                                     : (HttpMethod) new GetMethod(targetUrl.getFile());

        httpMethod.setFollowRedirects(params.isFollowRedirects());
        HttpMethodParams methodParams = httpMethod.getParams();
        methodParams.setVersion(HttpVersion.HTTP_1_1);
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

        createCookiesFromHeaders(headers, state, targetUrl);


        final ContentTypeHeader rct = params.getContentType();
        if (rct != null)
            httpMethod.addRequestHeader(MimeUtil.CONTENT_TYPE, rct.getFullValue());

        return new GenericHttpRequest() {
            private HttpMethod method = httpMethod;

            public void setInputStream(final InputStream bodyInputStream) {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");
                if (!(method instanceof PostMethod))
                    throw new UnsupportedOperationException("Only POST requests require a body InputStream");

                PostMethod postMethod = (PostMethod)method;
                postMethod.setRequestEntity(new RequestEntity(){
                    public long getContentLength() {
                        return contentLen != null ? contentLen.longValue() : -1;
                    }

                    public String getContentType() {
                        return rct.getFullValue();
                    }

                    public boolean isRepeatable() {
                        return false;
                    }

                    public void writeRequest(OutputStream outputStream) throws IOException {
                        HexUtils.copyStream(bodyInputStream, outputStream);
                    }
                });
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
                        HostConfiguration hc = (HostConfiguration) client.getHostConfiguration().clone();
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
                    throw new GenericHttpException("Unable to obtain HTTP response: " + ExceptionUtils.getMessage(e), e);
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

    private void createCookiesFromHeaders(List headers, HttpState state, URL targetUrl) {
        for (Iterator i = headers.iterator(); i.hasNext();) {
            HttpHeader theHeader = (HttpHeader)i.next();
            if (HttpConstants.HEADER_COOKIE.equalsIgnoreCase(theHeader.getName())) {
                String headerVal = theHeader.getFullValue();
                int indexOfEquals = headerVal.indexOf('=');
                if (indexOfEquals >= 0) {
                    String cookieName = headerVal.substring(0, indexOfEquals);
                    String cookieVal = headerVal.substring(indexOfEquals + 1);
                    Cookie aCookie = new Cookie(targetUrl.getHost(), cookieName, cookieVal,"",-1,false);
                    state.addCookie(aCookie);
                }
            }
        }

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
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
                    return verify(sockFac.createSocket(socket, host, port, autoClose), host);
                }

                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException, UnknownHostException {
                    return verify(sockFac.createSocket(host, port, clientAddress, clientPort), host);
                }

                public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                    return verify(sockFac.createSocket(host, port), host);
                }

                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort, HttpConnectionParams httpConnectionParams) throws IOException, UnknownHostException, ConnectTimeoutException {
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
                        if(!hostVerifier.verify(host, sslSocket.getSession())) {
                            try{
                                socket.close();
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
