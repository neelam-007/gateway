/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;

import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Http client wrapper that always sets thread-local {@link CurrentSslPeer} to a given value when creating
 * a new https connection.
 */
public class SslPeerHttpClient implements GenericHttpClient {
    private final GenericHttpClient client;
    private final SslPeer sslPeer;
    private final SSLSocketFactory socketFactory;

    /**
     * Create a SslPeerHttpClient that delegates to the specified implementation, and prepares https connections
     * to be against the specified SslPeer.
     *
     * @param client
     */
    public SslPeerHttpClient(GenericHttpClient client, SslPeer sslPeer) {
        this.sslPeer = sslPeer;
        this.client = client;
        this.socketFactory = ClientProxySecureProtocolSocketFactory.getInstance();
    }

    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params)
            throws GenericHttpException
    {
        GenericHttpRequestParamsImpl p = new GenericHttpRequestParamsImpl(params);

        final String proto = params.getTargetUrl().getProtocol();
        if ("https".equalsIgnoreCase(proto)) {
            CurrentSslPeer.set(sslPeer);
            p.setSslSocketFactory(socketFactory);
        }

        final GenericHttpRequest request = client.createRequest(method, p);
        return new GenericHttpRequest() {
            public OutputStream getOutputStream() throws GenericHttpException {
                return request.getOutputStream();
            }

            public GenericHttpResponse getResponse() throws GenericHttpException {
                final GenericHttpResponse response = request.getResponse();
                return new GenericHttpResponse() {
                    public InputStream getInputStream() throws GenericHttpException {
                        return response.getInputStream();
                    }

                    public void close() {
                        response.close();
                    }

                    public int getStatus() {
                        return response.getStatus();
                    }

                    public HttpHeader[] getHeaders() {
                        return response.getHeaders();
                    }

                    public ContentTypeHeader getContentType() {
                        return response.getContentType();
                    }

                    public Long getContentLength() {
                        return response.getContentLength();
                    }
                };
            }

            public void close() {
                request.close();
            }
        };
    }
}
