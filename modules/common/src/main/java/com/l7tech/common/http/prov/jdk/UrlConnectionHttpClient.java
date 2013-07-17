package com.l7tech.common.http.prov.jdk;

import com.l7tech.common.http.*;
import com.l7tech.util.IOUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.apache.http.NameValuePair;

import javax.mail.internet.MimeUtility;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

    public UrlConnectionHttpClient() {
    }

    @Override
    public GenericHttpRequest createRequest(final HttpMethod method, final GenericHttpRequestParams params)
            throws GenericHttpException
    {
        if ( params.isGzipEncode() ) {
            throw new GenericHttpException("GZIP encoding not supported.");            
        }

        try {
            final URLConnection conn = params.getTargetUrl().openConnection();
            if (!(conn instanceof HttpURLConnection))
                throw new GenericHttpException("URLConnection was not an HttpURLConnection");
            final HttpURLConnection httpConn = (HttpURLConnection)conn;
            httpConn.setInstanceFollowRedirects(params.isFollowRedirects());
            if ( params.getConnectionTimeout() >= 0 ) {
                httpConn.setConnectTimeout( params.getConnectionTimeout() );
            }
            if ( params.getReadTimeout() >= 0 ) {
                httpConn.setReadTimeout( params.getReadTimeout() );
            }
            if (conn instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsConn;
                httpsConn = (HttpsURLConnection)conn;
                if (params.getSslSocketFactory() != null)
                    httpsConn.setSSLSocketFactory(params.getSslSocketFactory());
                if (params.getHostnameVerifier() != null)
                    httpsConn.setHostnameVerifier(params.getHostnameVerifier());
            } else {
                if (params.getTargetUrl().getProtocol().equalsIgnoreCase("https"))
                    throw new GenericHttpException("HttpURLConnection was using SSL but was not an HttpsURLConnection");
            }

            if (params.needsRequestBody(method))
                conn.setDoOutput(true);
            conn.setAllowUserInteraction(false);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);

            // Set headers
            List<HttpHeader> extraHeaders = params.getExtraHeaders();
            for (HttpHeader extraHeader: extraHeaders) {
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

                @Override
                public void setInputStream(InputStream bodyInputStream) {
                    if (!params.needsRequestBody(method))
                        throw new UnsupportedOperationException("bodyInputStream not needed for request method: " + method);
                    if (completedRequest)
                        throw new IllegalStateException("This HTTP request is already closed");
                    requestInputStream = bodyInputStream;
                }

                @Override
                public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                    throw new IllegalStateException("this implementation of the GenericHttpRequest does not support addParameter");
                }

                @Override
                public GenericHttpResponse getResponse() throws GenericHttpException {
                    try {
                        if (requestInputStream != null)
                            IOUtils.copyStream(requestInputStream, conn.getOutputStream());

                        final int status = httpConn.getResponseCode();
                        String ctval = conn.getContentType();
                        final ContentTypeHeader contentTypeHeader =
                                ctval != null ? ContentTypeHeader.create(ctval) : null;
                        final List<HttpHeader> headers = new ArrayList<HttpHeader>();
                        int n = 0;
                        String value;
                        do {
                            String key = httpConn.getHeaderFieldKey(n);
                            value = httpConn.getHeaderField(n);
                            if (key != null && value != null)
                                headers.add(new GenericHttpHeader(key, value));
                            n++;
                        } while (value != null);
                        final GenericHttpHeaders genericHttpHeaders =
                                new GenericHttpHeaders(headers.toArray(new HttpHeader[headers.size()]));

                        completedRequest = true;

                        return new GenericHttpResponse() {
                            @Override
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

                            @Override
                            public int getStatus() {
                                return status;
                            }

                            @Override
                            public HttpHeaders getHeaders() {
                                return genericHttpHeaders;
                            }

                            @Override
                            public ContentTypeHeader getContentType() {
                                return contentTypeHeader;
                            }

                            @Override
                            public Long getContentLength() {
                                return (long)conn.getContentLength();
                            }

                            @Override
                            public void close() {
                                httpConn.disconnect();
                            }

                        };
                    } catch (IOException e) {
                        throw new GenericHttpException(e);
                    }
                }

                @Override
                public void close() {
                    if (!completedRequest) {
                        httpConn.disconnect();
                        completedRequest = true;
                    }
                }
            };
        } catch (IOException e) {
            throw new GenericHttpException(e);
        }
    }
}
