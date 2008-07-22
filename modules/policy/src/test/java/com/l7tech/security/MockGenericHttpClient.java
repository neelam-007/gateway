package com.l7tech.security;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Mock implementation of the GenericHttpClient. Used for testing.
 *
 * This client is only useful in instances where there is one request
 * (extend as required ...)
 *
 * @author $Author$
 * @version $Revision$
 */
public class MockGenericHttpClient implements GenericHttpClient {

    /**
     *
     */
    public MockGenericHttpClient() {
    }

    /**
     *
     */
    public MockGenericHttpClient( int status
                                , HttpHeaders headers
                                , ContentTypeHeader contentTypeHeader
                                , Long contentLength
                                , byte[] body) {
        this.responseStatus = status;
        this.headers = headers;
        this.contentTypeHeader = contentTypeHeader;
        this.contentLength = contentLength;
        this.responseBody = body;
    }

    /**
     *
     *
     * @param method
     * @param params
     * @throws com.l7tech.common.http.GenericHttpException
     */
    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
        this.method = method;
        this.params = params;
        return new MockGenericHttpRequest();
    }

    public byte[] getRequestBody() {
        return requestBody;
    }

    public GenericHttpMethod getMethod() {
        return method;
    }

    public GenericHttpRequestParams getParams() {
        return params;
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public void setContentTypeHeader(ContentTypeHeader contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public synchronized int getResponseCount() {
        return responseCount;
    }

    public synchronized void clearResponseCount() {
        responseCount = 0;
    }

    public Object getIdentity() {
        return identity;
    }

    public void setIdentity(Object identity) {
        this.identity = identity;
    }

    /**
     * Causes threads calling getResponse() to block indefinitely, until holdRespones is turned off again,
     * at which point they will resume normally.
     *
     * @param holdResponses true to hold respones; threads calling getResponse() will block until this method is
     *                              called again with holdResponses equal to false.
     *                      false to release response; any threads blocking in getResponse() will wake and continue
     *                              normally.
     */
    public void setHoldResponses(boolean holdResponses) {
        if (holdResponses) {
            synchronized (MockGenericHttpClient.this) {
                this.holdResponses = true;
            }
        } else {
            synchronized (MockGenericHttpClient.this) {
                this.holdResponses = false;
                MockGenericHttpClient.this.notifyAll();
            }
        }
    }

    public boolean isHoldResponses() {
        synchronized (MockGenericHttpClient.this) {
            return holdResponses;
        }
    }

    //- PRIVATE

    private volatile boolean holdResponses = false;
    private byte[] requestBody;
    private byte[] responseBody;
    private int responseStatus;
    private HttpHeaders headers;
    private ContentTypeHeader contentTypeHeader;
    private Long contentLength;
    private GenericHttpMethod method;
    private GenericHttpRequestParams params;
    private int responseCount = 0;
    private Object identity;

    private class MockGenericHttpRequest implements GenericHttpRequest
    {
        private InputStream in;

        public GenericHttpResponse getResponse() throws GenericHttpException {
            try {
                requestBody = in == null ? null : IOUtils.slurpStream(in);
            }
            catch(IOException ioe) {
                requestBody = null;
            }

            // If configured to wait, hold responses until we are released
            synchronized (MockGenericHttpClient.this) {
                while (holdResponses) {
                    try {
                        MockGenericHttpClient.this.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                responseCount++;
            }
            return new MockGenericHttpResponse();
        }

        public void addParameter(String paramName, String paramValue) throws IllegalArgumentException, IllegalStateException {
            throw new IllegalStateException("this impl does not support addParameter");
        }

        public void close() {
            // mock ...
        }

        public void setInputStream(InputStream bodyInputStream) {
            in = bodyInputStream;
        }
    }

    private class MockGenericHttpResponse extends GenericHttpResponse
    {
        public InputStream getInputStream() throws GenericHttpException {
            return new ByteArrayInputStream(responseBody);
        }

        public void close() {
            // - mock
        }

        public int getStatus() {
            return responseStatus;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public ContentTypeHeader getContentType() {
            return contentTypeHeader;
        }

        public Long getContentLength() {
            return contentLength;
        }
    }
}
