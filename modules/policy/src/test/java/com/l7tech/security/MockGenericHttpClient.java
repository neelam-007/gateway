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
                                , HttpHeaders responseHeaders
                                , ContentTypeHeader contentTypeHeader
                                , Long contentLength
                                , byte[] body) {
        this.responseStatus = status;
        this.responseHeaders = responseHeaders != null ? responseHeaders : new GenericHttpHeaders(new GenericHttpHeader[0]);
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
        MockGenericHttpRequest req = new MockGenericHttpRequest();
        if (createRequestListener != null) {
            MockGenericHttpRequest replacementRequest = createRequestListener.onCreateRequest(method, params, req);
            if (replacementRequest != null)
                req = replacementRequest;
        }
        return req;
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

    public void setResponseHeaders(HttpHeaders responseHeaders) {
        this.responseHeaders = responseHeaders;
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

    public CreateRequestListener getCreateRequestListener() {
        return createRequestListener;
    }

    public void setCreateRequestListener(CreateRequestListener createRequestListener) {
        this.createRequestListener = createRequestListener;
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

    public interface CreateRequestListener {
        /**
         * Notify that someone has called createRequest on a monitored mock client.
         * <p/>
         * The new (empty) MockGenericHttpRequest is provided.  The listener may optionally replace this with a different
         * MockGenericHttpRequest to create instead.
         *
         * @param method the method that is being created.  Normally never null, but may be null in test code if null was passed (presumably for testing purposes).
         * @param params the request parameters.  Normally never null, but may be null in test code if null was passed (presumably for testing).
         * @param request a new empty default MockGenericHttpRequest that will be returned from createRequest, or null to return the one that was created (@{link #request}).
         * @return
         */
        MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpRequest request);
    }

    //- PROTECTED

    protected byte[] getResponseBody() throws IOException {
        return responseBody;
    }

    //- PRIVATE

    private volatile boolean holdResponses = false;
    private byte[] requestBody;
    private byte[] responseBody;
    private int responseStatus;
    private HttpHeaders responseHeaders;
    private ContentTypeHeader contentTypeHeader;
    private Long contentLength;
    private HttpMethod method;
    private GenericHttpRequestParams params;
    private int responseCount = 0;
    private Object identity;
    private CreateRequestListener createRequestListener;

    public class MockGenericHttpRequest implements RerunnableHttpRequest
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

        private void initResponse() throws IOException {
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
            try {
                initResponse();
                return new ByteArrayInputStream(responseData);
            } catch ( IOException e ) {
                throw new GenericHttpException(e);
            }
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
            return responseHeaders;
        }

        @Override
        public ContentTypeHeader getContentType() {
            return contentTypeHeader;
        }

        @Override
        public Long getContentLength() {
            try {
                initResponse();
            } catch ( IOException e ) {
                // ok
            }
            return responseLength;
        }
    }
}
