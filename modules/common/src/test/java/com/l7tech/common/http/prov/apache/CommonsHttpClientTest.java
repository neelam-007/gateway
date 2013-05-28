package com.l7tech.common.http.prov.apache;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Unit tests for commons http client
 */
public class CommonsHttpClientTest {


    CommonsHttpClient fixture;

    @Before
    public void setUp() throws Exception {
        fixture = new CommonsHttpClient();
    }

    @Test
    @BugNumber(9258)
    public void testInvalidURLPath() throws Exception {
        final CommonsHttpClient client = new CommonsHttpClient( new SimpleHttpConnectionManager(true) );

        for ( final HttpMethod method : HttpMethod.values() ) {
            if ( method == HttpMethod.OTHER ) continue;
            final GenericHttpRequest request = client.createRequest( method, new GenericHttpRequestParams( new URL("http://host/this is not a valid path") ) );
            try {
                request.getResponse();
                fail( "Expected failure due to invalid uri path" );
            } catch ( GenericHttpException e ) {
                // ensure expected exception
                assertTrue( "Error is for path", ExceptionUtils.getMessage( e ).contains( "this is not a valid path" ));
            }
        }
    }

    @Test
    public void testCompressRequest() throws Exception {

        String testMessage = "<test>test message</test>";

        MockHttpServer httpServer = new MockHttpServer(17800);
        httpServer.setHttpHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] buffer = new byte[1024];
                InputStream is = exchange.getRequestBody();
                GZIPInputStream gzis = new GZIPInputStream(is);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
                gzis.close();
                bos.close();
                is.close();
                exchange.sendResponseHeaders(200, bos.size());
                OutputStream os = exchange.getResponseBody();
                os.write(bos.toByteArray());
                exchange.close();
            }
        });
        httpServer.start();

        try {
            GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
            requestParams.setContentType(ContentTypeHeader.XML_DEFAULT);
            requestParams.setTargetUrl(new URL("http://localhost:" + httpServer.getPort()));
            requestParams.setGzipEncode(true);
            RerunnableHttpRequest request = null;
            request = (RerunnableHttpRequest) fixture.createRequest(HttpMethod.POST, requestParams);
            request.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory() {
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(("<test>test message</test>").getBytes());
                }
            });
            GenericHttpResponse response = request.getResponse();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copyStream(response.getInputStream(), bos);
            assertEquals(testMessage, bos.toString());
        } finally {
            httpServer.stop();
        }
    }

}
