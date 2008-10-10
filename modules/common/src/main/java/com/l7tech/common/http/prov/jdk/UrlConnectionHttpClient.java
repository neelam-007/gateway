/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http.prov.jdk;

import com.l7tech.common.http.*;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ExceptionUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An implementation of GenericHttpClient that uses the JDK's {@link java.net.URL#openConnection()} as the
 * underlying client.
 */
public class UrlConnectionHttpClient implements GenericHttpClient {
    public static final HttpHeader[] HTTPHEADER_EMPTY_ARRAY = new HttpHeader[0];

    public UrlConnectionHttpClient() {
    }

    public GenericHttpRequest createRequest(final GenericHttpMethod method, GenericHttpRequestParams params)
            throws GenericHttpException
    {
        try {
            final URLConnection conn = params.getTargetUrl().openConnection();
            if (!(conn instanceof HttpURLConnection))
                throw new GenericHttpException("URLConnection was not an HttpURLConnection");
            final HttpURLConnection httpConn = (HttpURLConnection)conn;
            httpConn.setInstanceFollowRedirects(params.isFollowRedirects());
            final HttpsURLConnection httpsConn;
            if (conn instanceof HttpsURLConnection) {
                httpsConn = (HttpsURLConnection)conn;
                if (params.getSslSocketFactory() != null)
                    httpsConn.setSSLSocketFactory(params.getSslSocketFactory());
                if (params.getHostnameVerifier() != null)
                    httpsConn.setHostnameVerifier(params.getHostnameVerifier());
            } else {
                if (params.getTargetUrl().getProtocol().equalsIgnoreCase("https"))
                    throw new GenericHttpException("HttpURLConnection was using SSL but was not an HttpsURLConnection");
                httpsConn = null;
            }

            if (method.needsRequestBody())
                conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setDefaultUseCaches(false);

            // Set headers
            List extraHeaders = params.getExtraHeaders();
            for (Iterator i = extraHeaders.iterator(); i.hasNext();) {
                HttpHeader extraHeader = (HttpHeader)i.next();
                try {
                    conn.setRequestProperty(extraHeader.getName(), MimeUtility.encodeText(extraHeader.getFullValue(), "utf-8", "Q"));
                } catch (UnsupportedEncodingException e) {
                    throw new GenericHttpException("Unable to encode header value for header " + extraHeader.getName() + ": " + ExceptionUtils.getMessage(e), e);
                }
            }

            // Set content type
            if (params.getContentType() != null)
                conn.setRequestProperty(MimeUtil.CONTENT_TYPE, params.getContentType().getFullValue());

            // HTTP Basic support -- preemptive authentication
            PasswordAuthentication pw = params.getPasswordAuthentication();
            if (pw != null) {
                String auth = "Basic " + HexUtils.encodeBase64(
                        (pw.getUserName() + ":" + new String(pw.getPassword())).getBytes());
                conn.setRequestProperty("Authorization", auth);
            }

            return new GenericHttpRequest() {
                boolean completedRequest = false;
                private InputStream requestInputStream = null;

                public void setInputStream(InputStream bodyInputStream) {
                    if (!method.needsRequestBody())
                        throw new UnsupportedOperationException("bodyInputStream not needed for request method: " + method);
                    if (completedRequest)
                        throw new IllegalStateException("This HTTP request is already closed");
                    requestInputStream = bodyInputStream;
                }

                public void addParameter(String paramName, String paramValue) throws IllegalArgumentException, IllegalStateException {
                    throw new IllegalStateException("this implementation of the GenericHttpRequest does not support addParameter");
                }

                public GenericHttpResponse getResponse() throws GenericHttpException {
                    try {
                        if (requestInputStream != null)
                            IOUtils.copyStream(requestInputStream, conn.getOutputStream());

                        final int status = httpConn.getResponseCode();
                        String ctval = conn.getContentType();
                        final ContentTypeHeader contentTypeHeader =
                                ctval != null ? ContentTypeHeader.parseValue(ctval) : null;
                        final List headers = new ArrayList();
                        int n = 0;
                        String value = null;
                        do {
                            String key = httpConn.getHeaderFieldKey(n);
                            value = httpConn.getHeaderField(n);
                            if (key != null && value != null)
                                headers.add(new GenericHttpHeader(key, value));
                            n++;
                        } while (value != null);
                        final GenericHttpHeaders genericHttpHeaders =
                                new GenericHttpHeaders((HttpHeader[])headers.toArray(HTTPHEADER_EMPTY_ARRAY));

                        completedRequest = true;

                        return new GenericHttpResponse() {
                            public InputStream getInputStream() throws GenericHttpException {
                                InputStream inputStream;
                                try {
                                    inputStream = conn.getInputStream();
                                } catch (IOException e) {
                                    inputStream = httpConn.getErrorStream();
                                    if (inputStream == null)
                                        throw new GenericHttpException(e);
                                }
                                return inputStream;
                            }

                            public int getStatus() {
                                return status;
                            }

                            public HttpHeaders getHeaders() {
                                return genericHttpHeaders;
                            }

                            public ContentTypeHeader getContentType() {
                                return contentTypeHeader;
                            }

                            public Long getContentLength() {
                                return new Long(conn.getContentLength());
                            }

                            public void close() {
                                httpConn.disconnect();
                            }

                        };
                    } catch (IOException e) {
                        throw new GenericHttpException(e);
                    }
                }

                public void close() {
                    if (!completedRequest) {
                        httpConn.disconnect();
                        completedRequest = true;
                    }
                }
            };
        } catch (IOException e) {
            throw new GenericHttpException(e);
        } finally {
        }
    }
}
