package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.tls.ClientTrustingTrustManager;
import com.l7tech.util.CachedCallable;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cirving
 * Date: 5/31/12
 * Time: 9:38 AM
 */
public class WebSocketConnectionManager {
    protected static final Logger logger = Logger.getLogger(WebSocketConnectionManager.class.getName());

    private static WebSocketConnectionManager instance;
    private WebSocketClientFactory factory;
    private final Map<String, WebSocketClientFactory> outboundSslFactoryMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketInboundHandler> inboundHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketOutboundHandler> outboundHandlerMap = new ConcurrentHashMap<>();
    private QueuedThreadPool inboundQueuedThreadPool;
    private QueuedThreadPool outboundQueuedThreadPool;
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private DefaultKey defaultKey;

    public static void createConnectionManager(SsgKeyStoreManager keyStoreManager, TrustManager trustManager, SecureRandom secureRandom, DefaultKey defaultKey) throws IllegalStateException, WebSocketConnectionManagerException {
        if (instance != null) {
            logger.log(Level.WARNING, "Attempt to create a WebSocketConnectionManager that is already initialized");
            throw new IllegalStateException("WebSocket Connection Manager already initialized");
        }
        instance = new WebSocketConnectionManager(keyStoreManager, trustManager, secureRandom, defaultKey);
        try {
            instance.start();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to start WebSocketConnectionManager", e);
            instance = null;
            throw new WebSocketConnectionManagerException("WebSocket Factory failed to start");
        }
    }

    private WebSocketConnectionManager(SsgKeyStoreManager keyStoreManager, TrustManager trustManager, SecureRandom secureRandom, DefaultKey defaultKey) {
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.defaultKey = defaultKey;
    }

    public static WebSocketConnectionManager getInstance() throws WebSocketConnectionManagerException {
        if (instance == null ) {
            throw new WebSocketConnectionManagerException("Could not retrieve WebSocket Connection Manager");
        }

        return instance;
    }

    public boolean isStarted(String serviceId, boolean isInbound) {
        if ( isInbound ) {
            return inboundHandlerMap.containsKey(serviceId);
        } else {
            return outboundHandlerMap.containsKey(serviceId);
        }
    }

    private WebSocketClient getOutBoundWebSocket(String origin, String protocol, int maxIdleTime, WebSocketClientFactory factory) throws Exception {
        WebSocketClient client = factory.newWebSocketClient();
        client.setMaxBinaryMessageSize(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY));
        client.setMaxTextMessageSize(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY));
        client.setMaxIdleTime(maxIdleTime);
        client.setOrigin(origin);
        client.setProtocol(protocol);
        return client;
    }

    public WebSocketClient getOutBoundWebSocket(String origin, String protocol, int maxIdleTime) throws Exception {
        return getOutBoundWebSocket(origin, protocol, maxIdleTime, factory);
    }

    public WebSocketClient getOutBoundSslWebSocket(String origin, String protocol, int maxIdleTime, String handlerId) throws Exception {
        return getOutBoundWebSocket(origin, protocol, maxIdleTime, outboundSslFactoryMap.get(handlerId));
    }

    public void registerInboundHandler(String id, WebSocketInboundHandler handler) {
        inboundHandlerMap.put(id, handler);
    }

    public void deregisterInboundHandler(String handlerId) {
        inboundHandlerMap.remove(handlerId);
    }

    public WebSocketInboundHandler getInboundHandler(String handlerId) throws WebSocketConnectionManagerException {
        if (!inboundHandlerMap.containsKey(handlerId)) {
            logger.log(Level.WARNING, "No inbound handler exists for id " + handlerId);
            throw new WebSocketConnectionManagerException("No inbound handler exists for id " + handlerId);
        }
        return inboundHandlerMap.get(handlerId);
    }

    public void registerOutboundHandler(String id, WebSocketOutboundHandler handler, WebSocketConnectionEntity connection) {
        outboundHandlerMap.put(id, handler);
        if (connection.isOutboundSsl()) {
            startSsl(id, connection);
        }
    }

    public void deregisterOutboundHandler(String handlerId) {
        outboundHandlerMap.remove(handlerId);
        try {
            if (outboundSslFactoryMap.get(handlerId) != null) {
                outboundSslFactoryMap.remove(handlerId).stop();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to stop SslFactory");
        }
    }

    public WebSocketOutboundHandler getOutboundHandler(String handlerId) throws WebSocketConnectionManagerException {
        if (!outboundHandlerMap.containsKey(handlerId)) {
            logger.log(Level.WARNING, "No outbound handler exists for id " + handlerId);
            throw new WebSocketConnectionManagerException("No inbound handler exists for id " + handlerId);
        }
        return outboundHandlerMap.get(handlerId);
    }


    private QueuedThreadPool getOutboundThreadPool() {
        if (outboundQueuedThreadPool == null) {
            outboundQueuedThreadPool = new QueuedThreadPool();
            outboundQueuedThreadPool.setMaxThreads(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY));
            outboundQueuedThreadPool.setMaxThreads(WebSocketConstants.getClusterProperty(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY));
        }
        return outboundQueuedThreadPool;
    }

    public QueuedThreadPool getInboundThreadPool() {
        if (inboundQueuedThreadPool == null) {
            inboundQueuedThreadPool = new QueuedThreadPool();
            inboundQueuedThreadPool.setMaxThreads(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_INBOUND_THREADS_KEY));
            inboundQueuedThreadPool.setMinThreads(WebSocketConstants.getClusterProperty(WebSocketConstants.MIN_INBOUND_THREADS_KEY));
        }
        return inboundQueuedThreadPool;
    }

    private SsgKeyEntry getKeyEntry(WebSocketConnectionEntity connection, char direction) throws Exception {
        //Inbound
        if (direction == 'I') {
            if (Goid.isDefault(connection.getInboundPrivateKeyId())) {
                return defaultKey.getSslInfo();
            } else {
                return keyStoreManager.lookupKeyByKeyAlias(connection.getInboundPrivateKeyAlias(), connection.getInboundPrivateKeyId());
            }
        }

        //Outbound
        if (direction == 'O') {
            if (!connection.isOutboundClientAuthentication()) {
                return null;
            } else if (Goid.isDefault(connection.getOutboundPrivateKeyId())) {
                return defaultKey.getSslInfo();
            } else {
                return keyStoreManager.lookupKeyByKeyAlias(connection.getOutboundPrivateKeyAlias(), connection.getOutboundPrivateKeyId());
            }
        }

        return null;
    }

    private SSLContext createSslContext(WebSocketConnectionEntity connection, char direction) throws Exception {
        KeyManager[] keyManagers;

        SsgKeyEntry keyEntry = getKeyEntry(connection, direction);

        if ( keyEntry != null)  {
            keyManagers = new KeyManager[]{new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())};
        } else {
            keyManagers = new KeyManager[0];
        }
        TrustManager tm;
        if(direction=='I') {
            tm = new ClientTrustingTrustManager(new CachedCallable<>(30000L, new Callable<X509Certificate[]>() {
                @Override
                public X509Certificate[] call() throws Exception {
                    return new X509Certificate[0];
                }
            }));
        } else {
            tm = trustManager;
        }

        Provider provider = JceProvider.getInstance().getProviderFor("SSLContext.TLSv1");
        SSLContext sslContext = SSLContext.getInstance("TLSv1", provider);
        sslContext.init(keyManagers, new TrustManager[]{tm}, secureRandom);

        return sslContext;
    }

    public SslSelectChannelConnector getInboundSSLConnector(WebSocketConnectionEntity connection) throws Exception {
        SslContextFactory sslCtx = new SslContextFactory();
        sslCtx.setSslContext(createSslContext(connection, 'I'));
        sslCtx.setTrustAll(true);
        switch (connection.getInboundClientAuth()) {
            case NONE:
                sslCtx.setNeedClientAuth(false);
                sslCtx.setWantClientAuth(false);
                break;
            case OPTIONAL:
                sslCtx.setNeedClientAuth(false);
                sslCtx.setWantClientAuth(true);
                break;
            case REQUIRED:
                sslCtx.setNeedClientAuth(true);
                sslCtx.setWantClientAuth(true);
                break;
        }

        return new SslSelectChannelConnector(sslCtx);
    }

    private void start() throws Exception {
        factory = new WebSocketClientFactory(getOutboundThreadPool());
        factory.setBufferSize(WebSocketConstants.getClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY));
        factory.start();
    }

    private void startSsl(String id,WebSocketConnectionEntity connection) {
        WebSocketClientFactory sslFactory = new WebSocketClientFactory(getOutboundThreadPool());
        sslFactory.setBufferSize(WebSocketConstants.getClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY));
        try {
            sslFactory.getSslContextFactory().setSslContext(createSslContext(connection, 'O'));
            sslFactory.start();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to set SSL Context for outbound websocket connection");
        }

        outboundSslFactoryMap.put(id,sslFactory);
    }

}
