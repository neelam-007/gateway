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
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.masks.ZeroMasker;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
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
    private final Map<String, SslContextFactory> outboundSslFactoryMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, WebSocketClient>> handlerIdWebSocketClientMapMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketInboundHandler> inboundHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketOutboundHandler> outboundHandlerMap = new ConcurrentHashMap<>();
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private DefaultKey defaultKey;

    public static synchronized void createConnectionManager(SsgKeyStoreManager keyStoreManager, TrustManager trustManager, SecureRandom secureRandom, DefaultKey defaultKey) throws IllegalStateException, WebSocketConnectionManagerException {

        if (instance == null) {
            instance = new WebSocketConnectionManager(keyStoreManager, trustManager, secureRandom, defaultKey);
        } else {
            logger.log(Level.WARNING, "Attempt to create a WebSocketConnectionManager that is already initialized");
        }
    }

    private WebSocketConnectionManager(SsgKeyStoreManager keyStoreManager, TrustManager trustManager, SecureRandom secureRandom, DefaultKey defaultKey) {
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.defaultKey = defaultKey;

    }

    public static WebSocketConnectionManager getInstance() throws WebSocketConnectionManagerException {
        if (instance == null) {
            throw new WebSocketConnectionManagerException("Could not retrieve WebSocket Connection Manager");
        }

        return instance;
    }

    public boolean isStarted(String serviceId, boolean isInbound) {
        if (isInbound) {
            return inboundHandlerMap.containsKey(serviceId);
        } else {
            return outboundHandlerMap.containsKey(serviceId);
        }
    }

    private void initWebSocketClient(WebSocketClient client, int maxIdleTime) {

        client.getPolicy().setIdleTimeout(maxIdleTime);

        int connectionTimeOut = WebSocketConstants.getClusterProperty(WebSocketConstants.CONNECT_TIMEOUT_KEY) * 1000;
        if (connectionTimeOut > 0) {
            client.setConnectTimeout(connectionTimeOut);
        }

        int maxTextMessageSize = WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY);
        if (maxTextMessageSize > 0) {
            client.getPolicy().setMaxTextMessageSize(maxTextMessageSize);
        }

        int maxBinaryMessageSize = WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY);
        if (maxBinaryMessageSize > 0) {
            client.getPolicy().setMaxBinaryMessageSize(maxBinaryMessageSize);
        }

        int maxClientBufferSize = WebSocketConstants.getClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY);
        if (maxClientBufferSize > 0) {
            client.getPolicy().setMaxTextMessageBufferSize(maxClientBufferSize);
            client.getPolicy().setMaxBinaryMessageBufferSize(maxClientBufferSize);
        }

        //TAC-1982 Compress outbound messages  - install the extension.
        client.getExtensionFactory().register("permessage-deflate", PerMessageDeflateExtension.class);
    }

    public WebSocketClient getOutboundWebSocketClient(boolean isSsl, String handlerId, String outboundUrl, int maxIdleTime, WebSocketConnectionEntity webSocketConnectionEntity) throws Exception {

        Map<String, WebSocketClient> outboundWebSocketClientMap = handlerIdWebSocketClientMapMap.get(handlerId);

        if (outboundWebSocketClientMap == null) {
            outboundWebSocketClientMap = new ConcurrentHashMap<>();
            Map<String, WebSocketClient> outboundWebSocketClientMapPrev = handlerIdWebSocketClientMapMap.putIfAbsent(handlerId, outboundWebSocketClientMap);
            if (outboundWebSocketClientMapPrev != null) {
                outboundWebSocketClientMap = outboundWebSocketClientMapPrev;
            }
        }

        WebSocketClient webSocketClient = outboundWebSocketClientMap.get(outboundUrl);

        if (webSocketClient == null) {

            if (isSsl) {
                SslContextFactory sslCtxFactory = getOutboundSslCtxFactory(webSocketConnectionEntity);
                SslContextFactory sslCtxFactoryPrev = outboundSslFactoryMap.putIfAbsent(webSocketConnectionEntity.getId(), sslCtxFactory);
                if (sslCtxFactoryPrev != null) {
                    sslCtxFactory = sslCtxFactoryPrev;
                }

                webSocketClient = new WebSocketClient(sslCtxFactory);

            } else {
                webSocketClient = new WebSocketClient();
            }

            initWebSocketClient(webSocketClient, maxIdleTime);
            closeAnyIdleWebSocketClients(handlerId);

            webSocketClient.setExecutor(getOutboundThreadPool());
            webSocketClient.setMasker(new ZeroMasker());

            WebSocketClient webSocketClientPrev = outboundWebSocketClientMap.putIfAbsent(outboundUrl, webSocketClient);
            if (webSocketClientPrev != null) {
                webSocketClient = webSocketClientPrev;
            }

        }
        if (!webSocketClient.isStarted()) {
            webSocketClient.start();
        }

        return webSocketClient;
    }

    private void closeAnyIdleWebSocketClients(String handlerId) {

        Map<String, WebSocketClient> outboundWebSocketClientMap = handlerIdWebSocketClientMapMap.get(handlerId);

        if (outboundWebSocketClientMap != null) {
            for (Map.Entry<String, WebSocketClient> entry : outboundWebSocketClientMap.entrySet()) {
                Set<WebSocketSession> webSocketClientSet = entry.getValue().getOpenSessions();
                if (webSocketClientSet.isEmpty()) {
                    try {
                        entry.getValue().stop();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Exception stopping WebSocketClient:" + e.toString());
                    } finally {
                        outboundWebSocketClientMap.remove(entry.getKey());
                    }
                }
            }
        }
    }

    private void closeAllWebSocketClients(String handlerId) {

        Map<String, WebSocketClient> outboundWebSocketClientMap = handlerIdWebSocketClientMapMap.get(handlerId);

        if (outboundWebSocketClientMap != null) {
            for (Map.Entry<String, WebSocketClient> entry : outboundWebSocketClientMap.entrySet()) {
                Set<WebSocketSession> webSocketClientSet = entry.getValue().getOpenSessions();

                if (webSocketClientSet != null) {
                    for (WebSocketSession webSocketSession : webSocketClientSet) {
                        webSocketSession.close();
                    }
                }

                try {
                    entry.getValue().stop();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception stopping WebSocketClient:" + e.toString());
                } finally {
                    outboundWebSocketClientMap.remove(entry.getKey());
                }
            }
        }
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

    public void registerOutboundHandler(String id, WebSocketOutboundHandler handler) {
        outboundHandlerMap.put(id, handler);
    }

    public void deregisterOutboundHandler(String handlerId) {
        outboundHandlerMap.remove(handlerId);
        try {
            closeAllWebSocketClients(handlerId);

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

        QueuedThreadPool outboundQueuedThreadPool = new QueuedThreadPool();

        int minOutboundThreads = WebSocketConstants.getClusterProperty(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY);
        if (minOutboundThreads > 0) {
            outboundQueuedThreadPool.setMinThreads(minOutboundThreads);
        }

        int maxOutboundThreads = WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY);
        if (maxOutboundThreads > 0) {
            outboundQueuedThreadPool.setMaxThreads(maxOutboundThreads);
        }

        return outboundQueuedThreadPool;
    }


    private SsgKeyEntry getKeyEntry(WebSocketConnectionEntity connection, WebSocketConstants.ConnectionType direction) throws Exception {
        //Inbound
        if (direction == WebSocketConstants.ConnectionType.Inbound) {
            if (Goid.isDefault(connection.getInboundPrivateKeyId())) {
                return defaultKey.getSslInfo();
            } else {
                return keyStoreManager.lookupKeyByKeyAlias(connection.getInboundPrivateKeyAlias(), connection.getInboundPrivateKeyId());
            }
        }

        //Outbound
        if (direction == WebSocketConstants.ConnectionType.Outbound) {
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

    private SSLContext createSslContext(WebSocketConnectionEntity connection, WebSocketConstants.ConnectionType direction) throws Exception {
        KeyManager[] keyManagers;

        SsgKeyEntry keyEntry = getKeyEntry(connection, direction);

        if (keyEntry != null) {
            keyManagers = new KeyManager[]{new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())};
        } else {
            keyManagers = new KeyManager[0];
        }
        TrustManager tm;
        if (direction == WebSocketConstants.ConnectionType.Inbound) {
            tm = new ClientTrustingTrustManager(new CachedCallable<>(30000L, new Callable<X509Certificate[]>() {
                @Override
                public X509Certificate[] call() throws Exception {
                    return new X509Certificate[0];
                }
            }));
        } else {
            tm = trustManager;
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        JceProvider.getInstance().prepareSslContext(sslContext);
        sslContext.init(keyManagers, new TrustManager[]{tm}, secureRandom);

        return sslContext;
    }

    public SslContextFactory getInboundSslCtxFactory(WebSocketConnectionEntity webSocketConnectionEntity) throws Exception {
        SslContextFactory sslContextFactory = new SslContextFactory();

        sslContextFactory.setExcludeCipherSuites();
        sslContextFactory.setExcludeProtocols();

        if (webSocketConnectionEntity.getInboundCipherSuites() == null) {
            sslContextFactory.setIncludeCipherSuites(WebSocketLoadListener.getDefaultCipherSuiteNames());
        } else {
            sslContextFactory.setIncludeCipherSuites(webSocketConnectionEntity.getInboundCipherSuites());
        }

        if (webSocketConnectionEntity.getInboundTlsProtocols() == null) {
            sslContextFactory.setIncludeProtocols(WebSocketConstants.DEFAULT_TLS_PROTOCOL_LIST);
        } else {
            sslContextFactory.setIncludeProtocols(webSocketConnectionEntity.getInboundTlsProtocols());
        }

        sslContextFactory.setSslContext(createSslContext(webSocketConnectionEntity, WebSocketConstants.ConnectionType.Inbound));
        sslContextFactory.setTrustAll(true);
        switch (webSocketConnectionEntity.getInboundClientAuth()) {
            case NONE:
                sslContextFactory.setNeedClientAuth(false);
                sslContextFactory.setWantClientAuth(false);
                break;
            case OPTIONAL:
                sslContextFactory.setNeedClientAuth(false);
                sslContextFactory.setWantClientAuth(true);
                break;
            case REQUIRED:
                sslContextFactory.setNeedClientAuth(true);
                sslContextFactory.setWantClientAuth(true);
                break;
            default:
                throw new IllegalStateException("The ClientAuthType was not handled. Could not configure and return the SslContextFactory.");
        }

        return sslContextFactory;
    }

    private SslContextFactory getOutboundSslCtxFactory(WebSocketConnectionEntity webSocketConnectionEntity) throws Exception {

        SslContextFactory sslContextFactory = new SslContextFactory();
        SslContextFactory sslContextFactoryPrev = outboundSslFactoryMap.putIfAbsent(webSocketConnectionEntity.getId(), sslContextFactory);
        if (sslContextFactoryPrev != null) {
            sslContextFactory = sslContextFactoryPrev;
        } else {
            final SSLContext sslContext = createSslContext(webSocketConnectionEntity, WebSocketConstants.ConnectionType.Outbound);
            sslContextFactory.setSslContext(sslContext);

            sslContextFactory.setExcludeProtocols();
            sslContextFactory.setExcludeCipherSuites();

            if (webSocketConnectionEntity.getOutboundCipherSuites() == null) {
                sslContextFactory.setIncludeCipherSuites(WebSocketLoadListener.getDefaultCipherSuiteNames());
            } else {
                sslContextFactory.setIncludeCipherSuites(webSocketConnectionEntity.getOutboundCipherSuites());
            }

            if (webSocketConnectionEntity.getOutboundTlsProtocols() == null) {
                sslContextFactory.setIncludeProtocols(WebSocketConstants.DEFAULT_TLS_PROTOCOL_LIST);
            } else {
                sslContextFactory.setIncludeProtocols(webSocketConnectionEntity.getOutboundTlsProtocols());
            }

            sslContextFactory.start();
        }

        return sslContextFactory;
    }

}
