/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.Component;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.xml.TarariProber;
import com.l7tech.common.xml.tarari.TarariUtil;
import com.l7tech.logging.ServerLogHandler;
import com.l7tech.logging.ServerLogManager;
import com.l7tech.server.audit.SystemAuditListener;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.system.*;
import com.tarari.xml.xpath.XPathCompilerException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

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
public class BootProcess extends ApplicationObjectSupport implements ServerComponentLifecycle, DisposableBean {
    public static final String DEFAULT_LOGPROPERTIES_PATH = "/ssg/etc/conf/ssglog.properties";

    static {
        JdkLoggerConfigurator.configure("com.l7tech.logging",
          new File(DEFAULT_LOGPROPERTIES_PATH).exists() ?
          DEFAULT_LOGPROPERTIES_PATH : "ssglog.properties", true);
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private List _components = new ArrayList();
    private ServerConfig serverConfig;
    private EventManager eventManager;

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
            logger.info("Deleting leftover attachment cache file: " + goner.toString());
            goner.delete();
        }
    }

    public void setServerConfig(ServerConfig config) throws LifecycleException {
        if (TarariProber.isTarariPresent()) {
            logger.info("Initializing Hardware XML Acceleration");
            try {
                TarariUtil.setupIsSoap();
            } catch (XPathCompilerException e) {
                logger.log(Level.WARNING, "Error initializing Tarari board", e);
            }
        } else {
            logger.info("Hardware XML Acceleration Disabled");
        }

        serverConfig = config;
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Couldn't get local IP address. Will use 127.0.0.1 in audit records.", e);
            ipAddress = LOCALHOST_IP;
        }

        deleteOldAttachments(config.getAttachmentDirectory());
        try {
            // Initialize database stuff
            final ApplicationContext springContext = config.getSpringContext();

            // This needs to happen here, early enough that it will notice early events but after the database init
            systemAuditListener = new SystemAuditListener(springContext);
            eventManager.addListener(SystemEvent.class, systemAuditListener);

            // add the server handler programatically after the hibernate is initialized.
            // the handlers specified in the configuraiton get loaded by the system classloader and hibernate
            // stuff lives in the web app classloader
            JdkLoggerConfigurator.addHandler(new ServerLogHandler((ServerLogManager)springContext.getBean("serverLogManager")));

            eventManager.fireInNewTransaction(new Initializing(this, Component.GW_SERVER, ipAddress));
            logger.info("Initializing server");

            setSystemProperties(config);

            logger.info("Initializing cryptography subsystem");
            JceProvider.init();
            logger.info("Using asymmetric cryptography provider: " + JceProvider.getAsymmetricJceProvider().getName());
            logger.info("Using symmetric cryptography provider: " + JceProvider.getSymmetricJceProvider().getName());

            String classnameString = config.getProperty(ServerConfig.PARAM_SERVERCOMPONENTS);
            String[] componentClassnames = classnameString.split("\\s.*?");

            ServerComponentLifecycle component = null;
            for (int i = 0; i < componentClassnames.length; i++) {
                String classname = componentClassnames[i];
                logger.info("Initializing server component '" + classname + "'");
                try {
                    Class clazz = Class.forName(classname);
                    component = (ServerComponentLifecycle)clazz.newInstance();
                } catch (ClassNotFoundException cnfe) {
                    logger.log(Level.SEVERE, "Couldn't initialize server component '" + classname + "'", cnfe);
                } catch (InstantiationException e) {
                    logger.log(Level.SEVERE, "Couldn't initialize server component '" + classname + "'", e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.SEVERE, "Couldn't initialize server component '" + classname + "'", e);
                }

                if (component != null) {
                    try {
                        component.setServerConfig(config);
                        _components.add(component);
                    } catch (LifecycleException e) {
                        logger.log(Level.SEVERE, "Component " + component + " failed to initialize", e);
                    }
                }
            }

            eventManager.fireInNewTransaction(new Initialized(this, Component.GW_SERVER, ipAddress));

            logger.info("Initialized server");
        } catch (IOException e) {
            throw new LifecycleException(e.toString(), e);
        }
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
