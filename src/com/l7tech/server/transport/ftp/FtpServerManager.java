package com.l7tech.server.transport.ftp;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.util.Collection;
import java.util.Collections;

import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FileSystemManager;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ConfigurableFtpServerContext;
import org.apache.ftpserver.interfaces.FtpServerContext;
import org.apache.ftpserver.interfaces.CommandFactory;
import org.apache.ftpserver.interfaces.IpRestrictor;
import org.apache.ftpserver.config.PropertiesConfiguration;

import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.common.LicenseManager;
import com.l7tech.cluster.ClusterPropertyManager;

/**
 * Creates and controls the embedded FTP server.
 *
 * <p>The ftp server is configured via properties that are passed in during
 * construction.</p>
 *
 * <p>In addition to the usual Apache FTP Server properties the manager expects:</p>
 *
 * <ul>
 *   <li><code>ssgftp.listeners<code> - CSV for enabled listeners (e.g. 'default, secure')</li>
 * </ul>
 *
 * <p>Note that any properties for listeners that are not explictly enabled
 * are filtered out before the FTP server is configured.</p>
 *
 * @author Steve Jones
 */
public class FtpServerManager extends LifecycleBean {

    //- PUBLIC

    /**
     *
     */
    public FtpServerManager(final AuditContext auditContext,
                            final ClusterPropertyManager clusterPropertyManager,
                            final MessageProcessor messageProcessor,
                            final SoapFaultManager soapFaultManager,
                            final StashManagerFactory stashManagerFactory,
                            final LicenseManager licenseManager,
                            final Properties properties) {
        super("FTP Server Manager", logger, GatewayFeatureSets.SERVICE_FTP_MESSAGE_INPUT, licenseManager);

        this.auditContext = auditContext;
        this.clusterPropertyManager = clusterPropertyManager;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.properties = filterListeners(properties);
    }

    //- PROTECTED

    protected void init() {
        if (properties != null) {
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

            PropertiesConfiguration configuration = new PropertiesConfiguration(properties);

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

                ftpServer = new FtpServer(context);
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Unable to initialize FTP server.", e);
            }
        }
    }

    protected void doStart() throws LifecycleException {
        if (properties == null) {
            auditNotConfigured();
        }
        else {
            // Start on the refresh event since the auditing system won't work before the initial
            // refresh is completed
            auditStart(properties.getProperty(PROP_LISTENERS, ""));

            try {
                if (ftpServer == null)
                    throw new IllegalStateException("Not initialized.");

                if (ftpServer.isStopped())
                    ftpServer.start();
                else
                    logger.log(Level.WARNING, "FtpServer is already running!");
            }
            catch(Exception e) {
                auditError("Error during startup.", e);
            }
        }
    }

    protected void doStop() throws LifecycleException {
        if (properties != null) {
            try {
                if (ftpServer != null)
                    ftpServer.stop();
                auditStop();
            }
            catch(Exception e) {
                auditError("Error while shutting down.", e);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(FtpServerManager.class.getName());

    private static final String PROP_LISTENERS = "ssgftp.listeners";
    private static final String PROP_LISTENER_PREFX = "ssgftp.";
    private static final String PROP_LISTENER_ENABLED = ".enabled";
    private static final String PROP_FTP_LISTENER = "config.listeners.";

    private final AuditContext auditContext;
    private final ClusterPropertyManager clusterPropertyManager;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final Properties properties;
    private Auditor auditor;
    private FtpServer ftpServer;

    /**
     * Filter out any disabled listeners.
     *
     * @param properties The properties to filter
     * @return the properties or null if no listeners are enabled
     */
    private Properties filterListeners(Properties properties) {
        Properties filteredProperties = new Properties();
        boolean anyListeners = false;

        String[] listeners = properties.getProperty(PROP_LISTENERS, "").split("[, ]");
        for (String listener : listeners) {
            if (listener.length()==0) continue; //split will give empty tokens for multiple spaces, etc
            
            boolean enabled = Boolean.valueOf(properties.getProperty(PROP_LISTENER_PREFX + listener + PROP_LISTENER_ENABLED, "false").trim());
            if (enabled) {
                logger.log(Level.CONFIG, "FTP ''{0}'' is enabled.", listener);
                anyListeners = true;

                String propertyPrefix = PROP_FTP_LISTENER + listener;
                for (String property : (Collection<String>) Collections.list(properties.propertyNames())) {
                    if (property.startsWith(propertyPrefix)) {
                        filteredProperties.setProperty(property, properties.getProperty(property));
                    }
                }
            }
            else {
                logger.log(Level.CONFIG, "FTP ''{0}'' is disabled.", listener);
            }
        }

        if (anyListeners) {
            // copy all other properties over
            for (String property : (Collection<String>) Collections.list(properties.propertyNames())) {
                if (!property.startsWith(PROP_FTP_LISTENER)) {
                    filteredProperties.setProperty(property, properties.getProperty(property));
                }
            }
        }
        else {
            filteredProperties = null;    
        }

        return filteredProperties;
    }

    private void auditNotConfigured() {
        getAuditor().logAndAudit(SystemMessages.FTPSERVER_NOT_CONFIGURED);
    }

    private void auditStart(String listeners) {
        getAuditor().logAndAudit(SystemMessages.FTPSERVER_START, listeners);
    }

    private void auditStop() {
        getAuditor().logAndAudit(SystemMessages.FTPSERVER_STOP);
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
}
