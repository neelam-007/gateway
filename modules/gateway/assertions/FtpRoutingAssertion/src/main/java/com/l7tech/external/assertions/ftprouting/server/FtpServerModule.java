package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.ftp.*;
import com.l7tech.server.util.*;
import com.l7tech.util.*;
import org.apache.ftpserver.ConfigurableFtpServerContext;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.config.PropertiesConfiguration;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.interfaces.*;
import org.apache.ftpserver.listener.Listener;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Pair.pair;

/**
 * @author nilic
 */
public class FtpServerModule extends FtpServerManager implements ApplicationListener {

    public FtpServerModule(final Config config,
                           final GatewayState gatewayState,
                           final MessageProcessor messageProcessor,
                           final SoapFaultManager soapFaultManager,
                           final StashManagerFactory stashManagerFactory,
                           final LicenseManager licenseManager,
                           final DefaultKey defaultKeystore,
                           final TrustedCertServices trustedCertServices,
                           final SsgConnectorManager ssgConnectorManager,
                           final EventChannel messageProcessingEventChannel,
                           final Timer timer,
                           final ClusterPropertyManager clusterPropertyManager,
                           final ServiceManager serviceManager) {
        super(config, gatewayState, messageProcessor, soapFaultManager, stashManagerFactory,
              licenseManager, defaultKeystore, trustedCertServices, ssgConnectorManager,
              messageProcessingEventChannel,timer);
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.clusterPropertyManager = clusterPropertyManager;
        this.serviceManager = serviceManager;
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);

    }

    static FtpServerModule createModule( final ApplicationContext appContext ) {
        LicenseManager licenseManager = getBean(appContext, "licenseManager", LicenseManager.class);
        SoapFaultManager soapFaultManager = getBean(appContext, "soapFaultManager", SoapFaultManager.class);
        SsgConnectorManager ssgConnectorManager = getBean(appContext, "ssgConnectorManager", SsgConnectorManager.class);
        TrustedCertServices trustedCertServices = getBean(appContext, "trustedCertServices", TrustedCertServices.class);
        DefaultKey defaultKey = getBean(appContext, "defaultKey", DefaultKey.class);
        Config config = getBean(appContext, "serverConfig", Config.class);
        GatewayState gatewayState = getBean(appContext, "gatewayState", GatewayState.class);
        MessageProcessor messageProcessor = getBean(appContext, "messageProcessor", MessageProcessor.class);
        StashManagerFactory stashManagerFactory = getBean(appContext, "stashManagerFactory", StashManagerFactory.class);
        EventChannel messageProcessingEventChannel = getBean(appContext, "messageProcessingEventChannel", EventChannel.class);
        ClusterPropertyManager clusterPropertyManager = getBean(appContext, "clusterPropertyManager", ClusterPropertyManager.class);
        Timer managedBackgroundTimer = getBean(appContext, "managedBackgroundTimer", ManagedTimer.class);
        ServiceManager serviceManager = getBean(appContext, "serviceManager", ServiceManager.class);
        final FtpServerModule module = new FtpServerModule( config, gatewayState, messageProcessor, soapFaultManager, stashManagerFactory, licenseManager,
                defaultKey, trustedCertServices, ssgConnectorManager, messageProcessingEventChannel, managedBackgroundTimer, clusterPropertyManager, serviceManager);
        module.setApplicationContext( appContext );
        return module;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (LifecycleException e) {
                auditError( "FTP(S)", "Error during startup.", e );
            }
        }
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        connector = connector.getReadOnlyCopy();
        removeConnector(connector.getOid());
        if (!connectorIsOwnedByThisModule(connector))
            return;
        final FtpServer ftpServer = createFtpServer(connector);
        auditStart( connector.getScheme(), describe( connector ) );
        try {
            ftpServer.start();
            ftpServers.put(connector.getOid(), pair(connector,ftpServer));
        } catch (Exception e) {
            throw new ListenerException("Unable to start FTP server " + describe( connector ) + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void removeConnector(long oid) {
        Pair<SsgConnector,FtpServer> ftpServer = ftpServers.remove(oid);
        if (ftpServer == null)
            return;

        String listener = describe( ftpServer.left );
        auditStop( ftpServer.left.getScheme(), listener);
        try {
            ftpServer.right.stop();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error stopping FTP server for " + listener + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FtpServerModule.class.getName());

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

    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final Map<Long, Pair<SsgConnector,FtpServer>> ftpServers = new ConcurrentHashMap<>();
    private String ftpServerDefaultIdleTimeout;
    private String ftpServerTimeoutPoolInterval;
    private String maxConnections;
    private String maxLogin;

    private ClusterPropertyManager clusterPropertyManager;
    private ServiceManager serviceManager;

    private int toInt(String str, String name) throws ListenerException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            throw new ListenerException("Invalid parameter: " + name + ": " + str, nfe);
        }
    }

    private Properties asFtpProperties(SsgConnector connector) throws ListenerException {
        String propsFile;
        String prefix;
        if (SsgConnector.SCHEME_FTPS.equals(connector.getScheme())) {
            propsFile = SSL_PROPS;
            prefix = "secure.";
        } else {
            propsFile = DEFAULT_PROPS;
            prefix = "default.";
        }

        Properties props = new Properties();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(propsFile);
            if (is == null)
                throw new RuntimeException("Missing " + propsFile);
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e); // shouldn't be possible
        }

        for ( final String propertyName : connector.getPropertyNames() ) {
            if (propertyName.equals(SsgConnector.PROP_BIND_ADDRESS) ||
                    propertyName.equals(SsgConnector.PROP_PORT_RANGE_START) ||
                    propertyName.equals(SsgConnector.PROP_PORT_RANGE_COUNT) ||
                    propertyName.equals(SsgConnector.PROP_TLS_CIPHERLIST) ||
                    propertyName.equals(SsgConnector.PROP_TLS_PROTOCOLS))
                continue;

            final String propertyValue = connector.getProperty( propertyName );
            if ( propertyValue != null ) {
                props.setProperty( propertyName, propertyValue );
            }
        }

        String address = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        address = ssgConnectorManager.translateBindAddress(address, connector.getPort());
        if (address == null) address = InetAddressUtil.isUseIpv6() ? "::" : "0.0.0.0";

        int portStart = toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_START), "FTP port range start");
        int portEnd = portStart + toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT), "FTP port range count");
        String passiveRange = portStart + "-" + portEnd;

        String p = PROP_FTP_LISTENER + prefix;
        props.setProperty(p + "port", String.valueOf(connector.getPort()));
        props.setProperty(p + "server-address", address);
        props.setProperty(p + "data-connection.passive.ports", passiveRange);

        try{
            ftpServerDefaultIdleTimeout = clusterPropertyManager.getProperty(CP_FTP_IDLE_TIME);
            ftpServerTimeoutPoolInterval =  clusterPropertyManager.getProperty(CP_FTP_TIMEOUT_POLL_INTERVAL);
            maxConnections =  clusterPropertyManager.getProperty(CP_FTP_MAX_CONNECTIONS);
            maxLogin = clusterPropertyManager.getProperty(CP_FTP_MAX_LOGIN);
        }
        catch (FindException fe){
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
        long hardwiredServiceOid = connector.getLongProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, -1L);
        String initServiceUri = connector.getProperty("service");
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
        final FileSystemManager ftpFileSystem = new VirtualFileSystemManager();
        final UserManager ftpUserManager = new FtpUserManager(this);
        final IpRestrictor ftpIpRestrictor = new FtpIpRestrictor();
        final Ftplet messageProcessingFtplet = new MessageProcessingFtpletSubsystem(
                this,
                messageProcessor,
                soapFaultManager,
                stashManagerFactory,
                messageProcessingEventChannel,
                overrideContentType,
                hardwiredServiceOid,
                maxRequestSize,
                connector.getOid(),
                serviceManager,
                initServiceUri);

        Properties props = asFtpProperties(connector);

        PropertiesConfiguration configuration = new PropertiesConfiguration(props);

        try {
            FtpServerContext context = new ConfigurableFtpServerContext(configuration) {
                @Override
                public Ftplet getFtpletContainer() {
                    return messageProcessingFtplet;
                }
                @Override
                public UserManager getUserManager() {
                    return ftpUserManager;
                }
                @Override
                public FileSystemManager getFileSystemManager() {
                    return ftpFileSystem;
                }
                @Override
                public CommandFactory getCommandFactory() {
                    return ftpCommandFactory;
                }
                @Override
                public IpRestrictor getIpRestrictor() {
                    return ftpIpRestrictor;
                }
            };

            for(Listener listener : context.getListeners()) {
                configure(listener.getSsl(), connector);
                configure(listener.getDataConnectionConfig().getSSL(), connector);
            }

            return new FtpServer(context);
        }
        catch (Exception e) {
            throw new ListenerException("Unable to create FTP server: " + ExceptionUtils.getMessage(e), e);
        }
    }

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
                    logger.log(Level.WARNING, "Unable to start FTP connector OID " + connector.getOid() +
                            " (using control port " + connector.getPort() + "): " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                }
            }
        } catch (FindException e) {
            throw new RuntimeException("Unable to find initial FTP connectors: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void configure(Ssl ssl, SsgConnector ssgConnector) {
        if (ssl instanceof FtpSsl) {
            FtpSsl ftpSsl = (FtpSsl) ssl;
            ftpSsl.setTransportModule(this);
            ftpSsl.setSsgConnector(ssgConnector);
        } else if (ssl != null)
            throw new IllegalStateException("Unexpected Ssl implementation of class: " + ssl.getClass());
    }

    @Override
    protected String describe( final SsgConnector connector ) {
        return connector.getName() + " (#" +connector.getOid() + ",v" + connector.getVersion() + ") on control port " + connector.getPort();
    }

 }
