/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.ServerLogHandler;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.server.policy.DefaultGatewayPolicies;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceManagerImp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootProcess implements ServerComponentLifecycle {
    static {
        JdkLoggerConfigurator.configure("com.l7tech", "com/l7tech/server/resources/logging.properties", true);
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private List _components = new ArrayList();

    public void init(ComponentConfig config) throws LifecycleException {
        PersistenceContext context = null;
        try {
            // Initialize database stuff
            HibernatePersistenceManager.initialize();

            // add the server handler programatically after the hibernate is initialized.
            // the handlers specified in the configuraiton get loaded by the system classloader and hibernate
            // stuff lives in the web app classloader
            JdkLoggerConfigurator.addHandler(new ServerLogHandler());

            context = PersistenceContext.getCurrent();
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
                        if (component instanceof TransactionalComponent) context.beginTransaction();
                        component.init(config);
                        _components.add(component);
                        if (component instanceof TransactionalComponent) context.commitTransaction();
                    } catch (LifecycleException e) {
                        logger.log(Level.SEVERE, "Component " + component + " failed to initialize", e);
                    } catch (TransactionException e) {
                        logger.log(Level.SEVERE, "Component " + component + " could not commit its initialization process", e);
                    }
                }
            }

            logger.info("Initialized server");
        } catch (IOException e) {
            throw new LifecycleException(e.toString(), e);
        } catch (SQLException e) {
            throw new LifecycleException(e.toString(), e);
        } finally {
            if (context != null) context.close();
        }
    }

    public void start() throws LifecycleException {
        PersistenceContext context = null;
        try {
            context = PersistenceContext.getCurrent();
            logger.info("Starting server");

            // make sure the ServiceManager is available. this will also build the service cache
            if (Locator.getDefault().lookup(ServiceManager.class) == null) {
                logger.severe("Could not instantiate the ServiceManager");
            }

            DefaultGatewayPolicies.getInstance();

            logger.info("Starting server components...");
            for (Iterator i = _components.iterator(); i.hasNext();) {
                ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
                logger.info("Starting component " + component);
                try {
                    if (component instanceof TransactionalComponent) context.beginTransaction();
                    component.start();
                    if (component instanceof TransactionalComponent) context.commitTransaction();
                } catch (TransactionException e) {
                    logger.log(Level.SEVERE, "Component " + component + " could not commit its startup process", e);
                }
            }

            logger.info(BuildInfo.getLongBuildString());
            logger.info("Boot process complete.");
        } catch (SQLException se) {
            throw new LifecycleException(se.toString(), se);
        } finally {
            if (context != null) context.close();
        }
    }

    public void stop() throws LifecycleException {
        logger.info("Stopping server components");

        List stnenopmoc = new ArrayList();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (Iterator i = stnenopmoc.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Stopping component " + component);
            component.stop();
        }

        logger.info("Stopped.");
    }

    public void close() throws LifecycleException {
        logger.info("Closing server components");

        List stnenopmoc = new ArrayList();
        stnenopmoc.addAll(_components);
        Collections.reverse(stnenopmoc);

        for (Iterator i = stnenopmoc.iterator(); i.hasNext();) {
            ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
            logger.info("Closing component " + component);
            component.close();
        }

        // stop cache integrity process if necessary
        ServiceManager serviceManager = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (serviceManager != null && serviceManager instanceof ServiceManagerImp) {
            ((ServiceManagerImp)serviceManager).destroy();
        }
        logger.info("Closed.");
    }

    private void setSystemProperties(ComponentConfig config) throws IOException {
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
                System.setProperty(name, value);
            }
        } finally {
            if (is != null) is.close();
        }
    }
}
