/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.util.HexUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URLConnection;
import java.util.ArrayList;
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
            final HttpsURLConnection httpsConn;
            if (conn instanceof HttpsURLConnection) {
                httpsConn = (HttpsURLConnection)conn;
                if (params.getSslSocketFactory() != null)
                    httpsConn.setSSLSocketFactory(params.getSslSocketFactory());
            } else {
                if (params.getTargetUrl().getProtocol().equalsIgnoreCase("https"))
                    throw new GenericHttpException("HttpURLConnection was using SSL but was not an HttpsURLConnection");
                httpsConn = null;
            }

            if (method == POST)
                conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setDefaultUseCaches(false);

            // Set headers
            HttpHeader[] extraHeaders = params.getExtraHeaders();
            for (int i = 0; i < extraHeaders.length; i++) {
                HttpHeader extraHeader = extraHeaders[i];
                conn.setRequestProperty(extraHeader.getName(), extraHeader.getFullValue());
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

                public OutputStream getOutputStream() throws GenericHttpException {
                    try {
                        if (method != POST) throw new UnsupportedOperationException();
                        return conn.getOutputStream();
                    } catch (IOException e) {
                        throw new GenericHttpException(e);
                    }
                }

                public GenericHttpResponse getResponse() throws GenericHttpException {
                    try {
                        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(conn.getContentType());
                        final int status = httpConn.getResponseCode();
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

                            public HttpHeader[] getHeaders() {
                                return (HttpHeader[])headers.toArray(HTTPHEADER_EMPTY_ARRAY);
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
