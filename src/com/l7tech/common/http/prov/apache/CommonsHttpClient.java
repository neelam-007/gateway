/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;

/**
 * GenericHttpClient driver for the Apache Commons HTTP client.
 */
public class CommonsHttpClient implements GenericHttpClient {
    final HttpConnectionManager cman;

    public CommonsHttpClient(HttpConnectionManager cman) {
        this.cman = cman;
    }

    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params)
            throws GenericHttpException
    {
        final HttpClient client = new HttpClient(cman);
        HttpState state = client.getState();

        final HttpMethod httpMethod = method == POST ? new PostMethod(params.getTargetUrl().toString())
                                                     : new GetMethod(params.getTargetUrl().toString());

        PasswordAuthentication pw = params.getPasswordAuthentication();
        if (pw != null) {
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

        HttpHeader[] headers = params.getExtraHeaders();
        for (int i = 0; i < headers.length; i++) {
            HttpHeader header = headers[i];
            httpMethod.addRequestHeader(header.getName(), header.getFullValue());
        }

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
                    status = client.executeMethod(method);
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

                    public HttpHeader[] getHeaders() {
                        if (response == null)
                            throw new IllegalStateException("This response has already been closed");
                        Header[] in = response.getResponseHeaders();
                        List out = new ArrayList();
                        for (int i = 0; i < in.length; i++) {
                            Header header = in[i];
                            out.add(new GenericHttpHeader(header.getName(), header.getValue()));
                        }
                        return (HttpHeader[])out.toArray(new HttpHeader[0]);
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
}
