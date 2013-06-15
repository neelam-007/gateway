package com.l7tech.common.http.prov.apache;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.MockSSLSocketFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.params.HttpParams;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class HttpComponentClientMultipleContentLengthHeadersTest {

    private static final String RESP = "<h1>Hello</h1>";
    private static final String HTTP_RESP =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/xml; charset=iso-8859-1\r\n" +
            "Content-Length: 14\r\n" +
            "Content-Length: 14\r\n" +
            "Server: Jetty(6.1.24)\r\n" +
            "\r\n" +
            "<h1>Hello</h1>";

    @Test
    @BugNumber(7353)
    public void testFetchMultipleContentLength() throws Exception {

        MockWebServer server = new MockWebServer();
        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(RESP);
        server.enqueue(mockResponse);
        server.play();

        ClientConnectionManager connectionManager = null;
        try {
            connectionManager = HttpComponentsClient.newConnectionManager();

            GenericHttpClient client = new HttpComponentsClient(connectionManager, -1, -1);
            GenericHttpRequestParams params = new GenericHttpRequestParams(server.getUrl("/"));
            GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
            request.setInputStream(new ByteArrayInputStream("<blah/>".getBytes()));
            GenericHttpResponse response = request.getResponse();
            String got = response.getAsString(false, 1024 * 1024 * 10);
            assertEquals("Content", RESP, got);
            assertEquals("Content length", (long)RESP.length(), (long)response.getContentLength());
        } finally {
            if (connectionManager != null) {
                connectionManager.getSchemeRegistry().unregister("http");
            }
        }
    }
}
