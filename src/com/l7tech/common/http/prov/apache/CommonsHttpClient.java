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
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * GenericHttpClient driver for the Apache Commons HTTP client.
 */
public class CommonsHttpClient implements GenericHttpClient {
    private static final Logger logger = Logger.getLogger(CommonsHttpClient.class.getName());
    public static final String PROP_MAX_CONN_PER_HOST = CommonsHttpClient.class.getName() + ".maxConnectionsPerHost";
    public static final String PROP_MAX_TOTAL_CONN = CommonsHttpClient.class.getName() + ".maxTotalConnections";

    private final HttpConnectionManager cman;
    private final int connectionTimeout;
    private final int timeout;

    public CommonsHttpClient(HttpConnectionManager cman) {
        this(cman,0,0); // no timeouts
    }

    public CommonsHttpClient(HttpConnectionManager cman, int connectTimeout, int timeout) {
        this.cman = cman;
        this.connectionTimeout = connectTimeout;
        this.timeout = timeout;
    }

    public static MultiThreadedHttpConnectionManager newConnectionManager(int maxConnectionsPerHost, int maxTotalConnections) {
        MultiThreadedHttpConnectionManager hcm = new MultiThreadedHttpConnectionManager();
        hcm.setMaxConnectionsPerHost(maxConnectionsPerHost);
        hcm.setMaxTotalConnections(maxTotalConnections);
        return hcm;
    }

    public static MultiThreadedHttpConnectionManager newConnectionManager() {
        int maxConnPerHost = Integer.getInteger(PROP_MAX_CONN_PER_HOST, 200).intValue();
        int maxTotalConnections = Integer.getInteger(PROP_MAX_TOTAL_CONN, 2000).intValue();
        return newConnectionManager(maxConnPerHost, maxTotalConnections);
    }

    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params)
            throws GenericHttpException
    {
        final HostConfiguration hconf;
        final URL targetUrl = params.getTargetUrl();
        final String targetProto = targetUrl.getProtocol();
        if ("https".equals(targetProto)) {
            final SSLSocketFactory sockFac = params.getSslSocketFactory();
            if (sockFac != null) {
                hconf = getHostConfig(targetUrl, sockFac);
            } else
                hconf = null;
        } else
            hconf = null;

        final HttpClient client = new HttpClient(cman);
        client.setConnectionTimeout(connectionTimeout);
        client.setTimeout(timeout);

        final HttpState state = getHttpState(client, params);

        final HttpMethod httpMethod = method == POST ? new PostMethod(targetUrl.toString())
                                                     : new GetMethod(targetUrl.toString());

        httpMethod.setFollowRedirects(params.isFollowRedirects());
        PasswordAuthentication pw = params.getPasswordAuthentication();
        NtlmAuthentication ntlm = params.getNtlmAuthentication();
        if (ntlm != null) {
            httpMethod.setDoAuthentication(true);
            state.setCredentials(null, null,
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
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(username, new String(password)));
            state.setAuthenticationPreemptive(params.isPreemptiveAuthentication());
        }

        Long contentLen = params.getContentLength();
        if (httpMethod instanceof PostMethod && contentLen != null) {
            final long clen = contentLen.longValue();
            if (clen > Integer.MAX_VALUE)
                throw new GenericHttpException("Content-Length is too long -- maximum supported is " + Integer.MAX_VALUE);
            if (clen >= 0)
                ((PostMethod)httpMethod).setRequestContentLength((int)clen);
        }

        List headers = params.getExtraHeaders();
        for (Iterator i = headers.iterator(); i.hasNext();) {
            HttpHeader header = (HttpHeader)i.next();
            httpMethod.addRequestHeader(header.getName(), header.getFullValue());
        }

        createCookiesFromHeaders(headers, state, targetUrl);


        final ContentTypeHeader rct = params.getContentType();
        if (rct != null)
            httpMethod.addRequestHeader(MimeUtil.CONTENT_TYPE, rct.getFullValue());

        return new GenericHttpRequest() {
            private HttpMethod method = httpMethod;

            public void setInputStream(InputStream bodyInputStream) {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");
                if (!(method instanceof PostMethod))
                    throw new UnsupportedOperationException("Only POST requests require a body InputStream");

                PostMethod postMethod = (PostMethod)method;
                postMethod.setRequestBody(bodyInputStream);
            }

            public GenericHttpResponse getResponse() throws GenericHttpException {
                if (method == null)
                    throw new IllegalStateException("This request has already been closed");

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
                    throw new GenericHttpException(e);
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
                if (method != null) {
                    method.releaseConnection();
                    method = null;
                }
            }
        };
    }

    private HttpState getHttpState(HttpClient client, GenericHttpRequestParams params) {
        HttpState httpState;
        if(params.getState()==null) {
            // use per client http state (standard behaviour)
            httpState = client.getState();
            httpState.setCookiePolicy(CookiePolicy.COMPATIBILITY);
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

    private static Map protoBySockFac = Collections.synchronizedMap(new WeakHashMap());

    private HostConfiguration getHostConfig(final URL targetUrl, final SSLSocketFactory sockFac) {
        HostConfiguration hconf;
        Protocol protocol = (Protocol)protoBySockFac.get(sockFac);
        if (protocol == null) {
            logger.finer("Creating new commons Protocol for https");
            protocol = new Protocol("https", new SecureProtocolSocketFactory() {
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
                    return sockFac.createSocket(socket, host, port, autoClose);
                }

                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException, UnknownHostException {
                    return sockFac.createSocket(host, port, clientAddress, clientPort);
                }

                public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                    return sockFac.createSocket(host, port);
                }
            }, 443);
            protoBySockFac.put(sockFac, protocol);
        }
        hconf = new HostConfiguration();
        hconf.setHost(targetUrl.getHost(), targetUrl.getPort(), protocol);
        return hconf;
    }
}
