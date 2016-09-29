package com.l7tech.external.assertions.websocket.client;


import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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
    private final static String SERVER_TRUSTSTORE_LOCATION = "./resources/servertruststore.jks";
    private final static String SERVER_TRUSTSTORE_PASS = "password";

    /**
     * Start Embedding Jetty server
     *
     */
    public void start() {
        try {
            // 1) Create a Jetty server with the 8091 port.
            //this.server = new Server(8091);
            server = new Server();

            ServerConnector nonSSLconnector = new ServerConnector(server);
            nonSSLconnector.setPort(8091);
            server.addConnector(nonSSLconnector);

            // 2) Register ChatWebSocketHandler in the Jetty server instance.
            ChatWebSocketHandler chatWebSocketHandler = new ChatWebSocketHandler();
            server.setHandler(chatWebSocketHandler);
            // 2) Start the Jetty server.

            server.setDumpAfterStart(true);
            server.start();

            if (server.isStarted()){
                System.out.println("non-SSL Jetty server started.");
            }


            // Create SSL Jetty Server
            sslServer = new Server();

            ChatWebSocketHandler sslChatWebSocketHandler = new ChatWebSocketHandler();
            sslServer.setHandler(sslChatWebSocketHandler);

            if (Resource.newResource(this.getClass().getResource(KEYSTORE_LOCATION)) == null){
                throw new Exception("keystore not found:"+this.getClass().getResource(".."));
            }

            SslContextFactory sslContextFactory = new SslContextFactory();
            Resource keyStoreResource = Resource.newResource(getClass().getResource(KEYSTORE_LOCATION));
            sslContextFactory.setKeyStoreResource(keyStoreResource);
            sslContextFactory.setKeyStorePassword(KEYSTORE_PASS);

            sslContextFactory.setTrustAll(true);
            sslContextFactory.setNeedClientAuth(false);
            sslContextFactory.setWantClientAuth(false);

            boolean bMutualAuth=false;
            if (bMutualAuth){

                if (Resource.newResource(this.getClass().getResource(SERVER_TRUSTSTORE_LOCATION)) == null){
                    throw new Exception("truststore not found in the parent folder:"+this.getClass().getResource(".."));
                }

                Resource trustStoreResource = Resource.newResource(getClass().getResource(SERVER_TRUSTSTORE_LOCATION));

                sslContextFactory.setTrustStoreResource(trustStoreResource);
                sslContextFactory.setTrustStorePassword(SERVER_TRUSTSTORE_PASS);


                sslContextFactory.setNeedClientAuth(true);
                sslContextFactory.setWantClientAuth(true);
            }

            SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
            HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(new HttpConfiguration());
            ServerConnector sslConnector = new ServerConnector(sslServer, sslConnectionFactory, httpConnectionFactory);
            sslConnector.setPort(8092);

            sslServer.addConnector(sslConnector);

            sslServer.start();

            if (sslServer.isStarted()){
                System.out.println("The SSL Jetty server started on port: 8092.");
            }

            sslServer.setDumpAfterStart(true);

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

        System.setProperty("org.eclipse.jetty.util.log.class","org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL","WARN");
        System.setProperty("org.eclipse.jetty.websocket.LEVEL","DEBUG");

        ChatServerTestServer chatServer = new ChatServerTestServer();
        chatServer.start();



    }
}
