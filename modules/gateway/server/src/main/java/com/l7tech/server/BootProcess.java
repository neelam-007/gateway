/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.BootMessages;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Service;
import com.l7tech.util.ShutdownExceptionHandler;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContext;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.system.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootProcess
    extends ApplicationObjectSupport
    implements DisposableBean
{
    private static final Logger logger = Logger.getLogger(BootProcess.class.getName());
    private boolean wasStarted = false;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructor for bean usage via subclassing.
     */
    public BootProcess() {
    }

    private void deleteOldAttachments(File attachmentDir) {
        File[] goners = attachmentDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String local = pathname.getName();
                return local != null && local.startsWith("att") && local.endsWith(".part");
            }
        });

        for (File goner : goners) {
            String filename = goner.toString();
            auditor.logAndAudit(BootMessages.DELETING_ATTACHMENT, filename);
            goner.delete();
        }
    }

    public void setServerConfig(ServerConfig config) throws LifecycleException {
        serverConfig = config;
    }

    public void setOtherPropertiesFiles(Map<String, String> otherPropertiesFiles) {
        this.otherPropertiesFiles = otherPropertiesFiles;
    }

    public void setSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public void start() throws LifecycleException {
        try {
            initialize();
        } catch (LifecycleException e) {
            throw e;
        } catch (Exception e) {
            throw new LifecycleException(e);
        }

        initCaches();
        wasStarted = true;
        getApplicationContext().publishEvent(new Starting(this, Component.GW_SERVER, ipAddress));
        logger.info("Starting server");

        logger.info("Starting server components...");
        for (ServerComponentLifecycle component : getDiscoveredComponents()) {
            logger.info("Starting component " + component);
            component.start();
        }

        running.set(true);
        getApplicationContext().publishEvent(new Started(this, Component.GW_SERVER, ipAddress));
        logger.info("Boot process complete.");
    }

    public void stop() throws LifecycleException {
        if ( running.get() ) {
            getApplicationContext().publishEvent(new Stopping(this, Component.GW_SERVER, ipAddress));


            logger.info("Stopping server components");

            List<ServerComponentLifecycle> stnenopmocDerevocsid = new ArrayList<ServerComponentLifecycle>();
            stnenopmocDerevocsid.addAll(getDiscoveredComponents());
            Collections.reverse(stnenopmocDerevocsid);

            for (ServerComponentLifecycle component : stnenopmocDerevocsid) {
                logger.info("Stopping discovered component " + component);
                component.stop();
            }

            running.set(false);
            getApplicationContext().publishEvent(new Stopped(this, Component.GW_SERVER, ipAddress));
            logger.info("Stopped.");
        } else {
            logger.info("Stopped.");
        }
    }

    public void close() throws LifecycleException {
        getApplicationContext().publishEvent(new Closing(this, Component.GW_SERVER, ipAddress));


        logger.info("Closing server components");

        List<ServerComponentLifecycle> stnenopmocDerevocsid = new ArrayList<ServerComponentLifecycle>();
        stnenopmocDerevocsid.addAll(getDiscoveredComponents());
        Collections.reverse(stnenopmocDerevocsid);

        for (ServerComponentLifecycle component : stnenopmocDerevocsid) {
            logger.info("Closing discovered component " + component);
            try {
                component.close();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while closing component: " + ExceptionUtils.getMessage(e), e);
            }
        }

        ShutdownExceptionHandler.getInstance().shutdownNotify();

        getApplicationContext().publishEvent(new Closed(this, Component.GW_SERVER, ipAddress));
        logger.info("Closed.");
    }

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     */
    @Override
    public void destroy() {
        try {
            stop();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while destroying BootProcess: " + ExceptionUtils.getMessage(e), e);
        } finally {
            try {
                close();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while closing BootProcess: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }


    private void initialize() throws Exception {        
        ApplicationContext applicationContext = getApplicationContext();

        auditor = new Auditor(this, applicationContext, logger);

        if (otherPropertiesFiles == null || otherPropertiesFiles.isEmpty()) return;

        for (String systemPropertyPrefix : otherPropertiesFiles.keySet()) {
            String filename = otherPropertiesFiles.get(systemPropertyPrefix);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(filename));
                props.load(fis);
                systemProperties.setSystemProperties(props, systemPropertyPrefix, false);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't read from " + filename + "; ignoring", e);
            } finally {
                ResourceUtils.closeQuietly(fis);
            }
        }

        logger.info(BuildInfo.getLongBuildString());
        logConfiguredFactories();

        GlobalTarariContext context = TarariLoader.getGlobalContext();
        if (context != null) {
            auditor.logAndAudit(BootMessages.XMLHARDWARE_INIT);
            TarariLoader.compile();
        } else {
            auditor.logAndAudit(BootMessages.XMLHARDWARE_DISABLED);
        }

        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            auditor.logAndAudit(BootMessages.NO_IP, null, e);
            ipAddress = LOCALHOST_IP;
        }

        deleteOldAttachments(serverConfig.getAttachmentDirectory());
        // This needs to happen here, early enough that it will notice early events but after the database init

        applicationContext.publishEvent(new Initializing(this, Component.GW_SERVER, ipAddress));
        logger.info("Initializing server");


        auditor.logAndAudit(BootMessages.CRYPTO_INIT);
        JceProvider.init();
        auditor.logAndAudit(BootMessages.CRYPTO_ASYMMETRIC, String.valueOf(JceProvider.getInstance().getAsymmetricProvider()));
        auditor.logAndAudit(BootMessages.CRYPTO_SYMMETRIC, String.valueOf(JceProvider.getInstance().getSymmetricProvider()));

        applicationContext.publishEvent(new Initialized(this, Component.GW_SERVER, ipAddress));

        logger.info("Initialized server");
    }

    private void initCaches() throws LifecycleException {
        if (!wasStarted) {
            logger.info("Initializing server cache");
            ApplicationContext applicationContext = getApplicationContext();

            try {
                // Make sure certs without thumbprints get them
                TrustedCertManager tcm = (TrustedCertManager)applicationContext.getBean("trustedCertManager");
                tcm.findByThumbprint(null);
                tcm.findByThumbprint("");
                tcm.findBySki(null);
                tcm.findBySki("");

                ClientCertManager ccm = (ClientCertManager)applicationContext.getBean("clientCertManager");
                ccm.findByThumbprint(null);
                ccm.findByThumbprint("");
                ccm.findBySki(null);
                ccm.findBySki("");
            } catch (FindException fe) {
                // see bugzilla 2162, if a bad cert somehow makes it in the db, we should not prevent the gateway to boot
                logger.log(Level.WARNING, "Could not thumbprint certs. Something " +
                    "corrupted in trusted_cert or client_cert table.",
                    fe);
            }
        }
    }

    /**
     * Log the configured factories for XML parsing and XSLT transforms.
     *
     * <p>This steps through the various configuration options in reverse
     * order.</p>
     *
     * <p>We are currently ignoring the XPathFactory</p>
     *
     * @see javax.xml.parsers.DocumentBuilderFactory#newInstance()
     * @see javax.xml.parsers.SAXParserFactory#newInstance()
     * @see javax.xml.transform.TransformerFactory#newInstance()
     */
    private void logConfiguredFactories() {
        // providers we are interested in
        Class[] factoryClasses = {
            DocumentBuilderFactory.class,
            SAXParserFactory.class,
            TransformerFactory.class,
        };

        // load JAXP properties if any
        Properties jaxpProps = new Properties();
        File file = new File(System.getProperty("java.home"));
        file = new File(file, "lib");
        file = new File(file, "jaxp.properties");
        InputStream in = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            in = new FileInputStream(file);
            jaxpProps.load(in);
        } catch (IOException e) {
            logger.log( Level.FINE, "Error reading properties file '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
        } finally {
            ResourceUtils.closeQuietly(in);
        }

        // initialize to default
        Map<String, String> providers = new HashMap<String, String>();
        for (Class factoryClass : factoryClasses) {
            String factoryKey = factoryClass.getName();
            providers.put(factoryKey, "DEFAULT");
        }

        // next check for Services API
        for (Class factoryClass : factoryClasses) {
            String factoryKey = factoryClass.getName();
            Iterator names = Service.providerClassNames(factoryClass);
            if (names.hasNext()) {
                providers.put(factoryKey, names.next() + " - [Services API]");
            }
        }

        // then for jaxp.properties
        for (Class factoryClass : factoryClasses) {
            String factoryKey = factoryClass.getName();
            String value = jaxpProps.getProperty(factoryKey);
            if (value != null) {
                providers.put(factoryKey, value + " - [jaxp.properties]");
            }
        }

        // finally check system properties
        for (Class factoryClass : factoryClasses) {
            String factoryKey = factoryClass.getName();
            String value = System.getProperty(factoryKey);
            if (value != null) {
                providers.put(factoryKey, value + " - [System property]");
            }
        }

        // loggit
        for (Class factoryClass : factoryClasses) {
            String factoryKey = factoryClass.getName();
            logger.config("Provider for '" + factoryKey + "' is '" + providers.get(factoryKey) + "'.");
        }
    }

    @SuppressWarnings({"unchecked"})
    private Collection<ServerComponentLifecycle> getDiscoveredComponents() {
        Collection<ServerComponentLifecycle> comps = discoveredComponents;

        if (comps == null) {
            comps = (Collection<ServerComponentLifecycle>)
                    getApplicationContext().getBeansOfType(ServerComponentLifecycle.class, false, false).values();
            discoveredComponents = comps;
        }

        return comps;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public static final String LOCALHOST_IP = "127.0.0.1";

    private Collection<ServerComponentLifecycle> discoveredComponents;
    private Map<String, String> otherPropertiesFiles;
    private SystemProperties systemProperties;
    private ServerConfig serverConfig;
    private Auditor auditor;
    private String ipAddress;

}
