package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.*;
import com.l7tech.common.io.MockSSLSocketFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import org.apache.commons.httpclient.protocol.Protocol;
import org.junit.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

/**
 *
 */
public class CommonsHttpClientMultipleContentLengthHeadersTest {

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
        try {
            GenericHttpClient client = new CommonsHttpClient();
            Protocol.registerProtocol( "http", new Protocol( "http", new MockProtocolSocketFactory() {
                @Override
                protected Socket onCreateSocket( final String host, final int port ) throws IOException {
                    return new MockSSLSocketFactory.MockSSLSocket( HTTP_RESP.getBytes( Charsets.ISO8859 ) );
                }
            }, 80 ) );
            GenericHttpRequestParams params = new GenericHttpRequestParams(new URL("http://localhost:7353"));
            GenericHttpRequest request = client.createRequest(HttpMethod.POST, params);
            request.setInputStream(new ByteArrayInputStream("<blah/>".getBytes()));
            GenericHttpResponse response = request.getResponse();
            String got = response.getAsString(false, 1024 * 1024 * 10);
            assertEquals("Content", RESP, got);
            assertEquals("Content length", (long)RESP.length(), (long)response.getContentLength());
        } finally {
            Protocol.unregisterProtocol( "http" );
        }
    }
}
