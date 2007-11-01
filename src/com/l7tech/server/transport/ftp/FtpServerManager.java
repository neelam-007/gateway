package com.l7tech.server.transport.ftp;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.SoapFaultManager;
import org.apache.ftpserver.ConfigurableFtpServerContext;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.config.PropertiesConfiguration;
import org.apache.ftpserver.ftplet.FileSystemManager;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.interfaces.CommandFactory;
import org.apache.ftpserver.interfaces.FtpServerContext;
import org.apache.ftpserver.interfaces.IpRestrictor;
import org.apache.ftpserver.interfaces.Ssl;
import org.apache.ftpserver.listener.Listener;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates and controls an embedded FTP server for each configured SsgConnector
 * with an FTP or FTPS scheme.
 */
public class FtpServerManager extends TransportModule implements ApplicationListener {

    public FtpServerManager(final AuditContext auditContext,
                            final ClusterPropertyManager clusterPropertyManager,
                            final MessageProcessor messageProcessor,
                            final SoapFaultManager soapFaultManager,
                            final StashManagerFactory stashManagerFactory,
                            final LicenseManager licenseManager,
                            final SsgKeyStoreManager ssgKeyStoreManager,
                            final SsgConnectorManager ssgConnectorManager) {
        super("FTP Server Manager", logger, GatewayFeatureSets.SERVICE_FTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager);

        this.auditContext = auditContext;
        this.clusterPropertyManager = clusterPropertyManager;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.ssgConnectorManager = ssgConnectorManager;
    }

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

        String address = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if (address == null) address = "0.0.0.0";

        int portStart = toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_START), "FTP port range start");
        int portEnd = portStart + toInt(connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT), "FTP port range count");
        String passiveRange = portStart + "-" + portEnd;

        String p = PROP_FTP_LISTENER + prefix;
        props.setProperty(p + "port", String.valueOf(connector.getPort()));
        props.setProperty(p + "server-address", address);
        props.setProperty(p + "data-connection.passive.ports", passiveRange);
        return props;
    }

    private SsgKeyEntry findPrivateKey(SsgConnector connector) throws ListenerException {
        String alias = connector.getKeyAlias();
        if (alias == null)
            return null; // no private key configured
        Long keystore = connector.getKeystoreOid();
        if (keystore == null) keystore = -1L;

        try {
            return ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystore);
        } catch (Exception e) {
            throw new ListenerException("Unable to find private key for connector id " + connector.getOid() + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Create a new FtpServer instance using settings from the specified connector.
     *
     * @param connector SsgConnector instance describing the control port, the passive port range, and the SSL settings (if any).  Required.
     * @return a new FtpServer instance.  Never null.
     * @throws com.l7tech.server.transport.TransportModule.ListenerException if there is a problem creating the specified FTP server
     */
    private FtpServer createFtpServer(SsgConnector connector) throws ListenerException {
        final CommandFactory ftpCommandFactory = new FtpCommandFactory();
        final FileSystemManager ftpFileSystem = new VirtualFileSystemManager();
        final UserManager ftpUserManager = new FtpUserManager(this);
        final IpRestrictor ftpIpRestrictor = new FtpIpRestrictor();
        final Ftplet messageProcessingFtplet = new MessageProcessingFtplet(
                getApplicationContext(),
                this,
                messageProcessor,
                auditContext,
                soapFaultManager,
                clusterPropertyManager,
                stashManagerFactory);

        Properties props = asFtpProperties(connector);
        SsgKeyEntry privateKey = findPrivateKey(connector);

        PropertiesConfiguration configuration = new PropertiesConfiguration(props);

        try {
            FtpServerContext context = new ConfigurableFtpServerContext(configuration) {
                public Ftplet getFtpletContainer() {
                    return messageProcessingFtplet;
                }
                public UserManager getUserManager() {
                    return ftpUserManager;
                }
                public FileSystemManager getFileSystemManager() {
                    return ftpFileSystem;
                }
                public CommandFactory getCommandFactory() {
                    return ftpCommandFactory;
                }
                public IpRestrictor getIpRestrictor() {
                    return ftpIpRestrictor;
                }
            };

            for(Listener listener : context.getListeners()) {
                configure(listener.getSsl(), privateKey);
                configure(listener.getDataConnectionConfig().getSSL(), privateKey);
            }

            return new FtpServer(context);
        }
        catch (Exception e) {
            throw new ListenerException("Unable to create FTP server: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected void init() {
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

    protected boolean isValidConnectorConfig(SsgConnector connector) {
        if (!super.isValidConnectorConfig(connector))
            return false;
        if (!connector.offersEndpoint(SsgConnector.Endpoint.MESSAGE_INPUT)) {
            // The GUI isn't supposed to allow saving enabled FTP connectors without MESSAGE_INPUT checked
            logger.log(Level.WARNING, "FTP connector OID " + connector.getOid() + " does not allow published service message input");
            return false;
        }
        return true;
    }

    private void startInitialConnectors() throws LifecycleException {
        try {
            List<SsgConnector> ftps = findAllEnabledFtpConnectors();
            for (SsgConnector connector : ftps) {
                try {
                    addConnector(connector);
                } catch (ListenerException e) {
                    logger.log(Level.WARNING, "Unable to start FTP connector OID " + connector.getOid() +
                                              " (using control port " + connector.getPort() + "): " + ExceptionUtils.getMessage(e),
                               e);
                }
            }
        } catch (FindException e) {
            throw new RuntimeException("Unable to find initial FTP connectors: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected void doStart() throws LifecycleException {
        // Start on the refresh event since the auditing system won't work before the initial
        // refresh is completed
        try {
            startInitialConnectors();
        } catch(Exception e) {
            auditError("Error during startup.", e);
        }
    }

    protected void doStop() throws LifecycleException {
        try {
            List<Long> oidsToStop;
            oidsToStop = new ArrayList<Long>(ftpServers.keySet());
            for (Long oid : oidsToStop) {
                removeConnector(oid);
            }
        }
        catch(Exception e) {
            auditError("Error while shutting down.", e);
        }
    }

    private static final Logger logger = Logger.getLogger(FtpServerManager.class.getName());

    private static final String DEFAULT_PROPS = "com/l7tech/server/transport/ftp/ftpserver-normal.properties";
    private static final String SSL_PROPS = "com/l7tech/server/transport/ftp/ftpserver-ssl.properties";
    private static final String PROP_FTP_LISTENER = "config.listeners.";

    private final AuditContext auditContext;
    private final ClusterPropertyManager clusterPropertyManager;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final SsgConnectorManager ssgConnectorManager;
    private final Map<Long, FtpServer> ftpServers = Collections.synchronizedMap(new HashMap<Long, FtpServer>());
    private Auditor auditor;

    private void configure(Ssl ssl, SsgKeyEntry privateKey) {
        if (ssl instanceof FtpSsl) {
            FtpSsl ftpSsl = (FtpSsl) ssl;
            ftpSsl.setPrivateKey(privateKey);
        }
    }

    private void auditStart(SsgConnector connector) {
        getAuditor().logAndAudit(SystemMessages.FTPSERVER_START, toString(connector));
    }

    private String toString(SsgConnector connector) {
        return "FTP connector OID " + connector.getOid() + " (control port " + connector.getPort() + ")";

    }

    private void auditStop(String listener) {
        getAuditor().logAndAudit(SystemMessages.FTPSERVER_STOP, listener);
    }

    private void auditError(String message, Exception exception) {
        getAuditor().logAndAudit(SystemMessages.FTPSERVER_ERROR, new String[]{message}, exception);
    }

    private Auditor getAuditor() {
        Auditor auditor = this.auditor;

        if (auditor == null) {
            auditor = new Auditor(this, getApplicationContext(), logger);
            this.auditor = auditor;
        }

        return auditor;
    }

    protected void addConnector(SsgConnector connector) throws ListenerException {
        removeConnector(connector.getOid());
        FtpServer ftpServer = createFtpServer(connector);
        auditStart(connector);
        try {
            ftpServer.start();
            ftpServers.put(connector.getOid(), ftpServer);
        } catch (Exception e) {
            throw new ListenerException("Unable to start FTP server " + toString(connector) + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected void removeConnector(long oid) {
        FtpServer ftpServer = ftpServers.remove(oid);
        if (ftpServer == null)
            return;

        String listener = "connector OID " + oid;
        auditStop(listener);
        try {
            ftpServer.stop();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error stopping FTP server for " + listener + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private final Set<String> schemes = new HashSet<String>(Arrays.asList(SsgConnector.SCHEME_FTP, SsgConnector.SCHEME_FTPS));
    protected Set<String> getSupportedSchemes() {
        return schemes;
    }
}
