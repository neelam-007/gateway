/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.Component;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.ServerTarariContext;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.SystemAuditListener;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.system.*;
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
    public static final String DEFAULT_LOGPROPERTIES_PATH = "/ssg/etc/conf/ssglog.properties";

    static {
        JdkLoggerConfigurator.configure("com.l7tech.logging",
          new File(DEFAULT_LOGPROPERTIES_PATH).exists() ?
            DEFAULT_LOGPROPERTIES_PATH : "ssglog.properties", true);
    }

    private List _components = new ArrayList();
    private ServerConfig serverConfig;
    private EventManager eventManager;
    private Auditor auditor;

    /**
     * Constructor for bean usage via subclassing.
     */
    public BootProcess(EventManager eventManager) {
        this.eventManager = eventManager;
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
        eventManager.fireInNewTransaction(new Starting(this, Component.GW_SERVER, ipAddress));
        logger.info("Starting server");


        logger.info("Starting server components...");
        for (Iterator i = _components.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Starting component " + component);
            component.start();
        }

        logger.info(BuildInfo.getLongBuildString());
        eventManager.fireInNewTransaction(new Started(this, Component.GW_SERVER, ipAddress));
        logger.info("Boot process complete.");
    }

    public void stop() throws LifecycleException {
        eventManager.fireInNewTransaction(new Stopping(this, Component.GW_SERVER, ipAddress));

        logger.info("Stopping server components");

        List stnenopmoc = new ArrayList();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (Iterator i = stnenopmoc.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Stopping component " + component);
            component.stop();
        }

        eventManager.fire(new Stopped(this, Component.GW_SERVER, ipAddress));
        logger.info("Stopped.");
    }

    public void close() throws LifecycleException {
        eventManager.fireInNewTransaction(new Closing(this, Component.GW_SERVER, ipAddress));

        logger.info("Closing server components");

        List stnenopmoc = new ArrayList();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (Iterator i = stnenopmoc.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Closing component " + component);
            component.close();
        }
        eventManager.fire(new Closed(this, Component.GW_SERVER, ipAddress));
        eventManager.removeListener(systemAuditListener);
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
        auditor = new Auditor((AuditContext)serverConfig.getSpringContext().getBean("auditContext"), Logger.getLogger(getClass().getName()));

        ServerTarariContext context = TarariLoader.getServerContext();
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
            // Initialize database stuff
            final ApplicationContext springContext = serverConfig.getSpringContext();

            // This needs to happen here, early enough that it will notice early events but after the database init
            systemAuditListener = new SystemAuditListener(springContext);
            eventManager.addListener(SystemEvent.class, systemAuditListener);

            eventManager.fireInNewTransaction(new Initializing(this, Component.GW_SERVER, ipAddress));
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

            eventManager.fireInNewTransaction(new Initialized(this, Component.GW_SERVER, ipAddress));

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

    private SystemAuditListener systemAuditListener;
    private String ipAddress;
    public static final String LOCALHOST_IP = "127.0.0.1";

}
