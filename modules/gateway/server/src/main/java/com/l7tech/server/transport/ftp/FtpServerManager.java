package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.*;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.ftpserver.*;
import org.springframework.context.ApplicationEvent;

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
    private static final Logger logger = Logger.getLogger(FtpServerManager.class.getName());

    private final GatewayState gatewayState;
    private final Set<String> schemes = caseInsensitiveSet(SCHEME_FTP, SCHEME_FTPS);
    private final Map<Goid, Pair<SsgConnector,FtpServer>> ftpServers = new ConcurrentHashMap<>();

    // can't autowire or give in constructor due to circular requirements of FtpServerManager in sub-factories
    private SsgFtpServerFactory ssgFtpServerFactory;

    public FtpServerManager(final Config config,
                            final GatewayState gatewayState,
                            final LicenseManager licenseManager,
                            final DefaultKey defaultKeystore,
                            final TrustedCertServices trustedCertServices,
                            final SsgConnectorManager ssgConnectorManager) {
        super("FTP Server Manager", Component.GW_FTPSERVER, logger, SERVICE_FTP_MESSAGE_INPUT,
                licenseManager, ssgConnectorManager, trustedCertServices, defaultKeystore, config);

        this.gatewayState = gatewayState;
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
        logger.log(Level.WARNING, "Shutting down FTP connector for control port of connector GOID " + connectorGoid +
                " because it cannot be opened with its current configuration");
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

        final FtpServer ftpServer = createFtpServer(connector);

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
            List<Goid> goidsToStop;
            goidsToStop = new ArrayList<>(ftpServers.keySet());
            for (Goid goid : goidsToStop) {
                removeConnector(goid);
            }
        }
        catch(Exception e) {
            auditError("FTP(S)", "Error while shutting down.", e);
        }
    }

    /**
     * Create a new FtpServer instance using settings from the specified connector.
     *
     * @param connector SsgConnector instance describing the control port, the passive port range, and the SSL settings (if any).  Required.
     * @return a new FtpServer instance.  Never null.
     * @throws com.l7tech.server.transport.ListenerException if there is a problem creating the specified FTP server
     */
    public FtpServer createFtpServer(SsgConnector connector) throws ListenerException {
        if (null == ssgFtpServerFactory) {
            ssgFtpServerFactory = getApplicationContext().getBean(SsgFtpServerFactory.class);
        }

        return ssgFtpServerFactory.create(connector);
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
}