package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;

/**
 * Application Lifecycle Listener implementation for start/stop Embedding Jetty
 * Server configured to manage Chat WebSocket with {@link ChatWebSocketHandler}.
 *
 */
public class ChatServerTestServer {

    private Server server = null;
    private Server sslServer = null;
    private final static String KEYSTORE_LOCATION = "./resources/serverkeystore.jks";
    private final static String KEYSTORE_PASS = "password";

    /**
     * Start Embedding Jetty server
     *
     */
    public void start() {
        try {
            // 1) Create a Jetty server with the 8091 port.
            this.server = new Server(8091);
            // 2) Register ChatWebSocketHandler in the Jetty server instance.
            ChatWebSocketHandler chatWebSocketHandler = new ChatWebSocketHandler();
            chatWebSocketHandler.setHandler(new DefaultHandler());
            server.setHandler(chatWebSocketHandler);
            // 2) Start the Jetty server.
            server.start();

            this.sslServer = new Server();
            ChatWebSocketHandler sslChatWebSocketHandler = new ChatWebSocketHandler();
            sslChatWebSocketHandler.setHandler(new DefaultHandler());
            sslServer.setHandler(sslChatWebSocketHandler);

            SslContextFactory sslContextFactory = new SslContextFactory(KEYSTORE_LOCATION);
            sslContextFactory.setKeyStorePassword(KEYSTORE_PASS);
            sslContextFactory.setTrustAll(true);
            sslContextFactory.setNeedClientAuth(false);

            SslSelectChannelConnector connector = new SslSelectChannelConnector(sslContextFactory);
            connector.setPort(8092);

            sslServer.setConnectors(new Connector[] { connector });

            sslServer.start();
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
        if (sslServer != null) {
            try {
                // stop the Jetty server.
                sslServer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServerTestServer chatServer = new ChatServerTestServer();
        chatServer.start();



    }
}
