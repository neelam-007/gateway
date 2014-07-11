package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/27/12
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class StreamingTestServer {

    private Server server = null;
    /**
     * Start Embedding Jetty server
     *
     */
    public void start() {
        try {
            // 1) Create a Jetty server with the 9999 port.
            this.server = new Server(9999);
            // 2) Register ChatWebSocketHandler in the Jetty server instance.
            StreamingWebSocketHandler streamingWebSocketHandler = new StreamingWebSocketHandler();
            streamingWebSocketHandler.setHandler(new DefaultHandler());
            server.setHandler(streamingWebSocketHandler);
            // 2) Start the Jetty server.
            server.start();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop Embedding Jetty server .
     */
    public void stop() {
        if (server != null) {
            try {
                // stop the Jetty server.
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        StreamingTestServer streamingTestServer = new StreamingTestServer();
        streamingTestServer.start();
    }
}
