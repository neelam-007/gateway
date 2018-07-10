package com.l7tech.external.assertions.websocket.client;

import com.l7tech.external.assertions.websocket.server.WebSocketConnectionManagerException;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/19/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketClientConnectionManager {

    protected static final Logger logger = Logger.getLogger(WebSocketClientConnectionManager.class.getName());

    private final static String CLIENT_KEYSTORE_LOCATION = "./resources/clientkeystore.jks";
    private final static String CLIENT_KEYSTORE_PASS = "password";

    // the truststore we use for our server. This keystore should contain all the keys
    // that are allowed to make a connection to the server
    private final static String CLIENT_TRUSTSTORE_LOCATION = "./resources/clienttruststore.jks";
    private final static String CLIENT_TRUSTSTORE_PASS = "password";

    private static WebSocketClientConnectionManager instance;
    private QueuedThreadPool outboundQueuedThreadPool;

    public static synchronized void createConnectionManager() throws IllegalStateException, WebSocketConnectionManagerException {

        if (instance != null) {
            logger.log(Level.WARNING, "Attempt to create a WebSocketConnectionManager that is already initialized");
            throw new IllegalStateException("WebSocket Connection Manager already initialized");
        }
        instance = new WebSocketClientConnectionManager();
    }


    public static synchronized WebSocketClientConnectionManager getInstance() {
        return instance;
    }

    public void startWSClient(OutboundSocket socket, boolean isSsl, String dest, int maxIdleTime, Map<String, String> headers) throws Exception {

        WebSocketClient client;

        if (isSsl) {
            client = getSslWwebSocketClient();
        }
        else {
            client = new WebSocketClient();
        }

        client.start();

        if (client.isStarted()){
            System.out.println("WebSocket client started.");
        }else {
            System.out.println("WebSocket client failed to start.");
        }

        URI echoUri = new URI(dest);

        ClientUpgradeRequest request = new ClientUpgradeRequest();

        addCustomHeaders(request,headers);
        request.addExtensions("permessage-deflate");

        client.connect(socket, echoUri, request);

        client.getPolicy().setMaxBinaryMessageSize(1048576);
        client.getPolicy().setMaxTextMessageSize(65536);
        client.getPolicy().setIdleTimeout(maxIdleTime);


    }

    public WebSocketClient getSslWwebSocketClient() throws Exception {

        WebSocketClient client;

        if (Resource.newResource(this.getClass().getResource(CLIENT_KEYSTORE_LOCATION)) == null){
            throw new Exception("client keystore not found in the parent folder:"+this.getClass().getResource(".."));
        }

        Resource keyStoreResource = Resource.newResource(getClass().getResource(CLIENT_KEYSTORE_LOCATION));

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreResource(keyStoreResource);
        sslContextFactory.setKeyStoreType("JKS");
        sslContextFactory.setKeyStorePassword(CLIENT_KEYSTORE_PASS);
        sslContextFactory.setKeyManagerPassword("password");

        if (Resource.newResource(this.getClass().getResource(CLIENT_TRUSTSTORE_LOCATION)) == null){
            throw new Exception("client truststore not found in parent folder:"+this.getClass().getResource(".."));
        }

        // TrustStore required for client side authentication.
        Resource trustStoreResource = Resource.newResource(this.getClass().getResource(CLIENT_TRUSTSTORE_LOCATION));
        sslContextFactory.setTrustStoreResource(trustStoreResource);
        sslContextFactory.setTrustStorePassword(CLIENT_TRUSTSTORE_PASS);
        sslContextFactory.setTrustStoreType("JKS");

        client = new WebSocketClient(sslContextFactory);

        return client;
    }


    public OutboundSocket getOutboundWebSocket(String dest, int maxIdleTime, Map<String, String> headers) throws Exception {

        OutboundSocket socket = new OutboundSocket();

        startWSClient(socket,false,dest,maxIdleTime,headers);

        return socket;
    }

    public OutboundSocket getOutboundSslWebSocket(String dest, int maxIdleTime, Map<String, String> headers) throws Exception {

        OutboundSocket socket = new OutboundSocket();

        startWSClient(socket,true,dest,maxIdleTime,headers);

        return socket;
    }


    private void addCustomHeaders(ClientUpgradeRequest request, Map<String, String> headers) {
        if (headers != null) {
            Set<String> headerKeys = headers.keySet();
            for (String h : headerKeys) {
                request.setHeader(h, headers.get(h));
            }
        }
    }

}
