/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.Component;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.BootMessages;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.server.event.system.*;
import com.l7tech.server.service.ServiceManagerImp;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.identity.cert.ClientCertManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootProcess extends ApplicationObjectSupport
  implements ServerComponentLifecycle, DisposableBean, InitializingBean {
    public static final String DEFAULT_LOGPROPERTIES_PATH = ServerConfig.getInstance().getProperty("configDirectory") + File.separator + "ssglog.properties";

    static {
        JdkLoggerConfigurator.configure("com.l7tech.logging",
          new File(DEFAULT_LOGPROPERTIES_PATH).exists() ?
            DEFAULT_LOGPROPERTIES_PATH : "ssglog.properties", true);
    }

    private List _components = new ArrayList();
    private ServerConfig serverConfig;
    private Auditor auditor;
    private Logger logger;

    /**
     * Constructor for bean usage via subclassing.
     */
    public BootProcess() {
    }

    private void deleteOldAttachments(File attachmentDir) {
        File[] goners = attachmentDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                String local = pathname.getName();
                return local != null && local.startsWith("att") && local.endsWith(".part");
            }
        });

        for (int i = 0; i < goners.length; i++) {
            File goner = goners[i];
            String filename = goner.toString();
            auditor.logAndAudit(BootMessages.DELETING_ATTACHMENT, new String[]{filename});
            goner.delete();
        }
    }

    public void setServerConfig(ServerConfig config) throws LifecycleException {
        serverConfig = config;

    }

    public void start() throws LifecycleException {
        getApplicationContext().publishEvent(new Starting(this, Component.GW_SERVER, ipAddress));
        logger.info("Starting server");

        logger.info("Starting server components...");
        for (Iterator i = _components.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Starting component " + component);
            component.start();
        }

        logger.info(BuildInfo.getLongBuildString());
        getApplicationContext().publishEvent(new Started(this, Component.GW_SERVER, ipAddress));
        logger.info("Boot process complete.");
    }

    public void stop() throws LifecycleException {
        getApplicationContext().publishEvent(new Stopping(this, Component.GW_SERVER, ipAddress));

        logger.info("Stopping server components");

        List stnenopmoc = new ArrayList();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (Iterator i = stnenopmoc.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Stopping component " + component);
            component.stop();
        }

        getApplicationContext().publishEvent(new Stopped(this, Component.GW_SERVER, ipAddress));
        logger.info("Stopped.");
    }

    public void close() throws LifecycleException {
        getApplicationContext().publishEvent(new Closing(this, Component.GW_SERVER, ipAddress));

        logger.info("Closing server components");

        List stnenopmoc = new ArrayList();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (Iterator i = stnenopmoc.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Closing component " + component);
            component.close();
        }
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
        final ApplicationContext springContext = serverConfig.getSpringContext();
        logger = Logger.getLogger(BootProcess.class.getName());

        auditor = new Auditor(this, springContext, logger);

        GlobalTarariContext context = TarariLoader.getGlobalContext();
        if (context != null) {
            auditor.logAndAudit(BootMessages.XMLHARDWARE_INIT);
            context.compile();
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
        try {

            // This needs to happen here, early enough that it will notice early events but after the database init

            getApplicationContext().publishEvent(new Initializing(this, Component.GW_SERVER, ipAddress));
            logger.info("Initializing server");

            setSystemProperties(serverConfig);

            auditor.logAndAudit(BootMessages.CRYPTO_INIT);
            JceProvider.init();
            auditor.logAndAudit(BootMessages.CRYPTO_ASYMMETRIC, new String[]{JceProvider.getAsymmetricJceProvider().getName()});
            auditor.logAndAudit(BootMessages.CRYPTO_SYMMETRIC, new String[]{JceProvider.getSymmetricJceProvider().getName()});

            String classnameString = serverConfig.getProperty(ServerConfig.PARAM_SERVERCOMPONENTS);
            String[] componentClassnames = classnameString.split("\\s.*?");

            ServerComponentLifecycle component = null;
            for (int i = 0; i < componentClassnames.length; i++) {
                String classname = componentClassnames[i];
                logger.info("Initializing server component '" + classname + "'");
                try {
                    Class clazz = Class.forName(classname);
                    component = (ServerComponentLifecycle)clazz.newInstance();
                } catch (ClassNotFoundException cnfe) {
                    auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{classname}, cnfe);
                } catch (InstantiationException e) {
                    auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{classname}, e);
                } catch (IllegalAccessException e) {
                    auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{classname}, e);
                }

                if (component != null) {
                    try {
                        component.setServerConfig(serverConfig);
                        _components.add(component);
                    } catch (LifecycleException e) {
                        auditor.logAndAudit(BootMessages.COMPONENT_INIT_FAILED, new String[]{component.getClass().getName()}, e);
                    }
                }
            }

            getApplicationContext().publishEvent(new Initialized(this, Component.GW_SERVER, ipAddress));

            // initialize service cache after all this
            ServiceManagerImp serviceManager = (ServiceManagerImp)springContext.getBean("serviceManagerTarget");
            logger.info("initializing the service cache");
            serviceManager.initiateServiceCache();

            // Make sure certs without thumbprints get them
            TrustedCertManager tcm = (TrustedCertManager)getApplicationContext().getBean("trustedCertManager");
            tcm.findByThumbprint(null);
            tcm.findByThumbprint("");
            tcm.findBySki(null);
            tcm.findBySki("");

            ClientCertManager ccm = (ClientCertManager)getApplicationContext().getBean("clientCertManager");
            ccm.findByThumbprint(null);
            ccm.findByThumbprint("");
            ccm.findBySki(null);
            ccm.findBySki("");

            logger.info("Initialized server");
        } catch (IOException e) {
            throw new LifecycleException(e.toString(), e);
        }
    }

    private void setSystemProperties(ServerConfig config) throws IOException {
        // Set system properties
        String sysPropsPath = config.getProperty(ServerConfig.PARAM_SYSTEMPROPS);
        File propsFile = new File(sysPropsPath);
        Properties props = new Properties();

        // Set default properties
        props.setProperty("com.sun.jndi.ldap.connect.pool.timeout", new Integer(30 * 1000).toString());

        InputStream is = null;
        try {
            if (propsFile.exists()) {
                is = new FileInputStream(propsFile);
            } else {
                is = getClass().getClassLoader().getResourceAsStream("system.properties");
            }

            if (is != null) props.load(is);

            for (Iterator i = props.keySet().iterator(); i.hasNext();) {
                String name = (String)i.next();
                String value = (String)props.get(name);
                logger.info("Setting system property " + name + "=" + value);
                System.setProperty(name, value);
            }
        } finally {
            if (is != null) is.close();
        }
    }

    private String ipAddress;
    public static final String LOCALHOST_IP = "127.0.0.1";

}
