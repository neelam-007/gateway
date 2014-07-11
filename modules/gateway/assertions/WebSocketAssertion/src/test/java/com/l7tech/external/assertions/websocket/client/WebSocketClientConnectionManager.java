package com.l7tech.external.assertions.websocket.client;

import com.l7tech.external.assertions.websocket.server.WebSocketConnectionManagerException;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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

    private final static String KEYSTORE_LOCATION = "./resources/keystore.jks";
    private final static String KEYSTORE_PASS = "password";

    // the truststore we use for our server. This keystore should contain all the keys
    // that are allowed to make a connection to the server
    private final static String TRUSTSTORE_LOCATION = "./resources/truststore.jks";
    private final static String TRUSTSTORE_PASS = "password";

    private static WebSocketClientConnectionManager instance;
    private WebSocketClientFactory factory;
    private WebSocketClientFactory sslFactory;
    private QueuedThreadPool outboundQueuedThreadPool;

    public static void createConnectionManager() throws IllegalStateException, WebSocketConnectionManagerException {
        if (instance != null) {
            logger.log(Level.WARNING, "Attempt to create a WebSocketConnectionManager that is already initialized");
            throw new IllegalStateException("WebSocket Connection Manager already initialized");
        }
        instance = new WebSocketClientConnectionManager();
        try {
            instance.start();
            instance.startSsl();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to start WebSocketConnectionManager", e);
            instance = null;
            throw new WebSocketConnectionManagerException("WebSocket Factory failed to start");
        }
    }

    private WebSocketClientConnectionManager() {

    }

    public static WebSocketClientConnectionManager getInstance() {
        return instance;
    }

    private WebSocketClient getOutBoundWebSocket(String origin, String protocol, int maxIdleTime, WebSocketClientFactory factory) throws Exception {
        WebSocketClient client = factory.newWebSocketClient();
        client.setMaxBinaryMessageSize(1048576);
        client.setMaxTextMessageSize(65536);
        client.setMaxIdleTime(maxIdleTime);
        client.setOrigin(origin);
        client.setProtocol(protocol);
        return client;
    }

    public WebSocketClient getOutBoundWebSocket(String origin, String protocol, int maxIdleTime) throws Exception {
        return getOutBoundWebSocket(origin, protocol, maxIdleTime, factory);
    }

    public WebSocketClient getOutBoundSslWebSocket(String origin, String protocol, int maxIdleTime) throws Exception {
        return getOutBoundWebSocket(origin, protocol, maxIdleTime, sslFactory);
    }

    private QueuedThreadPool getOutboundThreadPool() {
        if (outboundQueuedThreadPool == null) {
            outboundQueuedThreadPool = new QueuedThreadPool();
            outboundQueuedThreadPool.setMaxThreads(25);
            outboundQueuedThreadPool.setMaxThreads(10);
        }
        return outboundQueuedThreadPool;
    }

    private void start() throws Exception {
        factory = new WebSocketClientFactory(getOutboundThreadPool());
        factory.setBufferSize(4096);
        factory.start();
    }

    private KeyStore loadTrustStore() {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = TRUSTSTORE_PASS.toCharArray();
            File file = new File(TRUSTSTORE_LOCATION);
            FileInputStream fis = new java.io.FileInputStream(file);
            ks.load(fis, password);
            fis.close();
            return ks;
        } catch (KeyStoreException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CertificateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    private KeyStore loadKeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = KEYSTORE_PASS.toCharArray();
            FileInputStream fis = new java.io.FileInputStream(KEYSTORE_LOCATION);
            ks.load(fis, password);
            fis.close();
            return ks;
        } catch (KeyStoreException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CertificateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }


    public void startSsl() {
        sslFactory = new WebSocketClientFactory(getOutboundThreadPool());
        sslFactory.setBufferSize(4096);
        try {
            sslFactory.getSslContextFactory().setKeyStore(loadKeyStore());
            sslFactory.getSslContextFactory().setKeyStorePassword(KEYSTORE_PASS);
            sslFactory.getSslContextFactory().setTrustStore(loadTrustStore());
            sslFactory.getSslContextFactory().setTrustStorePassword(TRUSTSTORE_PASS);
            sslFactory.start();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to set SSL Context for outbound websocket connection");
        }
    }

}
