package com.l7tech.common.http;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

import com.l7tech.common.mime.ContentTypeHeader;

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
     * @return
     * @throws GenericHttpException
     */
    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
        this.method = method;
        this.params = params;
        return new MockGenericHttpRequest();
    }

    //- PRIVATE

    private byte[] responseBody;
    private int responseStatus;
    private HttpHeaders headers;
    private ContentTypeHeader contentTypeHeader;
    private Long contentLength;
    private GenericHttpMethod method;
    private GenericHttpRequestParams params;

    private class MockGenericHttpRequest implements GenericHttpRequest
    {
        public GenericHttpResponse getResponse() throws GenericHttpException {
            return new MockGenericHttpResponse();
        }

        public void close() {
            // mock ...
        }

        public void setInputStream(InputStream bodyInputStream) {
            // mock ...
        }
    }

    private class MockGenericHttpResponse implements GenericHttpResponse
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
