package com.l7tech.common.http.prov.apache.components;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.IdentityBindingHttpConnectionManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BugId;
import com.l7tech.util.IOUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.client.CircularRedirectException;
import org.junit.*;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 4/5/13
 */
public class HttpComponentsClientTest {

    HttpComponentsClient fixture;

    @Before
    public void setUp() throws Exception {
        fixture = new HttpComponentsClient();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Ignore
    @Test
    public void testCreateRequest() throws Exception {
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.setTargetUrl(new URL("http://www.ca.com"));
        GenericHttpRequest request = null;
        try {
            request = fixture.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null) {
                request.close();
            }
        }
    }

    @Test
    public void testCreateRequestWithInvalidAuthorizationHeader() throws Exception {
        final String[] sawRequestMethod = new String[1];

        MockHttpServer httpServer = new MockHttpServer(17802);
        httpServer.setHttpHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                sawRequestMethod[0] = exchange.getRequestHeaders().getFirst("Authorization");
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            }
        });
        httpServer.start();

        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.setExtraHeaders(new HttpHeader[]{new HttpHeader() {
            @Override
            public String getName() {
                return "Authorization";
            }

            @Override
            public String getFullValue() {
                return "bearer 1";
            }
        }});
        requestParams.setTargetUrl(new URL("http://localhost:" + httpServer.getPort()));
        GenericHttpRequest request = null;
        try {
            fixture = new HttpComponentsClient(new IdentityBindingHttpConnectionManager(), -1, -1);
            request = fixture.createRequest(HttpMethod.GET, requestParams);
            assertTrue(request != null);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            assertEquals("bearer 1", sawRequestMethod[0]);
        } finally {
            if (request != null) {
                request.close();
            }
        }
    }

    @Ignore
    @Test
    public void testPostRequest() throws Exception {
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.setContentType(ContentTypeHeader.XML_DEFAULT);
        requestParams.setExtraHeaders(new HttpHeader[]{new GenericHttpHeader("SOAPAction", "http://echoall.com/ws/Echo2")});
        requestParams.setTargetUrl(new URL("http://hugh/EchoInfoWS/Service1.asmx"));
        RerunnableHttpRequest request = null;
        try {
            request = (RerunnableHttpRequest) fixture.createRequest(HttpMethod.POST, requestParams);
            request.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory() {
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://echoall.com/ws\" xmlns:oas=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <ws:Echo2>\n" +
                            "         <!--Optional:-->\n" +
                            "         <ws:nid oas:Id=\"1\">\n" +
                            "            <!--Optional:-->\n" +
                            "            <ws:name>Dummy</ws:name>\n" +
                            "         </ws:nid>\n" +
                            "         <!--Optional:-->\n" +
                            "         <ws:isboyid oas:Id4=\"1\">\n" +
                            "            <ws:isboy>false</ws:isboy>\n" +
                            "         </ws:isboyid>\n" +
                            "      </ws:Echo2>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>").getBytes());
                }
            });

            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null)
                request.close();
        }
    }

    @Ignore
    @Test
    public void testNtlmAuthentication() throws Exception {
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.setContentType(ContentTypeHeader.XML_DEFAULT);
        requestParams.setNtlmAuthentication(new NtlmAuthentication("administrator", "7layer".toCharArray(), "seattle", "hugh"));
        requestParams.setTargetUrl(new URL("http://hugh.l7tech.com:82/ACMEWarehouseNTLM/Service1.asmx"));
        GenericHttpRequest request = null;
        try {
            request = fixture.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null) {
                request.close();
            }
        }
    }

    @Ignore
    @Test
    public void testNoAuthentication() throws Exception {
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.setContentType(ContentTypeHeader.XML_DEFAULT);
        requestParams.setTargetUrl(new URL("http://hugh.l7tech.com:82/ACMEWarehouseNTLM/Service1.asmx"));
        GenericHttpRequest request = null;
        try {
            request = fixture.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(401, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null) {
                request.close();
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

    @Test
    public void testCustomRequestMethod() throws Exception {
        final String[] sawRequestMethod = { null };

        MockHttpServer httpServer = new MockHttpServer(17801);
        httpServer.setHttpHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                sawRequestMethod[0] = exchange.getRequestMethod();
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            }
        });
        httpServer.start();

        try {
            GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
            requestParams.setMethodAsString("MyCustomFancyHttpVerb");
            requestParams.setTargetUrl(new URL("http://localhost:" + httpServer.getPort()));
            RerunnableHttpRequest request = (RerunnableHttpRequest) fixture.createRequest(HttpMethod.OTHER, requestParams);
            request.setInputStream(new ByteArrayInputStream(("<test>test message</test>").getBytes()));
            GenericHttpResponse response = request.getResponse();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copyStream(response.getInputStream(), bos);
        } finally {
            httpServer.stop();
        }

        assertEquals(sawRequestMethod[0], "MyCustomFancyHttpVerb");
    }

    @Ignore
    @Test
    public void testCreateRequestWithSpecialCharacter() throws Exception {
        GenericHttpRequestParams requestParams = new GenericHttpRequestParams();
        requestParams.setTargetUrl(new URL("http://amaux/schema/simple%20addition/addition_schema2.xsd"));
        GenericHttpRequest request = null;
        try {
            request = fixture.createRequest(HttpMethod.GET, requestParams);
            GenericHttpResponse response = request.getResponse();
            assertEquals(200, response.getStatus());
            IOUtils.copyStream(response.getInputStream(), System.out);
        } finally {
            if (request != null) {
                request.close();
            }
        }
    }

    @BugId("DE329111")
    @Test
    public void testCreateRequestWith301FollowRedirect() throws Exception {
        final String[] sawRequestMethod = { null };
        MockHttpServer httpServer = new MockHttpServer(17801);
        String api1 = "/api1";
        String api2 = "/api2";
        String serverUrl = "http://localhost:" + httpServer.getPort();

        httpServer.setHttpHandler(new HttpHandler() {
            private Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                sawRequestMethod[0] = exchange.getRequestMethod();
                // Redirect api1 to api2
                if (api1.equalsIgnoreCase(exchange.getRequestURI().getPath())) {
                    exchange.getResponseHeaders().set("location", serverUrl + api2);
                    exchange.sendResponseHeaders(301, 0);
                }
                if (api2.equalsIgnoreCase(exchange.getRequestURI().getPath())) {
                    exchange.sendResponseHeaders(200, 0);
                }
                exchange.close();
            }
        });
        httpServer.start();

        try {
            GenericHttpRequestParams requestParams = new GenericHttpRequestParams();

            requestParams.setFollowRedirects(true);
            requestParams.setMethodAsString("GET");
            requestParams.setTargetUrl(new URL(serverUrl + api1));
            RerunnableHttpRequest request = (RerunnableHttpRequest) fixture.createRequest(HttpMethod.OTHER, requestParams);
            request.setInputStream(new ByteArrayInputStream(("<test>test message</test>").getBytes()));
            request.getResponse();

            request = (RerunnableHttpRequest) fixture.createRequest(HttpMethod.OTHER, requestParams);
            request.setInputStream(new ByteArrayInputStream(("<test>test message</test>").getBytes()));
            request.getResponse();

        } finally {
            httpServer.stop();
        }
        assertEquals(sawRequestMethod[0], "GET");
    }

    @BugId("DE329111")
    @Test
    public void testCreateRequestWith301CircularRedirect() throws Exception {
        final String[] sawRequestMethod = { null };
        MockHttpServer httpServer = new MockHttpServer(17801);
        String api1 = "/api1";
        String api2 = "/api2";
        String serverUrl = "http://localhost:" + httpServer.getPort();

        httpServer.setHttpHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                sawRequestMethod[0] = exchange.getRequestMethod();
                //Circular Redirect api1 will redirect to api2
                if (api1.equalsIgnoreCase(exchange.getRequestURI().getPath())) {
                    exchange.getResponseHeaders().set("location", serverUrl + api2);
                    exchange.sendResponseHeaders(301, 0);
                }
                //Circular Redirect api2 will redirect back to api1
                if (api2.equalsIgnoreCase(exchange.getRequestURI().getPath())) {
                    exchange.getResponseHeaders().set("location", serverUrl + api1);
                    exchange.sendResponseHeaders(301, 0);
                }
                exchange.close();
            }
        });
        httpServer.start();

        try {
            GenericHttpRequestParams requestParams = new GenericHttpRequestParams();

            requestParams.setFollowRedirects(true);
            requestParams.setMethodAsString("GET");
            requestParams.setTargetUrl(new URL(serverUrl + api1));
            RerunnableHttpRequest request = (RerunnableHttpRequest) fixture.createRequest(HttpMethod.OTHER, requestParams);
            request.setInputStream(new ByteArrayInputStream(("<test>test message</test>").getBytes()));
            request.getResponse();
        } catch (Exception e) {
            if(!(e.getCause().getCause() instanceof CircularRedirectException)){
                Assert.fail("unexpected exception");
            }
        }finally {
            httpServer.stop();
        }
    }
}
