package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExchangePatternEnum;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader.*;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.transport.tls.ClientTrustingTrustManager;
import com.l7tech.util.CachedCallable;
import com.l7tech.util.IOUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 01/12/11
 * Time: 1:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class SocketConnectorManager {
    public static class OutgoingMessageResponse {
        private String contentType;
        private long sessionId;
        private byte[] messageBytes;

        public OutgoingMessageResponse(String contentType, byte[] messageBytes, long sessionId) {
            this.contentType = contentType;
            this.messageBytes = messageBytes;
            this.sessionId = sessionId;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getMessageBytes() {
            return messageBytes;
        }

        public long getSessionId() {
            return sessionId;
        }
    }

    private static final Logger logger = Logger.getLogger(SocketConnectorManager.class.getName());

    private static SocketConnectorManager INSTANCE;
    private static final long MINUTE = 60000L;

    private EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager;
    private L7MinaRegistry registry;
    private ClusterPropertyManager clusterPropertyManager;
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private FirewallRulesManager firewallRulesManager;

    private Collection<ExtensibleSocketConnectorEntity> connectorEntities;
    private ExtensibleSocketConnectorClassLoader classLoader;
    private InitialDirContext directoryContext = null;

    private boolean stopped = false;
    private ReentrantReadWriteLock statusLock = new ReentrantReadWriteLock();
    private static final String EXTENSIBLE_SOCKET_CONNECTOR_PREFIX = "EXT-";

    public static synchronized void createConnectionManager(EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager,
                                                            ClusterPropertyManager clusterPropertyManager,
                                                            SsgKeyStoreManager keyStoreManager,
                                                            TrustManager trustManager,
                                                            SecureRandom secureRandom,
                                                            StashManagerFactory stashManagerFactory,
                                                            MessageProcessor messageProcessor,
                                                            DefaultKey defaultKey,
                                                            FirewallRulesManager firewallRulesManager)
            throws IllegalStateException {
        if (INSTANCE != null) {
            throw new IllegalStateException("ExtensibleSocketConnector Connection Manager is already initialized.");
        }

        INSTANCE = new SocketConnectorManager(entityManager, clusterPropertyManager, keyStoreManager, trustManager, secureRandom, stashManagerFactory, messageProcessor, defaultKey, firewallRulesManager);
    }

    public static synchronized SocketConnectorManager getInstance() {
        return INSTANCE;
    }

    private SocketConnectorManager(EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager,
                                   ClusterPropertyManager clusterPropertyManager,
                                   SsgKeyStoreManager keyStoreManager,
                                   TrustManager trustManager,
                                   SecureRandom secureRandom,
                                   StashManagerFactory stashManagerFactory,
                                   MessageProcessor messageProcessor,
                                   DefaultKey defaultKey,
                                   FirewallRulesManager firewallRulesManager) {
        INSTANCE = this;

        registry = new L7MinaRegistry();

        this.entityManager = entityManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessor = messageProcessor;
        this.firewallRulesManager = firewallRulesManager;
    }

    public void initializeConnectorClasses() {
        HashSet<Class> classesFromCurrentCL = new HashSet<>();
        classesFromCurrentCL.add(org.slf4j.Logger.class);
        classesFromCurrentCL.add(org.slf4j.LoggerFactory.class);

        classLoader = new ExtensibleSocketConnectorClassLoader(
                SocketConnectorManager.class.getClassLoader().getParent(),
                classesFromCurrentCL,
                new String[]{
                        "com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583",
                        "com.l7tech.external.assertions.extensiblesocketconnectorassertion.server"
                }
        );
        classLoader.initialize();
        classLoader.initializeWrapperClasses();
    }

    public void setDirectoryContext(InitialDirContext directoryContext) {
        this.directoryContext = directoryContext;
    }

    public void start() {
        logger.log(Level.INFO, "Starting connector contexts.");
        try {
            statusLock.readLock().lock();

            if (stopped) {
                return;
            }

            loadConnectors();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to start the socket connectors subsystem.", e);
        } finally {
            statusLock.readLock().unlock();
        }
    }

    public void stop() {
        logger.log(Level.INFO, "Stop connector contexts.");
        try {
            statusLock.writeLock().lock();
            stopped = true;

            unloadConnectors();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to stop the socket connectors subsystem.", e);
        } finally {
            statusLock.writeLock().unlock();
        }
    }

    private void loadConnectors() {
        try {
            connectorEntities = entityManager.findAll();

            for (ExtensibleSocketConnectorEntity connectorConfig : connectorEntities) {
                activateConnector(connectorConfig);
            }
        } catch (FindException e) {
            return;
        }
    }

    private void unloadConnectors() {
        if (connectorEntities != null) {
            //cycle through connector
            for (ExtensibleSocketConnectorEntity connectorConfig : connectorEntities) {
                destroySocketConnector(connectorConfig);
            }
        }
    }

    private void activateConnector(ExtensibleSocketConnectorEntity connectorConfig) {
        logger.log(Level.INFO, "Activating connector: " + connectorConfig.getGoid().toString());
        Object codec = MinaCodecFactory.createCodec(connectorConfig.getCodecConfiguration(), classLoader);

        NioSocketWrapper nioSocketWrapper = null;
        int port = 0;
        try {
            //before creating the connector determine what the port will be.
            if (connectorConfig.isUsePortValue()) {
                port = connectorConfig.getPort();
            } else if (connectorConfig.isUseDnsLookup()) {
                port = dnsLookupPort(connectorConfig.getDnsService(),
                        connectorConfig.getDnsDomainName(),
                        connectorConfig.getHostname(),
                        connectorConfig.isUseSsl());
            }

            //create an acceptor of connector depending on whether this is supposed to be an inbound
            //or outbound connector
            if (connectorConfig.isIn()) {
                nioSocketWrapper = NioSocketAcceptorWrapper.create();
                nioSocketWrapper.setHandler(InboundIoHandlerAdapterWrapper.create(stashManagerFactory, messageProcessor, connectorConfig.getServiceGoid()).getIoHandler());
            } else {
                nioSocketWrapper = NioSocketConnectorWrapper.create();
                nioSocketWrapper.setHandler(OutboundIoHandlerAdapterWrapper.create().getIoHandler());
                ((NioSocketConnectorWrapper) nioSocketWrapper).setHostName(connectorConfig.getHostname());
                ((NioSocketConnectorWrapper) nioSocketWrapper).setPort(port);
                ((NioSocketConnectorWrapper) nioSocketWrapper).setPortCacheTime(Calendar.getInstance().getTimeInMillis());
            }

            //add an ssl filter if ssl is required
            if (connectorConfig.isUseSsl()) {
                nioSocketWrapper.getFilterChain().addLast("sslFilter", createSSLFilter(connectorConfig));
            }

            //add our data protocal codec
            nioSocketWrapper.getFilterChain().addLast("codec", ProtocolCodecFilterWrapper.create(codec));

            //if the connector is inbound bind it, so it is ready for inbound messages.
            // Also, ensure the Firewall is open so that traffic can be sent in
            if (connectorConfig.isIn()) {
                ((NioSocketAcceptorWrapper) nioSocketWrapper).bind(new InetSocketAddress(port));
                firewallRulesManager.openPort(EXTENSIBLE_SOCKET_CONNECTOR_PREFIX + connectorConfig.getName(), port);
            }

        } catch (ExtensibleSocketConnectorClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, "Error starting connector: " + e);
            e.printStackTrace();
        } catch (ExtensibleSocketConnectorMinaClassException e) {
            logger.log(Level.WARNING, "Error starting connector: " + e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error starting connector: " + e);
            e.printStackTrace();
        }

        registry.addService(connectorConfig.getGoid(), nioSocketWrapper);
    }

    private void destroySocketConnector(ExtensibleSocketConnectorEntity config) {

        NioSocketWrapper nioSocketWrapper = registry.getService(config.getGoid());
        try {
            //ensure that the sessions for this connector are closed.
            closeAllSessionForConnection(nioSocketWrapper);

            //dispose of any resources used by this socket.
            nioSocketWrapper.dispose(true);

            // If listening to a port on the Gateway, close the firewall hole
            if(config.isIn()) {
                   firewallRulesManager.removeRule(EXTENSIBLE_SOCKET_CONNECTOR_PREFIX + config.getName());
            }

            registry.removeService(config.getGoid());
        } catch (ExtensibleSocketConnectorClassHelperNotInitializedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ExtensibleSocketConnectorMinaClassException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void connectionAdded(ExtensibleSocketConnectorEntity entity) {
        logger.log(Level.INFO, "Adding connector: " + entity.getGoid().toString());
        connectorEntities.add(entity);
        activateConnector(entity);
    }

    public void connectionUpdated(ExtensibleSocketConnectorEntity entity) {
        logger.log(Level.INFO, "Updating connector: " + entity.getGoid().toString());
        for (Iterator<ExtensibleSocketConnectorEntity> it = connectorEntities.iterator(); it.hasNext(); ) {
            ExtensibleSocketConnectorEntity oldConfig = it.next();
            if (oldConfig.getGoid().equals(entity.getGoid())) {
                destroySocketConnector(oldConfig);
                it.remove();
            }
        }
        connectorEntities.add(entity);
        activateConnector(entity);
    }

    public void connectionRemoved(Goid entityGoid) {
        logger.log(Level.INFO, "Removing connector: " + entityGoid.toString());
        for (Iterator<ExtensibleSocketConnectorEntity> it = connectorEntities.iterator(); it.hasNext(); ) {
            ExtensibleSocketConnectorEntity oldConfig = it.next();
            if (oldConfig.getGoid().equals(entityGoid)) {
                destroySocketConnector(oldConfig);
                it.remove();
            }
        }
    }

    private SslFilterWrapper createSSLFilter(ExtensibleSocketConnectorEntity connectorConfig) throws SSLException {

        SslFilterWrapper sslFilterWrapper = null;

        try {
            KeyManager[] keyManagers = null;
            if (connectorConfig.isIn() || (connectorConfig.isUseSsl() && connectorConfig.getSslKeyId() != null)) {
                String[] parts = connectorConfig.getSslKeyId().split(":");
                SsgKeyEntry keyEntry = keyStoreManager.lookupKeyByKeyAlias(parts[1], Goid.parseGoid(parts[0]));
                keyManagers = new KeyManager[]{new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())};
            } else {
                keyManagers = new KeyManager[0];
            }

            TrustManager tm = null;
            if (connectorConfig.isIn()) {
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
            sslContext.init(keyManagers, new TrustManager[]{tm}, secureRandom);

            sslFilterWrapper = SslFilterWrapper.create(sslContext);

            if (connectorConfig.isIn()) {
                sslFilterWrapper.setUseClientMode(false);

                switch (connectorConfig.getClientAuthEnum()) {
                    case DISABLED:
                        sslFilterWrapper.setNeedClientAuth(false);
                        sslFilterWrapper.setWantClientAuth(false);
                        break;
                    case OPTIONAL:
                        sslFilterWrapper.setNeedClientAuth(false);
                        sslFilterWrapper.setWantClientAuth(true);
                        break;
                    case REQUIRED:
                        sslFilterWrapper.setNeedClientAuth(true);
                        sslFilterWrapper.setWantClientAuth(true);
                        break;
                }
            } else {
                sslFilterWrapper.setUseClientMode(true);
            }

            return sslFilterWrapper;
        } catch (ExtensibleSocketConnectorMinaClassException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        } catch (UnrecoverableKeyException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        } catch (NoSuchAlgorithmException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        } catch (KeyStoreException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        } catch (FindException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        } catch (KeyManagementException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        } catch (ExtensibleSocketConnectorClassHelperNotInitializedException e) {
            throw new SSLException("Failed to create the SSL filter for an MLLP listener: " + e);
        }
    }

    /**
     * Sends the given message with the connector determined by given connector ID.
     * <p/>
     * Can return null if not OUT message is generated from the exchange - depending on the exchange pattern.
     */
    public OutgoingMessageResponse sendMessage(Message message, Goid connectorGoid, String sessionId, boolean failOnNoSession) throws Exception {
        IoSessionWrapper ioSessionWrapper = null;

        ExtensibleSocketConnectorEntity config = null;
        for (ExtensibleSocketConnectorEntity c : connectorEntities) {
            if (c.getGoid().equals(connectorGoid)) {
                config = c;
            }
        }

        if (config == null) {
            throw new Exception("Failed to find the outgoing socket connector (GOID = " + connectorGoid.toString() + ").");
        }

        //get the connector and handler
        NioSocketConnectorWrapper nioSocketConnectorWrapper = (NioSocketConnectorWrapper) registry.getService(connectorGoid);
        OutboundIoHandlerAdapterWrapper outboundIoHandlerAdapterWrapper = nioSocketConnectorWrapper.getHandler();

        //get a valid session
        ioSessionWrapper = getSessionFromConnection(nioSocketConnectorWrapper, config, sessionId, failOnNoSession);

        //send the message
        ioSessionWrapper.write(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()));

        //wait for response from server if we are expecting one
        if (config.getExchangePattern() == ExchangePatternEnum.OutIn) {
            boolean readSuccess = ioSessionWrapper.read().awaitUninterruptibly(config.getListenTimeout());

            if (!readSuccess) {
                logger.log(Level.WARNING, "Client connector timed out while waiting for server response.");
            }
        }

        //close session if we don't want to keep alive
        if (!config.isKeepAlive()) {
            ioSessionWrapper.close(true);
        }

        //return response if any...
        byte[] response = outboundIoHandlerAdapterWrapper.getResponse();
        if (response == null) {
            return new OutgoingMessageResponse(config.getContentType(), new byte[]{}, ioSessionWrapper.getId());
        } else {
            // we received a response return it.
            return new OutgoingMessageResponse(config.getContentType(), response, ioSessionWrapper.getId());
        }
    }

    /**
     * Receive all active sessions from connection
     *
     * @param connectionGoid the goid of the connection containing the active session
     * @return An List<String> of session ids
     * @throws Exception
     */
    public List<String> getSessionsFromConnection(Goid connectionGoid) throws Exception {
        List<String> keyList = new ArrayList<String>();

        NioSocketWrapper connection = registry.getService(connectionGoid);

        if (connection == null)
            throw new Exception("No connection found.");

        Set<Long> keySet = connection.getManagedSessions().keySet();

        //turn Set<Long> into List<String>
        for (Long key : keySet) {
            keyList.add(key.toString());
        }

        return keyList;
    }

    /**
     * Get a session from the specified connector.  If the session id (sessionIdString) is provided the function
     * tries to get a live session from the connector.  If no live session is found and  failOnNoSession is True then
     * the function will throw an Exception.
     * <p/>
     * The function will create a new session if the port timing has expired, or a session id was provided and
     * no session was found and failOnNoSession is False, or no session id was provided.
     *
     * @param socketConnectorWrapper
     * @param config
     * @param sessionIdString
     * @param failOnNoSession
     * @return A valid session (IoSessionWrapper) or null if no session was found
     * @throws Exception
     */
    private IoSessionWrapper getSessionFromConnection(NioSocketConnectorWrapper socketConnectorWrapper,
                                                      ExtensibleSocketConnectorEntity config,
                                                      String sessionIdString,
                                                      boolean failOnNoSession) throws Exception {

        if (config.isUseDnsLookup()) {
            if (portExpired(socketConnectorWrapper, config)) {
                return connect(socketConnectorWrapper, config);
            }
        }

        IoSessionWrapper session = getSessionFromConnectionBySessionId(socketConnectorWrapper, sessionIdString);
        if (session == null) {

            if (failOnNoSession)
                throw new Exception("No session found for session id [" + sessionIdString + "]");

            return connect(socketConnectorWrapper, config);
        } else {
            return session;
        }
    }

    /**
     * Get a live session from a connection using the session id
     *
     * @param nioSocketWrapper the socket we want to get the session from
     * @param sessionIdString  the id of the session to retrieve, will always be non-empty string
     * @return The current session (IoSessionWrapper) used by the socket.
     */
    private IoSessionWrapper getSessionFromConnectionBySessionId(NioSocketWrapper nioSocketWrapper, String sessionIdString) {

        if (sessionIdString != null && !sessionIdString.trim().isEmpty()) {
            try {
                if (nioSocketWrapper.getManagedSessionCount() > 0) {
                    long sessionId = Long.parseLong(sessionIdString);
                    return nioSocketWrapper.getManagedSessions().get(sessionId);
                }
            } catch (ExtensibleSocketConnectorClassHelperNotInitializedException e) {
                logger.log(Level.INFO, "Could not retrieve session with id [" + sessionIdString + "]: " + e);
            } catch (ExtensibleSocketConnectorMinaClassException e) {
                logger.log(Level.INFO, "Could not retrieve session with id [" + sessionIdString + "]: " + e);
            } catch (NumberFormatException e) {
                logger.log(Level.INFO, "Invalid session id [" + sessionIdString + "]: " + e);
            }
        }

        return null;
    }

    /**
     * Open a connection to a server
     *
     * @param connection The NioSocketConnectorWrapper with setup to create the desired connection.
     * @param config     Additional configuration
     * @return The newly created session (IoSessionWrapper)
     * @throws ExtensibleSocketConnectorClassHelperNotInitializedException
     * @throws ExtensibleSocketConnectorMinaClassException
     */
    private IoSessionWrapper connect(NioSocketConnectorWrapper connection,
                                     ExtensibleSocketConnectorEntity config) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        IoSessionWrapper ioSessionWrapper = null;

        ConnectFutureWrapper connectFutureWrapper = connection.connect();
        connectFutureWrapper.awaitUninterruptibly();
        ioSessionWrapper = connectFutureWrapper.getSession();

        if (config.getExchangePattern() == ExchangePatternEnum.OutIn) {
            ioSessionWrapper.getConfig().setUseReadOperation(true);
        } else if (config.getExchangePattern() == ExchangePatternEnum.OutOnly) {
            ioSessionWrapper.suspendRead();
        }

        return ioSessionWrapper;
    }

    /**
     * Close all the session for a given connection
     *
     * @param socketWrapper an NioSocketWrapper which wraps our connection object
     * @throws ExtensibleSocketConnectorClassHelperNotInitializedException
     * @throws ExtensibleSocketConnectorMinaClassException
     */
    private void closeAllSessionForConnection(NioSocketWrapper socketWrapper) throws ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        if (socketWrapper.getManagedSessionCount() > 0) {

            Map<Long, IoSessionWrapper> managedSessions = socketWrapper.getManagedSessions();
            Iterator<Long> keySet = managedSessions.keySet().iterator();
            while (keySet.hasNext()) {
                managedSessions.get(keySet.next()).close(true);
            }
        }
    }

    /**
     * @param service
     * @param domainName
     * @param hostName
     * @param useSSL
     * @return
     * @throws Exception
     */
    private int dnsLookupPort(String service, String domainName, String hostName, boolean useSSL) throws Exception {
        if (useSSL) {
            try {
                return dnsLookupPort(service, "tls", domainName, hostName, directoryContext);
            } catch (Exception e) {
                //log message for failed tls lookup...
                //If tls lookup fails we want to try the tcp lookup afterwards,
                //that is why this exception isn't thrown out of the method.
                logger.log(Level.WARNING, e.getMessage());
            }
        }

        return dnsLookupPort(service, "tcp", domainName, hostName, directoryContext);
    }

    private int dnsLookupPort(String service, String protocol, String domainName, String hostName, InitialDirContext dirContext) throws Exception {

        String attributeName = "dns:/_" + service + "._" + protocol + "." + domainName;

        Attribute attribute = null;
        try {
            attribute = getAttribute(attributeName, dirContext);
        } catch (NamingException e) {
            throw new Exception("No record found for service,'" + service +
                    "', with protocol,'" + protocol +
                    "', and domainName,'" + domainName +
                    "', in DNS");
        }

        List<String[]> valuesWithHostname = getValuesByHostname(attribute, hostName);
        if (valuesWithHostname.size() < 1)
            throw new Exception("No entries for hostname,'" + hostName +
                    "', found for service, '" + service +
                    "', with protocol ,'" + protocol +
                    "', and domainName,'" + domainName +
                    "', in DNS.");

        sortValues(valuesWithHostname);
        return getPort(valuesWithHostname);
    }

    private Attribute getAttribute(String attributeName, InitialDirContext dirContext) throws NamingException {
        Attributes attributes = dirContext.getAttributes(attributeName, new String[]{"SRV"});
        return attributes.get("SRV");
    }

    private List<String[]> getValuesByHostname(Attribute attribute, String hostName) throws NamingException {

        List<String[]> requiredValueList = new ArrayList<String[]>();

        String[] valueInfo = null;
        for (int i = 0; i < attribute.size(); i++) {
            valueInfo = ((String) attribute.get(i)).split("\\s+");

            if (hostName.equals(valueInfo[3].substring(0, valueInfo[3].length() - 1))) {
                requiredValueList.add(valueInfo);
            }
        }

        return requiredValueList;
    }

    private void sortValues(List<String[]> requiredValues) {
        Collections.sort(requiredValues, new Comparator<String[]>() {

            @Override
            public int compare(String[] o1, String[] o2) {
                return Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]);
            }

        });
    }

    private int getPort(List<String[]> requiredValues) {
        return Integer.parseInt(requiredValues.get(0)[2]);
    }

    /**
     * Determine if the port specified for a connector has expired
     * A port is considered expired if 1 minute has elapsed since the last time the port expiry was checked and the port
     * has changed.
     *
     * @param nioSocketConnectorWrapper
     * @param config
     * @return True if the above condition is met otherwise False
     * @throws Exception
     */
    private boolean portExpired(NioSocketConnectorWrapper nioSocketConnectorWrapper, ExtensibleSocketConnectorEntity config) throws Exception {

        boolean expired = false;
        int port = 0;
        long currentTime = Calendar.getInstance().getTimeInMillis();

        if ((currentTime - nioSocketConnectorWrapper.getPortCacheTime()) >= MINUTE) {

            port = dnsLookupPort(config.getDnsService(),
                    config.getDnsDomainName(),
                    config.getHostname(),
                    config.isUseSsl());

            if (nioSocketConnectorWrapper.getPort() != port) {
                nioSocketConnectorWrapper.setPort(port);
                expired = true;
            }

            nioSocketConnectorWrapper.setPortCacheTime(currentTime);
        }

        return expired;
    }
}
