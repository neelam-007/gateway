package com.l7tech.server.transport.ftp;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;
import org.apache.ftpserver.*;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.ftplet.*;
import org.springframework.context.ApplicationEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgConnector.SCHEME_FTP;
import static com.l7tech.gateway.common.transport.SsgConnector.SCHEME_FTPS;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_FTP_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.Pair.pair;

/**
 * Creates and controls an embedded FTP server for each configured SsgConnector
 * with an FTP or FTPS scheme.
 */
public class FtpServerManager extends TransportModule {
    private static final String DEFAULT_FTPLET_NAME = "default";

    private final Properties COMMON_PROPERTIES = loadProperties(COMMON_PROPS);
    private final Properties DEFAULT_PROPERTIES = loadProperties(DEFAULT_PROPS);
    private final Properties FTPS_PROPERTIES = loadProperties(SSL_PROPS);

    //- PUBLIC

    public FtpServerManager(final Config config,
                            final GatewayState gatewayState,
                            final MessageProcessor messageProcessor,
                            final SoapFaultManager soapFaultManager,
                            final StashManagerFactory stashManagerFactory,
                            final LicenseManager licenseManager,
                            final DefaultKey defaultKeystore,
                            final TrustedCertServices trustedCertServices,
                            final SsgConnectorManager ssgConnectorManager,
                            final EventChannel messageProcessingEventChannel,
                            final ClusterPropertyManager clusterPropertyManager,
                            final ServiceManager serviceManager) {
        super("FTP Server Manager", Component.GW_FTPSERVER, logger, SERVICE_FTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKeystore, config);
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.clusterPropertyManager = clusterPropertyManager;
        this.serviceManager = serviceManager;
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if (TransportModule.isEventIgnorable(applicationEvent)) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if (!isStarted())
            return;

        if (applicationEvent instanceof ReadyForMessages && ftpServers.isEmpty()) {
            try {
                startInitialConnectors();
            } catch (LifecycleException e) {
                auditError("FTP(S)", "Error during startup.", e);
            }
        }
    }

    //- PROTECTED

    @Override
    protected void init() {
//        timer.schedule(new TimerTask(){ // TODO jwilliams: remove
//            @Override
//            public void run() {
//                updateControlConnectionAccessTimes();
//            }
//        }, 30000L, 30000L);
    }

    @Override
    protected boolean isValidConnectorConfig(final SsgConnector connector) {
        if (!super.isValidConnectorConfig(connector))
            return false;
        if (!connector.offersEndpoint(SsgConnector.Endpoint.MESSAGE_INPUT)) {
            // The GUI isn't supposed to allow saving enabled FTP connectors without MESSAGE_INPUT checked
            logger.log(Level.WARNING, "FTP connector OID " + connector.getGoid() + " does not allow published service message input");
            return false;
        }
        return true;
    }

    @Override
    public void reportMisconfiguredConnector(Goid connectorGoid) {
        logger.log(Level.WARNING, "Shutting down FTP connector for control port of connector GOID " + connectorGoid + " because it cannot be opened with its current configuration");
        removeConnector(connectorGoid);
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if (!isLicensed())
            return;

        connector = connector.getReadOnlyCopy();
        removeConnector(connector.getGoid());
        if (!connectorIsOwnedByThisModule(connector))
            return;
        final FtpServer ftpServer = new_createFtpServer(connector);
        auditStart(connector.getScheme(), describe(connector));
        try {
            ftpServer.start();
            ftpServers.put(connector.getGoid(), pair(connector,ftpServer));
        } catch (Exception e) {
            throw new ListenerException("Unable to start FTP server " + describe(connector) + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void removeConnector(Goid goid) {
        Pair<SsgConnector,FtpServer> ftpServer = ftpServers.remove(goid);
        if (ftpServer == null)
            return;

        String listener = describe(ftpServer.left);
        auditStop(ftpServer.left.getScheme(), listener);
        try {
            ftpServer.right.stop();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error stopping FTP server for " + listener + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return schemes;
    }

    @Override
    protected void doStart() throws LifecycleException {
        // Start on the refresh event since the auditing system won't work before the initial
        // refresh is completed
        try {
            registerProtocols();
            if (gatewayState.isReadyForMessages()) {
                startInitialConnectors();
            }
        } catch(Exception e) {
            auditError("FTP(S)", "Error during startup.", e);
        }
    }

    @Override
    protected void doStop() throws LifecycleException {
        try {
            unregisterProtocols();
            List<Goid> oidsToStop;
            oidsToStop = new ArrayList<>(ftpServers.keySet());
            for (Goid goid : oidsToStop) {
                removeConnector(goid);
            }
        }
        catch(Exception e) {
            auditError("FTP(S)", "Error while shutting down.", e);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FtpServerManager.class.getName());

    private static final String COMMON_PROPS = "com/l7tech/server/transport/ftp/ftpserver-common.properties";
    private static final String DEFAULT_PROPS = "com/l7tech/server/transport/ftp/ftpserver-normal.properties";
    private static final String SSL_PROPS = "com/l7tech/server/transport/ftp/ftpserver-ssl.properties";
    private static final String PROP_FTP_LISTENER = "config.listeners.";
    private static final String PROP_FTP_IDLE_TIME = "config.connection-manager.default-idle-time";
    private static final String PROP_FTP_POLL_INTERVAL = "config.connection-manager.timeout-poll-interval";
    private static final String PROP_FTP_MAX_CONNECTIONS = "config.connection-manager.max-connection";
    private static final String PROP_FTP_MAX_LOGIN = "config.connection-manager.max-login";
    private static final String CP_FTP_IDLE_TIME = "ftp.connection.idle_timeout";
    private static final String CP_FTP_TIMEOUT_POLL_INTERVAL = "ftp.connection.timeout_poll_interval";
    private static final String CP_FTP_MAX_CONNECTIONS = "ftp.connection.max";
    private static final String CP_FTP_MAX_LOGIN = "ftp.connection.max_login";

    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ClusterPropertyManager clusterPropertyManager;
    private final ServiceManager serviceManager;

    private final Set<String> schemes = caseInsensitiveSet(SCHEME_FTP, SCHEME_FTPS);
    private final Map<Goid, Pair<SsgConnector,FtpServer>> ftpServers = new ConcurrentHashMap<>();

    private String ftpServerDefaultIdleTimeout;
    private String ftpServerTimeoutPoolInterval;
    private String maxConnections;
    private String maxLogin;

    // TODO jwilliams: remove once implemented in SsgFtpServerFactory
    private int toInt(String str, String name) throws ListenerException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            throw new ListenerException("Invalid parameter: " + name + ": " + str, nfe);
        }
    }

    private Properties asFtpProperties(SsgConnector connector) throws ListenerException {
        Properties props = new Properties();

        if (SsgConnector.SCHEME_FTPS.equals(connector.getScheme())) {
            props.putAll(FTPS_PROPERTIES);
        } else {
            props.putAll(DEFAULT_PROPERTIES);
        }

        for (final String propertyName : connector.getPropertyNames()) {
            if (propertyName.equals(SsgConnector.PROP_BIND_ADDRESS) ||
                    propertyName.equals(SsgConnector.PROP_PORT_RANGE_START) ||
                    propertyName.equals(SsgConnector.PROP_PORT_RANGE_COUNT) ||
                    propertyName.equals(SsgConnector.PROP_TLS_CIPHERLIST) ||
                    propertyName.equals(SsgConnector.PROP_TLS_PROTOCOLS)) {
                continue;
            }

            final String propertyValue = connector.getProperty(propertyName);

            if (propertyValue != null) {
                props.setProperty(propertyName, propertyValue);
            }
        }

        String address = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        address = ssgConnectorManager.translateBindAddress(address, connector.getPort());
        if (address == null) address = InetAddressUtil.isUseIpv6() ? "::" : "0.0.0.0";

        int portStart = toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_START), "FTP port range start");
        int portEnd = portStart + toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT), "FTP port range count");
        String passiveRange = portStart + "-" + portEnd;

        String prefix = PROP_FTP_LISTENER + (SsgConnector.SCHEME_FTPS.equals(connector.getScheme()) ? "secure." : "default.");
        props.setProperty(prefix + "port", String.valueOf(connector.getPort()));
        props.setProperty(prefix + "server-address", address);
        props.setProperty(prefix + "data-connection.passive.ports", passiveRange);

        try {
            ftpServerDefaultIdleTimeout = clusterPropertyManager.getProperty(CP_FTP_IDLE_TIME);
            ftpServerTimeoutPoolInterval = clusterPropertyManager.getProperty(CP_FTP_TIMEOUT_POLL_INTERVAL);
            maxConnections = clusterPropertyManager.getProperty(CP_FTP_MAX_CONNECTIONS);
            maxLogin = clusterPropertyManager.getProperty(CP_FTP_MAX_LOGIN);
        } catch (FindException fe){
            //Ignore it
        }

        if (ftpServerDefaultIdleTimeout != null) {
            props.setProperty(PROP_FTP_IDLE_TIME, ftpServerDefaultIdleTimeout);
        }

        if (ftpServerTimeoutPoolInterval != null) {
            props.setProperty(PROP_FTP_POLL_INTERVAL, ftpServerTimeoutPoolInterval);
        }

        if (maxConnections != null) {
            props.setProperty(PROP_FTP_MAX_CONNECTIONS, maxConnections);
        }

        if (maxLogin != null) {
            props.setProperty(PROP_FTP_MAX_LOGIN, maxLogin);
        }

        return props;
    }

    /**
     * Create a new FtpServer instance using settings from the specified connector.
     *
     * @param connector SsgConnector instance describing the control port, the passive port range, and the SSL settings (if any).  Required.
     * @return a new FtpServer instance.  Never null.
     * @throws com.l7tech.server.transport.ListenerException if there is a problem creating the specified FTP server
     */
    private FtpServer createFtpServer(SsgConnector connector) throws ListenerException {
        Goid hardwiredServiceGoid = connector.getGoidProperty(EntityType.SERVICE, SsgConnector.PROP_HARDWIRED_SERVICE_ID, PersistentEntity.DEFAULT_GOID);

        long maxRequestSize = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, -1L);
        String overrideContentTypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        ContentTypeHeader overrideContentType = null;

        try {
            if (overrideContentTypeStr != null)
                overrideContentType = ContentTypeHeader.parseValue(overrideContentTypeStr);
        } catch (IOException e) {
            throw new ListenerException("Unable to start FTP listener: Invalid overridden content type: " + overrideContentTypeStr);
        }

        final CommandFactory ftpCommandFactory = new FtpCommandFactory();
//        final FileSystemManager ftpFileSystem = new VirtualFileSystemManager();
        final UserManager ftpUserManager = new FtpUserManager();
//        final IpRestrictor ftpIpRestrictor = new FtpIpRestrictor();
        final Ftplet messageProcessingFtplet = new MessageProcessingFtplet(
                this,
                messageProcessor,
                soapFaultManager,
                stashManagerFactory,
                messageProcessingEventChannel,
                overrideContentType,
                hardwiredServiceGoid,
                maxRequestSize,
                connector.getGoid(),
                serviceManager,
                connector.getProperty("service")); // the 'service' property is set in the advanced config, tying the ftplet's VFS to an endpoint (?)
        // TODO jwilliams: review use of property to tie connector to service

//        Properties props = asFtpProperties(connector);
//
//        PropertiesConfiguration configuration = new PropertiesConfiguration(props);
//
//        try {
//            DefaultFtpServerContext context = new DefaultFtpServerContext(configuration) {
//                @Override
//                public FtpletContainer getFtpletContainer() {
//                    return messageProcessingFtplet;
//                }
//                @Override
//                public UserManager getUserManager() {
//                    return ftpUserManager;
//                }
//                @Override
//                public FileSystemFactory getFileSystemManager() {
//                    return ftpFileSystem;
//                }
//                @Override
//                public CommandFactory getCommandFactory() {
//                    return ftpCommandFactory;
//                }
//            };
//
//            for(Listener listener : context.getListeners()) {
//                configure(listener.getSsl(), connector);
//                configure(listener.getDataConnectionConfig().getSSL(), connector);
//            }
//
//            return ftpServer;
//        }
//        catch (Exception e) {
//            throw new ListenerException("Unable to create FTP server: " + ExceptionUtils.getMessage(e), e);
//        }

        return null;
    }

    /**
     * Create a new FtpServer instance using settings from the specified connector.
     *
     * @param connector SsgConnector instance describing the control port, the passive port range, and the SSL settings (if any).  Required.
     * @return a new FtpServer instance.  Never null.
     * @throws com.l7tech.server.transport.ListenerException if there is a problem creating the specified FTP server
     */
    public FtpServer new_createFtpServer(SsgConnector connector) throws ListenerException {
        Pair ftpletEntry = new Pair<>(DEFAULT_FTPLET_NAME, createFtplet(connector));

        // TODO jwilliams: ensure all properties are retrieved in the factory, then remove 'asFtpProperties' and original 'createServer' methods
        SsgFtpServerFactory factory =
                new SsgFtpServerFactory(connector, ssgConnectorManager, this, clusterPropertyManager, ftpletEntry);

        return factory.create();
    }

    private Ftplet createFtplet(SsgConnector connector) throws ListenerException {
        Goid hardwiredServiceGoid = connector.getGoidProperty(EntityType.SERVICE, SsgConnector.PROP_HARDWIRED_SERVICE_ID, PersistentEntity.DEFAULT_GOID);

        long maxRequestSize = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, -1L);
        String overrideContentTypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        ContentTypeHeader overrideContentType = null;

        try {
            if (overrideContentTypeStr != null)
                overrideContentType = ContentTypeHeader.parseValue(overrideContentTypeStr);
        } catch (IOException e) {
            throw new ListenerException("Unable to start FTP listener: Invalid overridden content type: " + overrideContentTypeStr);
        }

        return new MessageProcessingFtplet(
                this,
                messageProcessor,
                soapFaultManager,
                stashManagerFactory,
                messageProcessingEventChannel,
                overrideContentType,
                hardwiredServiceGoid,
                maxRequestSize,
                connector.getGoid(),
                serviceManager,
                connector.getProperty("service"));
    }

//    private ConnectionConfig createConnectionConfig() { // TODO jwilliams: look at making these settings available per-connector before migrating then to SsgFtpServerFactory
//        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
//
//        try {
//            ftpServerDefaultIdleTimeout = clusterPropertyManager.getProperty(CP_FTP_IDLE_TIME);
//            ftpServerTimeoutPoolInterval = clusterPropertyManager.getProperty(CP_FTP_TIMEOUT_POLL_INTERVAL);
//            maxConnections = clusterPropertyManager.getProperty(CP_FTP_MAX_CONNECTIONS);
//            maxLogin = clusterPropertyManager.getProperty(CP_FTP_MAX_LOGIN);
//        } catch (FindException e) {
//            //Ignore it
//        }
//
//        if (ftpServerDefaultIdleTimeout != null) { // TODO jwilliams: deprecated
////            props.setProperty(PROP_FTP_IDLE_TIME, ftpServerDefaultIdleTimeout);
//        }
//
//        if (ftpServerTimeoutPoolInterval != null) { // TODO jwilliams: deprecated
////            props.setProperty(PROP_FTP_POLL_INTERVAL, ftpServerTimeoutPoolInterval);
//        }
//
//        if (maxConnections != null) {
////            props.setProperty(PROP_FTP_MAX_CONNECTIONS, maxConnections); // TODO jwilliams: what does this correspond to?
//        }
//
//        if (maxLogin != null) {
//            connectionConfigFactory.setMaxLogins(Integer.parseInt(maxLogin));
////            props.setProperty(PROP_FTP_MAX_LOGIN, maxLogin);
//        }
//
//        return connectionConfigFactory.createConnectionConfig();
//    }

    private List<SsgConnector> findAllEnabledFtpConnectors() throws FindException {
        Collection<SsgConnector> all = ssgConnectorManager.findAll();
        List<SsgConnector> ret = new ArrayList<>();
        for (SsgConnector connector : all) {
            if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                ret.add(connector);
            }
        }
        return ret;
    }

    private void startInitialConnectors() throws LifecycleException {
        try {
            List<SsgConnector> ftps = findAllEnabledFtpConnectors();
            for (SsgConnector connector : ftps) {
                try {
                    addConnector(connector);
                } catch (ListenerException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    logger.log(Level.WARNING, "Unable to start FTP connector GOID " + connector.getGoid() +
                                              " (using control port " + connector.getPort() + "): " + ExceptionUtils.getMessage(e),
                               ExceptionUtils.getDebugException(e));
                }
            }
        } catch (FindException e) {
            throw new RuntimeException("Unable to find initial FTP connectors: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void registerProtocols() {
        final TransportDescriptor ftp = new TransportDescriptor(SCHEME_FTP, false);
        ftp.setFtpBased(true);
        ftp.setSupportsHardwiredServiceResolution(true);
        ftp.setSupportsSpecifiedContentType(true);
        ssgConnectorManager.registerTransportProtocol(ftp, this);

        final TransportDescriptor ftps = new TransportDescriptor(SCHEME_FTPS, true);
        ftps.setFtpBased(true);
        ftps.setSupportsHardwiredServiceResolution(true);
        ftps.setSupportsSpecifiedContentType(true);
        ssgConnectorManager.registerTransportProtocol(ftps, this);
    }

    private void unregisterProtocols() {
        ssgConnectorManager.unregisterTransportProtocol(SCHEME_FTP);
        ssgConnectorManager.unregisterTransportProtocol(SCHEME_FTPS);
    }

    private Properties loadProperties(String propertiesFile) {
        Properties properties = new Properties();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
            if (is == null)
                throw new RuntimeException("Missing " + propertiesFile);

            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e); // shouldn't be possible
        }

        return properties;
    }
}