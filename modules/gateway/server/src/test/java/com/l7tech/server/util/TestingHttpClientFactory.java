package com.l7tech.server.util;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.security.MockGenericHttpClient;

/**
 * @author Steve Jones
 */
public class TestingHttpClientFactory implements GenericHttpClientFactory {

    //- PUBLIC


    public TestingHttpClientFactory() {
    }

    public TestingHttpClientFactory(MockGenericHttpClient mockGenericHttpClient) {
        this.mockGenericHttpClient = mockGenericHttpClient;
    }

    public GenericHttpClient createHttpClient() {
        return mockGenericHttpClient;
    }

    public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
        if (mockGenericHttpClient != null)
            mockGenericHttpClient.setIdentity(identity);
        
        return mockGenericHttpClient;
    }

    public void setMockHttpClient(MockGenericHttpClient mockClient) {
        mockGenericHttpClient = mockClient;
    }

    //- PRIVATE

    private MockGenericHttpClient mockGenericHttpClient;
}
