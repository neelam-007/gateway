package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.XMPPStartTLSAssertion;
import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPMinaClassException;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.transport.tls.ClientTrustingTrustManager;
import com.l7tech.util.CachedCallable;
import com.l7tech.util.GoidUpgradeMapper;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Until we get a decent ClassLoader for modular assertions, this class must use a lot
 * of reflection to get around using the wrong class loader.
 * This modular assertion wants a newer version of Apache MINA than is included in
 * the released product.
 */
public class XMPPConnectionManager {
    private static class FirewallPortUpdateTask extends TimerTask {
        Integer oldPort;
        Integer newPort;
        String name;
        FirewallRulesManager firewallRulesManager;

        @Override
        public void run() {
            if(oldPort != null) {
                firewallRulesManager.removeRule(name);
            }

            if(newPort != null) {
                firewallRulesManager.openPort(name, newPort);
            }
        }
    }

    private static final Logger logger = Logger.getLogger(XMPPConnectionManager.class.getName());

    private static final String EXCEPTION_LOGGING_SYSPROP = "com.l7tech.xmppassertion.logging.exceptions";
    private static final String FIREWALL_RULE_NAME_PREFIX = "XMPP-";

    private static XMPPConnectionManager INSTANCE;

    private EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager;
    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private DefaultKey defaultKey;
    private FirewallRulesManager firewallRulesManager;

    private List<XMPPConnectionEntity> connectionEntities = new ArrayList<XMPPConnectionEntity>();
    private HashSet<Goid> pendingInboundConnections = new HashSet<Goid>();
    private HashMap<Goid, Object> inboundAcceptors = new HashMap<Goid, Object>();
    private HashMap<Long, Object> inboundSessions = new HashMap<Long, Object>();
    private HashMap<Long, Object> outboundSessions = new HashMap<Long, Object>();

    private AssociatedSessionManager associatedSessionManager = new AssociatedSessionManager();

    private boolean stopped = false;
    private ReentrantReadWriteLock statusLock = new ReentrantReadWriteLock();

    private Timer timer = new Timer();

    private XMPPClassHelper classHelper;

    public static synchronized void createConnectionManager(EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager,
                                                            StashManagerFactory stashManagerFactory,
                                                            MessageProcessor messageProcessor,
                                                            SsgKeyStoreManager keyStoreManager,
                                                            TrustManager trustManager,
                                                            SecureRandom secureRandom,
                                                            DefaultKey defaultKey,
                                                            FirewallRulesManager firewallRulesManager)
            throws IllegalStateException {
        if(INSTANCE != null) {
            throw new IllegalStateException("XMPP Connection Manager is already initialized.");
        }

        INSTANCE = new XMPPConnectionManager(entityManager, stashManagerFactory, messageProcessor, keyStoreManager, trustManager, secureRandom, defaultKey, firewallRulesManager);
    }

    public static synchronized XMPPConnectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * This allows the singleton instance to be destroyed in unit tests, and as such is default access.
     * It is declared final so it is not accidentally brought into subclasses.
     * Added by: rseminoff  29 May 2012
     */
    static final synchronized void destroyInstance() {
        INSTANCE = null;
    }

    private XMPPConnectionManager(EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager,
                                  StashManagerFactory stashManagerFactory,
                                  MessageProcessor messageProcessor,
                                  SsgKeyStoreManager keyStoreManager,
                                  TrustManager trustManager,
                                  SecureRandom secureRandom,
                                  DefaultKey defaultKey,
                                  FirewallRulesManager firewallRulesManager) {
        this.entityManager = entityManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessor = messageProcessor;
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.defaultKey = defaultKey;
        this.firewallRulesManager = firewallRulesManager;

        classHelper = XMPPClassHelperFactory.getIntance().createClassHelper();
    }

    public void start() {
        try {
            statusLock.readLock().lock();

            if(stopped) {
                return;
            }

            try {
                connectionEntities.addAll(entityManager.findAll());
                for(XMPPConnectionEntity entity : connectionEntities) {
                    if(entity.isInbound() && entity.isEnabled()) {
                        startInboundConnection(entity);
                    }
                }
            } catch(FindException e) {
                logger.log(Level.WARNING, "Failed to start the XMPP connections.", e);
                return;
            }
        } finally {
            statusLock.readLock().unlock();
        }
    }

    public void stop() {
        try {
            statusLock.writeLock().lock();
            stopped = true;

            try {
                for(Object session : outboundSessions.values()) {
                    try {
                        classHelper.closeSession(session);
                    } catch(XMPPMinaClassException e) {
                        logException(e);
                    }
                }

                for(Iterator<XMPPConnectionEntity> it = connectionEntities.iterator();it.hasNext();) {
                    XMPPConnectionEntity entity = it.next();
                    if(inboundAcceptors.containsKey(entity.getGoid())) {
                        stopInboundConnection(entity);
                    }
                    it.remove();
                }
            } catch(XMPPClassHelperNotInitializedException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        } finally {
            statusLock.writeLock().unlock();
        }
    }

    public void connectionAdded(XMPPConnectionEntity entity) {
        connectionEntities.add(entity);
        if(entity.isInbound() && entity.isEnabled()) {
            startInboundConnection(entity);

            FirewallPortUpdateTask task = new FirewallPortUpdateTask();
            task.newPort = entity.getPort();
            task.name = FIREWALL_RULE_NAME_PREFIX + entity.getGoid().toString();
            task.firewallRulesManager = firewallRulesManager;
            task.run();
        }
    }

    public void connectionUpdated(XMPPConnectionEntity entity) {
        for(int i = 0;i < connectionEntities.size();i++) {
            XMPPConnectionEntity e = connectionEntities.get(i);

            if(e.getGoid().equals(entity.getGoid())) {
                connectionEntities.set(i, entity);

                if(inboundAcceptors.containsKey(entity.getGoid()) && !entity.isEnabled()) {
                    stopInboundConnection(entity);

                    FirewallPortUpdateTask task = new FirewallPortUpdateTask();
                    task.oldPort = entity.getPort();
                    task.name = FIREWALL_RULE_NAME_PREFIX + entity.getGoid().toString();
                    task.firewallRulesManager = firewallRulesManager;
                    timer.schedule(task, 10);
                } else if(!inboundAcceptors.containsKey(entity.getGoid()) && entity.isEnabled()) {
                    startInboundConnection(entity);

                    FirewallPortUpdateTask task = new FirewallPortUpdateTask();
                    task.newPort = entity.getPort();
                    task.name = FIREWALL_RULE_NAME_PREFIX + entity.getGoid().toString();
                    task.firewallRulesManager = firewallRulesManager;
                    timer.schedule(task, 10);
                } else if(inboundAcceptors.containsKey(entity.getGoid()) && entity.isEnabled() && XMPPConnectionEntity.restartNeeded(e, entity)) {
                    stopInboundConnection(entity);
                    startInboundConnection(entity);

                    FirewallPortUpdateTask task = new FirewallPortUpdateTask();
                    task.oldPort = e.getPort();
                    task.newPort = entity.getPort();
                    task.name = FIREWALL_RULE_NAME_PREFIX + entity.getGoid().toString();
                    task.firewallRulesManager = firewallRulesManager;
                    timer.schedule(task, 10);
                }

                break;
            }
        }
    }

    public void connectionRemoved(XMPPConnectionEntity entity) {
        if(inboundAcceptors.containsKey(entity.getGoid())) {
            stopInboundConnection(entity);

            FirewallPortUpdateTask task = new FirewallPortUpdateTask();
            task.oldPort = entity.getPort();
            task.name = FIREWALL_RULE_NAME_PREFIX + entity.getGoid().toString();
            task.firewallRulesManager = firewallRulesManager;
            task.run();
        }

        XMPPConnectionEntity entityToRemove = null;
        for(XMPPConnectionEntity e : connectionEntities) {
            if(e.getGoid().equals(entity.getGoid())) {
                entityToRemove = e;
                break;
            }
        }

        if(entityToRemove != null) {
            connectionEntities.remove(entityToRemove);
        }
    }

    private void startInboundConnection(XMPPConnectionEntity entity) {
        try {
            statusLock.readLock().lock();
            if(stopped) {
                return;
            }

            synchronized(pendingInboundConnections) {
                if(pendingInboundConnections.contains(entity.getGoid()) || inboundAcceptors.containsKey(entity.getGoid())) {
                    return;
                }

                pendingInboundConnections.add(entity.getGoid());
            }

            try {
                Object acceptor = classHelper.createNioSocketAcceptor();
                if (entity.isLegacySsl()) {
                    addLegacySSLFilterToChainBuilder(acceptor, entity);
                }
                classHelper.addLastToFilterChainBuilder(
                        classHelper.nioSocketAcceptorGetFilterChain(acceptor),
                        "codec",
                        classHelper.createProtocolCodecFilter(classHelper.createXmlStreamCodecFactory(classHelper.createXmppCodecConfiguration(), entity.isInbound()))
                );
                classHelper.addLastToFilterChainBuilder(
                        classHelper.nioSocketAcceptorGetFilterChain(acceptor),
                        "executor",
                        classHelper.createExecutorFilter(entity.getThreadpoolSize())
                );

                SessionStartedCallback sessionStartedCallback = new SessionStartedCallback() {
                    @Override
                    public void sessionStarted(Object session) {
                        try {
                            inboundSessions.put(classHelper.getSessionId(session), session);
                        } catch(XMPPClassHelperNotInitializedException e) {
                            // Should not happen
                        } catch(XMPPMinaClassException e) {
                            logException(e);
                        }
                    }

                    @Override
                    public long getSessionId() {
                        return -1;
                    }

                    @Override
                    public void waitForSessionId() {
                    }
                };
                SessionTerminatedCallback sessionTerminatedCallback = new SessionTerminatedCallback() {
                    @Override
                    public void sessionTerminated(Object session) {
                        try {
                            Long sessionId = classHelper.getSessionId(session);
                            inboundSessions.remove(classHelper.getSessionId(session));
                            associatedSessionManager.removeClientSession(classHelper.getSessionId(session));
                        } catch(XMPPClassHelperNotInitializedException e) {
                            // Should not happen
                        } catch(XMPPMinaClassException e) {
                            logException(e);
                        }
                    }
                };

                InboundMessageHandler messageHandler = new InboundMessageHandler(
                        entity,
                        stashManagerFactory,
                        messageProcessor,
                        sessionStartedCallback,
                        sessionTerminatedCallback,
                        classHelper
                );
                classHelper.ioServiceSetHandler(acceptor, messageHandler.getIoHandler());
                Object sessionConfig = classHelper.ioServiceGetSessionConfig(acceptor);
                classHelper.ioSessionConfigSetReadBufferSize(sessionConfig, 2048);
                classHelper.ioSessionConfigSetIdleTime(sessionConfig, classHelper.getIdleStatus_BOTH_IDLE(), 120);
                classHelper.ioAcceptorBind(acceptor, new InetSocketAddress(entity.getBindAddress(), entity.getPort()));

                synchronized(pendingInboundConnections) {
                    pendingInboundConnections.remove(entity.getGoid());
                    inboundAcceptors.put(entity.getGoid(), acceptor);
                }
            } catch(XMPPClassHelperNotInitializedException e) {
                logger.log(Level.WARNING, e.getMessage());
            } catch(XMPPMinaClassException e) {
                logException(e);
            } catch(Exception e) {
                logger.log(Level.WARNING, "Failed to start the inbound XMPP connection (" + entity.getName() + ").", e);
            } finally {
                synchronized(pendingInboundConnections) {
                    pendingInboundConnections.remove(entity.getGoid());
                }
            }
        } finally {
            statusLock.readLock().unlock();
        }
    }

    private void stopInboundConnection(XMPPConnectionEntity entity) {
        Object acceptor = null;
        synchronized(pendingInboundConnections) {
            acceptor = inboundAcceptors.remove(entity.getGoid());
        }

        if(acceptor == null) {
            return;
        }

        try {
            for(Object session : inboundSessions.values()) {
                try {
                    if(classHelper.getService(session) == acceptor) {
                        classHelper.closeSession(session);
                    }
                } catch(XMPPMinaClassException e) {
                    logException(e);
                }
            }

            try {
                classHelper.ioAcceptorDispose(acceptor, false);
            } catch(XMPPMinaClassException e) {
                logException(e);
            }
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public long connectToServer(Goid xmppClientConnectionId)throws ConnectionConfigNotFoundException {
        SessionStartedCallback sessionStartedCallback = new SessionStartedCallback() {
            private long sessionId = -1;

            @Override
            public void sessionStarted(Object session) {
                try {
                    long sId = classHelper.getSessionId(session);
                    outboundSessions.put(sId, session);

                    synchronized(this) {
                        sessionId = sId;
                        notifyAll();
                    }
                } catch(XMPPClassHelperNotInitializedException e) {
                    // Should not happen
                } catch(XMPPMinaClassException e) {
                    logException(e);
                }
            }

            @Override
            public long getSessionId() {
                return sessionId;
            }

            @Override
            public void waitForSessionId() {
                synchronized(this) {
                    if(sessionId == -1) {
                        try {
                            wait(30000);
                        } catch(InterruptedException e) {
                            // Ignore
                        }
                    }
                }
            }
        };

        SessionTerminatedCallback sessionTerminatedCallback = new SessionTerminatedCallback() {
            @Override
            public void sessionTerminated(Object session) {
                try {
                    Long sessionId = classHelper.getSessionId(session);

                    outboundSessions.remove(sessionId);
                    associatedSessionManager.removeServerSession(sessionId);
                } catch(XMPPClassHelperNotInitializedException e) {
                    // Should not happen
                } catch(XMPPMinaClassException e) {
                    logException(e);
                }
            }
        };

        createOutboundSession(xmppClientConnectionId, sessionStartedCallback, sessionTerminatedCallback);

        sessionStartedCallback.waitForSessionId();
        return sessionStartedCallback.getSessionId();
    }

    public long sendMessageToServer(final Message requestMessage, long sessionId)
            throws ConnectionConfigNotFoundException, OutboundSessionNotFoundException,
            IOException, NoSuchPartException {
        if(!outboundSessions.containsKey(sessionId)) {
            throw new OutboundSessionNotFoundException(sessionId);
        }

        StashManager stashManager = stashManagerFactory.createStashManager();
        stashManager.stash(0, requestMessage.getMimeKnob().getEntireMessageBodyAsInputStream());

        try {
            classHelper.ioSessionWrite(outboundSessions.get(sessionId), stashManager.recallBytes(0));

            return sessionId;
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage());
            throw new OutboundSessionNotFoundException(sessionId);
        } catch(XMPPMinaClassException e) {
            logException(e);
            throw new OutboundSessionNotFoundException(sessionId);
        }
    }

    /**
     * Creates a normal connection or an SSL connection (if legacy SSL is checked).
     */
    private void createOutboundSession(Goid xmppClientConnectionId,
                                       SessionStartedCallback sessionStartedCallback,
                                       SessionTerminatedCallback sessionTerminatedCallback)
            throws ConnectionConfigNotFoundException {
        try {
            statusLock.readLock().lock();

            XMPPConnectionEntity config = null;
            for(XMPPConnectionEntity entity : connectionEntities) {
                if(entity.getGoid().equals(xmppClientConnectionId)) {
                    config = entity;
                    break;
                }
            }
            if(config == null) {
                throw new ConnectionConfigNotFoundException(xmppClientConnectionId);
            }

            Object connector = classHelper.createNioSocketConnector();
            Object sessionConfig = classHelper.ioServiceGetSessionConfig(connector);
            classHelper.ioSessionConfigSetReadBufferSize(sessionConfig, 2048);
            classHelper.ioSessionConfigSetIdleTime(sessionConfig, classHelper.getIdleStatus_BOTH_IDLE(), 120);
            OutboundMessageHandler messageHandler = new OutboundMessageHandler(config, stashManagerFactory, messageProcessor, sessionStartedCallback, sessionTerminatedCallback, classHelper);
            classHelper.ioServiceSetHandler(connector, messageHandler.getIoHandler());

            if (config.isLegacySsl()) {
                SSLContext sslContext = createSSLContext(config);
                Object sslFilter = classHelper.createSslFilter(sslContext);
                classHelper.sslFilterSetUseClientMode(sslFilter, true);
                classHelper.addLastToFilterChainBuilder(
                        classHelper.nioSocketConnectorGetFilterChain(connector),
                        "sslFilter", sslFilter
                );
            }

            classHelper.addLastToFilterChainBuilder(
                    classHelper.nioSocketConnectorGetFilterChain(connector),
                    "codec", classHelper.createProtocolCodecFilter(
                        classHelper.createXmlStreamCodecFactory(classHelper.createXmppCodecConfiguration(), false))
            );

            Object future = classHelper.nioSocketConnectorConnect(connector, new InetSocketAddress(config.getHostname(), config.getPort()));
            classHelper.connectFutureAwaitUninterruptibly(future);
        } catch (NoSuchAlgorithmException e) {
            logException(e);
        } catch(XMPPClassHelperNotInitializedException e) {
            logException(e);
        } catch (IOException e) {
            logException(e);
        } catch (UnrecoverableKeyException e) {
            logException(e);
        } catch (KeyManagementException e) {
            logException(e);
        } catch(XMPPMinaClassException e) {
            logException(e);
        } finally {
            statusLock.readLock().unlock();
        }
    }

    public void sendMessageToClient(final Message requestMessage, Long sessionId)
            throws InboundSessionNotFoundException, IOException, NoSuchPartException {
        if(!inboundSessions.containsKey(sessionId)) {
            throw new InboundSessionNotFoundException(sessionId);
        }

        StashManager stashManager = stashManagerFactory.createStashManager();
        stashManager.stash(0, requestMessage.getMimeKnob().getEntireMessageBodyAsInputStream());

        try {
            classHelper.ioSessionWrite(inboundSessions.get(sessionId), stashManager.recallBytes(0));
        } catch(XMPPClassHelperNotInitializedException e) {
            throw new IOException(e);
        } catch(XMPPMinaClassException e) {
            logException(e);
            throw new IOException(e);
        }
    }

    public void closeServerConnection(long sessionId) throws OutboundSessionNotFoundException {
        if(!outboundSessions.containsKey(sessionId)) {
            throw new OutboundSessionNotFoundException(sessionId);
        }

        Object session = outboundSessions.remove(sessionId);

        try {
            classHelper.closeSession(session);
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new OutboundSessionNotFoundException(sessionId);
        } catch(XMPPMinaClassException e) {
            logException(e);
            throw new OutboundSessionNotFoundException(sessionId);
        }
    }

    public void closeClientConnection(long sessionId) throws InboundSessionNotFoundException {
        if(!inboundSessions.containsKey(sessionId)) {
            throw new InboundSessionNotFoundException(sessionId);
        }

        Object session = inboundSessions.remove(sessionId);

        try {
            classHelper.closeSession(session);
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new InboundSessionNotFoundException(sessionId);
        } catch(XMPPMinaClassException e) {
            logException(e);
            throw new InboundSessionNotFoundException(sessionId);
        }
    }

    /**
     * Adds TLS filter and forwards the STARTLTS message.
     */
    public void startTLSForSession(long sessionId, XMPPStartTLSAssertion tlsAssertion, Message message)
            throws SSLException, OutboundSessionNotFoundException, InboundSessionNotFoundException {
        Object session = null;
        if(tlsAssertion.isToServer()) {
            session = outboundSessions.get(sessionId);
            if(session == null) {
                throw new OutboundSessionNotFoundException(sessionId);
            }
        } else {
            session = inboundSessions.get(sessionId);
            if(session == null) {
                throw new InboundSessionNotFoundException(sessionId);
            }
        }

        try {
            KeyManager[] keyManagers = null;
            if(!tlsAssertion.isToServer() || tlsAssertion.isToServer() && tlsAssertion.isProvideClientCert()) {
                String[] parts = tlsAssertion.getPrivateKeyId().split(":");

                SsgKeyEntry keyEntry;
                if("-1".equals(parts[0])) {
                    keyEntry = defaultKey.getSslInfo();
                } else {
                    Goid keystoreGOID = null;
                    long keystoreOID = Long.MIN_VALUE;

                    try {
                        keystoreGOID = Goid.parseGoid(parts[0]);
                    } catch (IllegalArgumentException iae) {
                        try {
                            keystoreOID = Long.parseLong(parts[0]);
                        } catch (NumberFormatException nfe) {
                            throw new SSLException("Unable to parse the reference to the default key.");
                        }
                    }

                    if (keystoreGOID == null) {
                        keystoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keystoreOID);
                    }

                    keyEntry = keyStoreManager.lookupKeyByKeyAlias(parts[1], keystoreGOID);
                }
                keyManagers = new KeyManager[] {new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())};
            } else {
                keyManagers = new KeyManager[0];
            }

            TrustManager tm = null;
            if(!tlsAssertion.isToServer()) {
                tm = new ClientTrustingTrustManager(new CachedCallable<X509Certificate[]>(30000L, new Callable<X509Certificate[]>() {
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
            JceProvider.getInstance().prepareSslContext( sslContext );
            sslContext.init(keyManagers, new TrustManager[] {tm}, secureRandom);

            Object sslFilter = classHelper.createSslFilter(sslContext);
            //sslFilter.setEnabledProtocols(new String[]{sslContext.getProtocol()});
            classHelper.sslFilterSetEnabledCipherSuites(sslFilter, sslContext.getSupportedSSLParameters().getCipherSuites());

            if(!tlsAssertion.isToServer()) {
                classHelper.sslFilterSetUseClientMode(sslFilter, false);

                switch(tlsAssertion.getClientAuthType()) {
                    case NONE:
                        classHelper.sslFilterSetNeedClientAuth(sslFilter, false);
                        classHelper.sslFilterSetWantClientAuth(sslFilter, false);
                        break;
                    case OPTIONAL:
                        classHelper.sslFilterSetNeedClientAuth(sslFilter, false);
                        classHelper.sslFilterSetWantClientAuth(sslFilter, true);
                        break;
                    case REQUIRED:
                        classHelper.sslFilterSetNeedClientAuth(sslFilter, true);
                        classHelper.sslFilterSetWantClientAuth(sslFilter, true);
                        break;
                }
            } else {
                classHelper.sslFilterSetUseClientMode(sslFilter, true);
            }

            StashManager stashManager = stashManagerFactory.createStashManager();
            stashManager.stash(0, message.getMimeKnob().getEntireMessageBodyAsInputStream());
            byte[] messageBytes = stashManager.recallBytes(0);

            classHelper.addFirstToFilterChain(classHelper.ioSessionGetFilterChain(session), "sslFilter", sslFilter);
            if(!tlsAssertion.isToServer() && messageBytes.length > 0) {
                classHelper.ioSessionSetAttribute(session, classHelper.getSslFilter_DISABLE_ENCRYPTION_ONCE(), Boolean.TRUE);
                classHelper.ioSessionWrite(session, messageBytes);
            }
        } catch(KeyStoreException e) {
            throw new SSLException("Failed to create the TLS filter for a XMPP connection. " + e.getMessage());
        } catch(NoSuchAlgorithmException e) {
            throw new SSLException("Failed to create the TLS filter for a XMPP connection. " + e.getMessage());
        } catch(Exception e) {
            throw new SSLException("Failed to create the TLS filter for a XMPP connection. " + e.getMessage());
        }
    }

    /**
     * Adds a filter to the chain builder to support legacy SSL connections.
     */
    public void addLegacySSLFilterToChainBuilder(Object acceptor, XMPPConnectionEntity entity)
            throws SSLException, OutboundSessionNotFoundException, InboundSessionNotFoundException {
        try {
            SSLContext sslContext = createSSLContext(entity);
            Object sslFilter = classHelper.createSslFilter(sslContext);
            classHelper.sslFilterSetEnabledCipherSuites(sslFilter, sslContext.getSupportedSSLParameters().getCipherSuites());

            if(entity.isInbound()) {
                classHelper.sslFilterSetUseClientMode(sslFilter, false);
            } else {
                classHelper.sslFilterSetUseClientMode(sslFilter, true);
            }

            classHelper.addLastToFilterChainBuilder(
                    classHelper.nioSocketConnectorGetFilterChain(acceptor),
                    "sslFilter", sslFilter
            );
            logger.log(Level.FINE, "Added legacy SSL filter");
        } catch(NoSuchAlgorithmException e) {
            throw new SSLException("Failed to create the legacy SSL filter. " + e.getMessage());
        } catch(Exception e) {
            throw new SSLException("Failed to create the legacy SSL filter. " + e.getMessage());
        }
    }

    private SSLContext createSSLContext(XMPPConnectionEntity entity)
            throws NoSuchAlgorithmException, IOException, UnrecoverableKeyException, KeyManagementException {
        SsgKeyEntry keyEntry = defaultKey.getSslInfo();
        KeyManager[] keyManagers = null;
        TrustManager tm = null;
        if(entity.isInbound()) {
            keyManagers = new KeyManager[] {new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())};
            tm = new ClientTrustingTrustManager(new CachedCallable<X509Certificate[]>(30000L, new Callable<X509Certificate[]>() {
                @Override
                public X509Certificate[] call() throws Exception {
                    return new X509Certificate[0];
                }
            }));
        } else {
            keyManagers = new KeyManager[0];
            tm = trustManager;
        }

        Provider provider = JceProvider.getInstance().getProviderFor("SSLContext.TLSv1");
        SSLContext sslContext = SSLContext.getInstance("TLSv1", provider);
        JceProvider.getInstance().prepareSslContext( sslContext );
        sslContext.init(keyManagers, new TrustManager[] {tm}, secureRandom);
        return sslContext;
    }

    public X509Certificate getRemoteCertificate(long sessionId, boolean inbound)
            throws InboundSessionNotFoundException, OutboundSessionNotFoundException, TLSNotStartedException {
        Object session = null;
        if(inbound) {
            session = inboundSessions.get(sessionId);
            if(session == null) {
                throw new InboundSessionNotFoundException(sessionId);
            }
        } else {
            session = outboundSessions.get(sessionId);
            if(session == null) {
                throw new OutboundSessionNotFoundException(sessionId);
            }
        }

        try {
            Object filterChain = classHelper.ioSessionGetFilterChain(session);
            Object sslFilter = classHelper.filterChainGet(filterChain, "sslFilter");
            if(sslFilter == null) {
                throw new TLSNotStartedException(sessionId);
            }

            SSLSession sslSession = classHelper.sslFilterGetSslSession(sslFilter, session);
            if(sslSession == null) {
                throw new TLSNotStartedException(sessionId);
            }

            try {
                return (X509Certificate)sslSession.getPeerCertificates()[0];
            } catch(SSLPeerUnverifiedException e) {
                return null;
            }
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return null;
        } catch(XMPPMinaClassException e) {
            logException(e);
            return null;
        }
    }

    public void associateSessions(long clientSessionId, long serverSessionId) {
        associatedSessionManager.associateSessions(clientSessionId, serverSessionId);
    }

    public Long getAssociatedClientSessionId(long serverSessionId) {
        return associatedSessionManager.getClientSessionFromServerSession(serverSessionId);
    }

    public Long getAssociatedServerSessionId(long clientSessionId) {
        return associatedSessionManager.getServerSessionFromClientSession(clientSessionId);
    }

    public String getSessionAttribute(long sessionId, boolean inbound, Object key)
            throws InboundSessionNotFoundException, OutboundSessionNotFoundException {
        Object session = null;
        if(inbound) {
            session = inboundSessions.get(sessionId);
            if(session == null) {
                throw new InboundSessionNotFoundException(sessionId);
            }
        } else {
            session = outboundSessions.get(sessionId);
            if(session == null) {
                throw new OutboundSessionNotFoundException(sessionId);
            }
        }

        try {
            return classHelper.ioSessionGetAttribute(session, key);
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return null;
        } catch(XMPPMinaClassException e) {
            logException(e);
            return null;
        }
    }

    public void setSessionAttribute(long sessionId, boolean inbound, Object key, Object value)
            throws InboundSessionNotFoundException, OutboundSessionNotFoundException {
        Object session = null;
        if(inbound) {
            session = inboundSessions.get(sessionId);
            if(session == null) {
                throw new InboundSessionNotFoundException(sessionId);
            }
        } else {
            session = outboundSessions.get(sessionId);
            if(session == null) {
                throw new OutboundSessionNotFoundException(sessionId);
            }
        }

        try {
            classHelper.ioSessionSetAttribute(session, key, value);
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } catch(XMPPMinaClassException e) {
            logException(e);
        }
    }

    private void logException(Exception e) {
        if("true".equals(System.getProperty(EXCEPTION_LOGGING_SYSPROP))) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
        } else {
            logger.log(Level.WARNING, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "");
        }
    }
}
