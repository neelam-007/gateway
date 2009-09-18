package com.l7tech.security;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.IOUtils;
import com.l7tech.common.http.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Mock implementation of the GenericHttpClient. Used for testing.
 *
 * This client is only useful in instances where there is one request
 * (extend as required ...)
 *
 * @author alex
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
    @Override
    public GenericHttpRequest createRequest(HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
        this.method = method;
        this.params = params;
        return new MockGenericHttpRequest();
    }

    public byte[] getRequestBody() {
        return requestBody;
    }

    public HttpMethod getMethod() {
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

    //- PROTECTED

    protected byte[] getResponseBody() {
        return responseBody;
    }

    //- PRIVATE

    private volatile boolean holdResponses = false;
    private byte[] requestBody;
    private byte[] responseBody;
    private int responseStatus;
    private HttpHeaders headers;
    private ContentTypeHeader contentTypeHeader;
    private Long contentLength;
    private HttpMethod method;
    private GenericHttpRequestParams params;
    private int responseCount = 0;
    private Object identity;

    private class MockGenericHttpRequest implements RerunnableHttpRequest
    {
        private InputStreamFactory inFac;

        @Override
        public void setInputStreamFactory( final InputStreamFactory isf ) {
            inFac = isf;
        }

        @Override
        public GenericHttpResponse getResponse() throws GenericHttpException {
            try {
                requestBody = inFac == null ? null : IOUtils.slurpStream(inFac.getInputStream());
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

        @Override
        public void addParameter(String paramName, String paramValue) throws IllegalArgumentException, IllegalStateException {
            throw new IllegalStateException("this impl does not support addParameter");
        }

        @Override
        public void close() {
            // mock ...
        }

        @Override
        public void setInputStream(final InputStream bodyInputStream) {
            inFac = new InputStreamFactory(){
                @Override
                public InputStream getInputStream() {
                    return bodyInputStream;
                }
            };
        }
    }

    private class MockGenericHttpResponse extends GenericHttpResponse
    {
        private long responseLength;
        private byte[] responseData;

        private void initResponse() {
            if ( responseData == null ) {
                responseData = getResponseBody();
                if ( contentLength==null ) {
                    contentLength = (long) responseData.length;
                } else {
                    responseLength = contentLength;
                }
            }
        }

        @Override
        public InputStream getInputStream() throws GenericHttpException {
            initResponse();
            return new ByteArrayInputStream(responseData);
        }

        @Override
        public void close() {
            // - mock
        }

        @Override
        public int getStatus() {
            return responseStatus;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public ContentTypeHeader getContentType() {
            return contentTypeHeader;
        }

        @Override
        public Long getContentLength() {
            initResponse();
            return responseLength;
        }
    }
}
