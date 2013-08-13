package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static junit.framework.Assert.assertEquals;

public class IdentityBindingHttpClientFactoryTest {

    private IdentityBindingHttpClientFactory factory;
    private MockHttpServer httpServer;

    @Before
    public void setUp() throws Exception {
        factory = new IdentityBindingHttpClientFactory();
        //Setup Http server
        httpServer = new MockHttpServer(17800);
        httpServer.start();
    }

    @After
    public void tearDown() throws Exception {
        httpServer.stop();
    }

    @Test
    public void testConnectionWithBinding() throws Exception {

        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM"));
        requestParams.setTargetUrl(new URL("http://localhost:" +httpServer.getPort()));
        GenericHttpRequest request = null;
        GenericHttpClient client = factory.createHttpClient(2, 10, -1, -1, "MyConnection");
        try {
            request = client.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);

        } finally {
            if (request != null) {
                request.close();
            }
        }

        client = factory.createHttpClient(2, 10, -1, -1, "MyConnection");
        try {
            request = client.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null) {
                request.close();
            }
        }

        assertEquals(1, factory.getTotalStats().getAvailable());
        assertEquals(1, httpServer.getServedClient().size());

    }

    @Test
    public void testConnectionWithBinding2() throws Exception {
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.addExtraHeader(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM"));

        requestParams.setTargetUrl(new URL("http://localhost:" +httpServer.getPort()));
        GenericHttpRequest request = null;
        GenericHttpClient client = factory.createHttpClient(2, 10, -1, -1, "MyConnection1");
        try {
            request = client.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);

        } finally {
            if (request != null) {
                request.close();
            }
        }

        client = factory.createHttpClient(2, 10, -1, -1, "MyConnection2");
        try {
            request = client.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null) {
                request.close();
            }
        }

        assertEquals(2, factory.getTotalStats().getAvailable());
        assertEquals(2, httpServer.getServedClient().size());
    }


}
