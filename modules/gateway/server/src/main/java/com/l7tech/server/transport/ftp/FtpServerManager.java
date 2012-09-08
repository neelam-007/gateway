package com.l7tech.server.transport.ftp;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;
import org.apache.ftpserver.ConfigurableFtpServerContext;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpSessionImpl;
import org.apache.ftpserver.config.PropertiesConfiguration;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.interfaces.CommandFactory;
import org.apache.ftpserver.interfaces.FtpServerContext;
import org.apache.ftpserver.interfaces.IpRestrictor;
import org.apache.ftpserver.interfaces.Ssl;
import org.apache.ftpserver.listener.Connection;
import org.apache.ftpserver.listener.ConnectionManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.mina.MinaConnection;
import org.apache.mina.common.support.BaseIoSession;
import org.springframework.context.ApplicationEvent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
                            final Timer timer) {
        super("FTP Server Manager", Component.GW_FTPSERVER, logger, SERVICE_FTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKeystore, config );
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.timer = timer;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
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
                auditError( "FTP(S)", "Error during startup.", e );
            }
        }
    }

    //- PROTECTED

    @Override
    protected void init() {
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                updateControlConnectionAccessTimes();
            }
        }, 30000L, 30000L );
    }

    @Override
    protected boolean isValidConnectorConfig(final SsgConnector connector) {
        if (!super.isValidConnectorConfig(connector))
            return false;
        if (!connector.offersEndpoint(SsgConnector.Endpoint.MESSAGE_INPUT)) {
            // The GUI isn't supposed to allow saving enabled FTP connectors without MESSAGE_INPUT checked
            logger.log(Level.WARNING, "FTP connector OID " + connector.getOid() + " does not allow published service message input");
            return false;
        }
        return true;
    }

    @Override
    public void reportMisconfiguredConnector(long connectorOid) {
        logger.log(Level.WARNING, "Shutting down FTP connector for control port of connector OID " + connectorOid + " because it cannot be opened with its current configuration");
        removeConnector(connectorOid);
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
            List<Long> oidsToStop;
            oidsToStop = new ArrayList<Long>(ftpServers.keySet());
            for (Long oid : oidsToStop) {
                removeConnector(oid);
            }
        }
        catch(Exception e) {
            auditError("FTP(S)", "Error while shutting down.", e);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FtpServerManager.class.getName());

    private static final String DEFAULT_PROPS = "com/l7tech/server/transport/ftp/ftpserver-normal.properties";
    private static final String SSL_PROPS = "com/l7tech/server/transport/ftp/ftpserver-ssl.properties";
    private static final String PROP_FTP_LISTENER = "config.listeners.";

    private final Set<String> schemes = caseInsensitiveSet( SCHEME_FTP, SCHEME_FTPS );
    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final Timer timer;
    private final Map<Long, Pair<SsgConnector,FtpServer>> ftpServers = new ConcurrentHashMap<Long, Pair<SsgConnector,FtpServer>>();

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
        final Ftplet messageProcessingFtplet = new MessageProcessingFtplet(
                this,
                messageProcessor,
                soapFaultManager,
                stashManagerFactory,
                messageProcessingEventChannel,
                overrideContentType,
                hardwiredServiceOid,
                maxRequestSize,
                connector.getOid());

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
        List<SsgConnector> ret = new ArrayList<SsgConnector>();
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

    private void registerProtocols() {
        final TransportDescriptor ftp = new TransportDescriptor( SCHEME_FTP, false);
        ftp.setFtpBased(true);
        ftp.setSupportsHardwiredServiceResolution(true);
        ftp.setSupportsSpecifiedContentType(true);
        ssgConnectorManager.registerTransportProtocol(ftp, this);

        final TransportDescriptor ftps = new TransportDescriptor( SCHEME_FTPS, true);
        ftps.setFtpBased(true);
        ftps.setSupportsHardwiredServiceResolution(true);
        ftps.setSupportsSpecifiedContentType(true);
        ssgConnectorManager.registerTransportProtocol(ftps, this);
    }

    private void unregisterProtocols() {
        ssgConnectorManager.unregisterTransportProtocol(SCHEME_FTP);
        ssgConnectorManager.unregisterTransportProtocol(SCHEME_FTPS);
    }

    /**
     * Update the last access times for the MINA sessions of the control
     * connections.
     *
     * The FtpServer does not correctly manage the control connection
     * timeout when there are active data connections.
     */
    private void updateControlConnectionAccessTimes() {
        if ( logger.isLoggable(Level.FINER) )
            logger.log( Level.FINER, "Updating FTP control connection usage for active data connections.");

        // just ignore this bit ...
        Field field = null;
        try {
            field = MinaConnection.class.getDeclaredField("session");
            field.setAccessible(true);
        } catch (NoSuchFieldException nsfe) {
            logger.warning("IoSession field not found for MINA connection, not updating control connection usage for active data connections.");
        }

        if ( field != null) {
            for ( final Pair<SsgConnector,FtpServer> server : ftpServers.values() ) {
                if ( !server.right.isStopped() && !server.right.isSuspended() ) {
                    ConnectionManager cm = server.right.getServerContext().getConnectionManager();
                    List<Connection> connections = cm.getAllConnections();

                    for ( Connection connection : connections ) {
                        FtpSession session = connection.getSession();
                        FtpRequest request = session.getCurrentRequest();

                         if ( connection instanceof MinaConnection &&
                                 request != null &&
                                  ( "STOR".equals(request.getCommand()) ||
                                    "STOU".equals(request.getCommand()))) {
                            logger.log(Level.FINE,
                                    "Updating FTP control connection usage for data connection ''{0}''",
                                    request.getArgument());

                            synchronized ( connection ) {
                                if ( session instanceof FtpSessionImpl) {
                                    ((FtpSessionImpl)session).updateLastAccessTime();
                                }

                                try {
                                    BaseIoSession baseIoSession = (BaseIoSession) field.get(connection);
                                    baseIoSession.increaseReadBytes(0);
                                    baseIoSession.increaseWrittenBytes(0);
                                } catch (IllegalAccessException iae) {
                                    logger.log(Level.WARNING,
                                            "Error updating FTP control connection usage for data connection ''{0}'', message is ''{1}''",
                                            new String[]{ request.getArgument(), iae.getMessage()});
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
