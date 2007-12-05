/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.Component;
import com.l7tech.common.audit.BootMessages;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.Service;
import com.l7tech.common.util.ShutdownExceptionHandler;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.system.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootProcess
    extends ApplicationObjectSupport
    implements DisposableBean, InitializingBean
{
    private static final Logger logger;
    private boolean wasStarted = false;

    static {
        ServerConfig serverConfig = ServerConfig.getInstance();
        String logConfigurationPath = serverConfig.getPropertyCached("configDirectory") + File.separator + "ssglog.properties";

        if ( new File(logConfigurationPath).exists() ) {
            JdkLoggerConfigurator.configure("com.l7tech.logging", "ssglog.properties", logConfigurationPath, true);
        } else {
            // specify "ssglog.properties" twice since the non-default one can be overridden by
            // a system property.
            JdkLoggerConfigurator.configure("com.l7tech.logging", "ssglog.properties", "ssglog.properties", true);
        }

        logger = Logger.getLogger(BootProcess.class.getName());
    }

    /**
     * Constructor for bean usage via subclassing.
     */
    public BootProcess() {
        _components = new ArrayList<ServerComponentLifecycle>();
    }

    private void deleteOldAttachments(File attachmentDir) {
        File[] goners = attachmentDir.listFiles(new FileFilter() {
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

    public void start() throws LifecycleException {
        initCaches();
        wasStarted = true;
        getApplicationContext().publishEvent(new Starting(this, Component.GW_SERVER, ipAddress));
        logger.info("Starting server");

        logger.info("Starting server components...");
        for (ServerComponentLifecycle component : _components) {
            logger.info("Starting component " + component);
            component.start();
        }

        logger.info("Starting discovered server components...");
        for (ServerComponentLifecycle component : getDiscoveredComponents()) {
            logger.info("Starting component " + component);
            component.start();
        }

        getApplicationContext().publishEvent(new Started(this, Component.GW_SERVER, ipAddress));
        logger.info("Boot process complete.");
    }

    public void stop() throws LifecycleException {
        getApplicationContext().publishEvent(new Stopping(this, Component.GW_SERVER, ipAddress));

        logger.info("Stopping server components");

        List<ServerComponentLifecycle> stnenopmoc = new ArrayList<ServerComponentLifecycle>();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (ServerComponentLifecycle component : stnenopmoc) {
            logger.info("Stopping component " + component);
            component.stop();
        }

        logger.info("Stopping discovered server components");

        List<ServerComponentLifecycle> stnenopmocDerevocsid = new ArrayList<ServerComponentLifecycle>();
        stnenopmocDerevocsid.addAll(getDiscoveredComponents());
        Collections.reverse(stnenopmocDerevocsid);

        for (ServerComponentLifecycle component : stnenopmocDerevocsid) {
            logger.info("Stopping discovered component " + component);
            component.stop();
        }

        getApplicationContext().publishEvent(new Stopped(this, Component.GW_SERVER, ipAddress));
        logger.info("Stopped.");
    }

    public void close() throws LifecycleException {
        getApplicationContext().publishEvent(new Closing(this, Component.GW_SERVER, ipAddress));

        logger.info("Closing server components");

        List<ServerComponentLifecycle> stnenopmoc = new ArrayList<ServerComponentLifecycle>();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (ServerComponentLifecycle component : stnenopmoc) {
            logger.info("Closing component " + component);
            component.close();
        }

        logger.info("Closing discovered server components");

        List<ServerComponentLifecycle> stnenopmocDerevocsid = new ArrayList<ServerComponentLifecycle>();
        stnenopmocDerevocsid.addAll(getDiscoveredComponents());
        Collections.reverse(stnenopmocDerevocsid);

        for (ServerComponentLifecycle component : stnenopmocDerevocsid) {
            logger.info("Closing discovered component " + component);
            component.close();
        }

        ShutdownExceptionHandler.getInstance().shutdownNotify();

        getApplicationContext().publishEvent(new Closed(this, Component.GW_SERVER, ipAddress));
        logger.info("Closed.");
    }

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     *
     * @throws Exception in case of shutdown errors.
     *                   Exceptions will get logged but not rethrown to allow
     *                   other beans to release their resources too.
     */
    public void destroy() throws Exception {
        try {
            stop();
        } finally {
            close();
        }
    }


    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        ApplicationContext applicationContext = getApplicationContext();

        auditor = new Auditor(this, applicationContext, logger);

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
        auditor.logAndAudit(BootMessages.CRYPTO_ASYMMETRIC, JceProvider.getAsymmetricJceProvider().getName());
        auditor.logAndAudit(BootMessages.CRYPTO_SYMMETRIC, JceProvider.getSymmetricJceProvider().getName());

        String classnameString = serverConfig.getPropertyCached(ServerConfig.PARAM_SERVERCOMPONENTS);
        String[] componentClassnames = classnameString.split("\\s.*?");

        for (String classname : componentClassnames) {
            ServerComponentLifecycle component = null;
            logger.info("Initializing server component '" + classname + "'");
            try {
                Class clazz = Class.forName(classname);
                component = (ServerComponentLifecycle) clazz.newInstance();
            } catch (ClassNotFoundException cnfe) {
                auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{classname}, cnfe);
            } catch (InstantiationException e) {
                auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{classname}, e);
            } catch (IllegalAccessException e) {
                auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{classname}, e);
            }

            if (component != null) {
                try {
                    if (component instanceof ApplicationContextAware) {
                        ((ApplicationContextAware) component).setApplicationContext(applicationContext);
                    }
                    component.setServerConfig(serverConfig);
                    _components.add(component);
                } catch (LifecycleException e) {
                    auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{component.getClass().getName()}, e);
                }
            }
        }

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

    private Collection<ServerComponentLifecycle> getDiscoveredComponents() {
        Collection<ServerComponentLifecycle> comps = discoveredComponents;

        if (comps == null) {
            comps = (Collection<ServerComponentLifecycle>)
                    getApplicationContext().getBeansOfType(ServerComponentLifecycle.class, false, false).values();
            discoveredComponents = comps;
        }

        return comps;
    }

    public static final String LOCALHOST_IP = "127.0.0.1";

    private List<ServerComponentLifecycle> _components;
    private Collection<ServerComponentLifecycle> discoveredComponents;
    private ServerConfig serverConfig;
    private Auditor auditor;
    private String ipAddress;
}
