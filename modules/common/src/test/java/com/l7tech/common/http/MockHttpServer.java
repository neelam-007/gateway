package com.l7tech.common.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public class MockHttpServer {

    private Integer port = null;
    private boolean started = false;

    private HttpServer httpServer;
    public int responseCode = HttpsURLConnection.HTTP_OK;
    private byte[] responseContent = new byte[0];
    private Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();

    public MockHttpServer(int port) {
        this.port = port;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent.getBytes();
    }

    public void addResponseHeader(String key, List<String> values) {
        this.responseHeaders.put(key, values);
    }

    public void clearResponse() {
        this.responseCode = HttpsURLConnection.HTTP_OK;
        this.responseContent = new byte[0];
        this.responseHeaders.clear();
    }

    public Integer getPort() {
        return port;
    }

    public void start() {

        if (!started) {
            //only retry 10 times
            int retry = 10;
            while (httpServer == null) {
                try {
                    httpServer = HttpServer.create(new InetSocketAddress(port), 0);
                } catch (IOException e) {
                    if (retry-- <= 0) {
                        throw new RuntimeException("Unable to start Mock Http Server.");
                    }
                    port++;
                }
            }
            httpServer.createContext("/", new MyHandler());
            httpServer.setExecutor(null); // creates a default executor
            httpServer.start();
            started = true;
        }
    }

    public void stop() throws IOException {
        try {
            if (started) {
                started = false;
                httpServer.stop(0);
            }
        } finally {
            httpServer = null;
        }
    }

    public class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            int b;
            StringBuilder sb = new StringBuilder();
            InputStream is = t.getRequestBody();
            is = t.getRequestBody();

            while ((b = is.read()) != -1) {
                sb.append((char) b);
            }
            is.close();


            if (responseHeaders != null && responseHeaders.size() > 0) {
                Headers respHeaders = t.getResponseHeaders();
                for (String key : responseHeaders.keySet()) {
                    respHeaders.put(key, responseHeaders.get(key));
                }
            }
            t.sendResponseHeaders(responseCode, responseContent.length);
            OutputStream os = t.getResponseBody();
            os.write(responseContent);
            is.close();
            t.close();
        }
    }

}
