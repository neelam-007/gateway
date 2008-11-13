package com.l7tech.skunkworks;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.io.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.mail.internet.MimeUtility;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Test the way UrlConnectionHttpClient handles header values that need to be MIME encoded. 
 */
public class TestHttpHeaderQuoting implements Closeable {
    private static final Logger logger = Logger.getLogger(TestHttpHeaderQuoting.class.getName());
    private static final int PROXY_PORT = 8888;
    private static final int ECHO_SERVICE_PORT = 8889;

    private ByteArrayOutputStream requestCollector;
    private OutputStream concurrentRequestCollector;
    private ByteArrayOutputStream responseCollector;
    private OutputStream concurrentResponseCollector;
    private Closeable proxyHandle;
    private GenericHttpRequest httpRequest;
    private GenericHttpResponse httpResponse;
    private HttpServer httpServer;

    private void initEchoWebService() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(ECHO_SERVICE_PORT), 0);
        httpServer.createContext("/", new HttpHandler() {
            public void handle(HttpExchange hex) throws IOException {
                Headers reqHeaders = hex.getRequestHeaders();
                final InputStream requestBody = hex.getRequestBody();
                byte[] bodyBytes = IOUtils.slurpStream(requestBody);

                logRequestHeaders(reqHeaders);

                Headers respHeaders = hex.getResponseHeaders();
                for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
                    String name = entry.getKey();
                    List<String> values = entry.getValue();
                    for (String value : values)
                        respHeaders.add(name, value);
                }
                hex.sendResponseHeaders(200, 0);

                OutputStream out = hex.getResponseBody();
                out.write(bodyBytes);
                out.close();
            }
        });
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
    }

    private void logRequestHeaders(Headers reqHeaders) throws UnsupportedEncodingException {
        for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
            for (String value : entry.getValue()) {
                logger.info("Saw request header (unencoded): " + entry.getKey() + ": " + MimeUtility.decodeText(value));
            }
        }        
    }

    private void initCollectors() {
        requestCollector = new ByteArrayOutputStream();
        concurrentRequestCollector = new ConcurrentOutputStream(requestCollector);

        responseCollector = new ByteArrayOutputStream();
        concurrentResponseCollector = new ConcurrentOutputStream(responseCollector);
    }

    private void initCollectionProxy() throws IOException {
        proxyHandle = TcpPlugProxy.createCustomProxy(PROXY_PORT, 5, "localhost", ECHO_SERVICE_PORT,
                new Functions.Quaternary<TcpPlugProxy, ServerSocket, ExecutorService, String, Integer>()
                {
                    public TcpPlugProxy call(ServerSocket serverSocket, ExecutorService executorService, String targetHost, Integer targetPort) {
                        return new TcpPlugProxy(serverSocket, executorService, targetHost, targetPort) {
                            protected OutputStream getToClientOutputStream(Socket client) throws IOException {
                                return new TeeOutputStream(super.getToClientOutputStream(client), new NonCloseableOutputStream(concurrentResponseCollector));
                            }

                            protected OutputStream getToTargetOutputStream(Socket target) throws IOException {
                                return new TeeOutputStream(super.getToTargetOutputStream(target), new NonCloseableOutputStream(concurrentRequestCollector));
                            }
                        };
                    }
                });
    }

    private void transmitRequestAndSlurpResponse() throws IOException {
        GenericHttpClient httpClient = new UrlConnectionHttpClient();
        GenericHttpRequestParams params = new GenericHttpRequestParams();
        params.setTargetUrl(new URL("http", "localhost", PROXY_PORT, "/"));
        params.addExtraHeader(new GenericHttpHeader("X-Unicode-Test", "test value with whitespace\n\n\t  blah  \t\n\n  and some unicode: \u0436\u2665\u0152"));
        params.addExtraHeader(new GenericHttpHeader("X-No-Quoting-Required", "testvaluethatdoestneedquoting"));
        params.addExtraHeader(new GenericHttpHeader("X-Only-Space-Characters", "test value that has inner spaces but no tabs"));
        params.addExtraHeader(new GenericHttpHeader("X-Already-Quoted", MimeUtility.encodeText("test value that was already pre-quoted: \u0436\u2665\u0152", "utf-8", "Q")));
        params.addExtraHeader(new GenericHttpHeader("X-Like-Content-Type", "text/xml; charset=utf-8"));
        params.setContentType(ContentTypeHeader.XML_DEFAULT);
        httpRequest = httpClient.createRequest(HttpMethod.POST, params);
        httpRequest.setInputStream(new ByteArrayInputStream("<test>blah blah blah</test>".getBytes()));
        httpResponse = httpRequest.getResponse();
        IOUtils.slurpStream(httpResponse.getInputStream());
    }

    private void runTest() throws IOException {
        initEchoWebService();
        initCollectors();
        initCollectionProxy();
        transmitRequestAndSlurpResponse();
    }

    public void close() throws IOException {
        ResourceUtils.closeQuietly(httpResponse);
        ResourceUtils.closeQuietly(httpRequest);
        ResourceUtils.closeQuietly(proxyHandle);
        if (httpServer != null) httpServer.stop(0);
    }

    public static void main(String[] args) throws IOException {
        final TestHttpHeaderQuoting test = new TestHttpHeaderQuoting();
        try {
            test.runTest();

            logger.info("Raw request as sent over the wire: " + new String(test.requestCollector.toByteArray(), "utf8"));
            logger.info("Raw response as received over the wire: " + new String(test.responseCollector.toByteArray(), "utf8"));

        } finally {
            test.close();
        }
    }

}
