/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.StatusUpdater;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.ServerLogManager;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.remote.jini.Services;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceManagerImp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootProcess implements ServerComponentLifecycle {
    private final ServerLogManager serverLogManager = ServerLogManager.getInstance();
    private final Logger logger = Logger.getLogger(getClass().getName());
    private List _components = new ArrayList();

    public void init(ComponentConfig config) throws LifecycleException {
        try {
            logger.info( "Initializing server" );
            setSystemProperties(config);
            HibernatePersistenceManager.initialize();

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
                    logger.log(Level.WARNING, "Couldn't initialize server component '" + classname + "'", cnfe);
                } catch (InstantiationException e) {
                    logger.log(Level.WARNING, "Couldn't initialize server component '" + classname + "'", e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.WARNING, "Couldn't initialize server component '" + classname + "'", e);
                }

                if (component != null) {
                    try {
                        component.init(config);
                        _components.add(component);
                    } catch (LifecycleException e) {
                        logger.log(Level.WARNING, "Component " + component + " failed to initialize!", e);
                    }
                }
            }

            logger.info( "Initialized server" );
        } catch (IOException e) {
            throw new LifecycleException(e.toString(), e);
        } catch (SQLException e) {
            throw new LifecycleException(e.toString(), e);
        }
    }

    public void start() throws LifecycleException {
        try {
            logger.info( "Starting server" );

            initializeAdminServices();
            // make sure the ServiceManager is available. this will also build the service cache
            if (Locator.getDefault().lookup(ServiceManager.class) == null) {
                logger.severe("Could not instantiate the ServiceManager");
            }

            PersistenceContext.getCurrent().beginTransaction();
            // initialize the log dumper
            serverLogManager.suscribeDBHandler();
            /*LogManager logManager = LogManager.getInstance();
            if (logManager instanceof ServerLogManager) {
                ((ServerLogManager)logManager).suscribeDBHandler();
            }*/
            // initialize the process that updates the cluster status info
            initializeClusterStatusUpdate();
            PersistenceContext.getCurrent().commitTransaction();
            PersistenceContext.getCurrent().close();

            logger.info("Starting server components...");
            for (Iterator i = _components.iterator(); i.hasNext();) {
                ServerComponentLifecycle component = (ServerComponentLifecycle)i.next();
                logger.info("Starting component " + component);
                component.start();
            }

            logger.info(BuildInfo.getLongBuildString());

            logger.info("Boot process complete.");
        } catch (SQLException se) {
            throw new LifecycleException(se.toString(), se);
        } catch (TransactionException te) {
            throw new LifecycleException(te.toString(), te);
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

        logger.info("Stopping admin services.");

        RemoteService.unexportAll();
        // stop cache integrity process if necessary
        ServiceManager serviceManager = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (serviceManager != null && serviceManager instanceof ServiceManagerImp) {
            ((ServiceManagerImp)serviceManager).destroy();
        }
        // if we were updating cluster status, stop doing it
        StatusUpdater.stopUpdater();
        logger.info("Closed.");
    }

    private void setSystemProperties(ComponentConfig config) throws IOException {
        // Set system properties
        String sysPropsPath = config.getProperty(ServerConfig.PARAM_SYSTEMPROPS);
        File propsFile = new File(sysPropsPath);
        Properties props = new Properties();

        // Set default properties
        props.setProperty("com.sun.jndi.ldap.connect.pool.timeout", new Integer(30 * 1000).toString());

        if (propsFile.exists()) {
            FileInputStream fis = new FileInputStream(propsFile);
            props.load(fis);
        }

        for (Iterator i = props.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            String value = (String)props.get(name);
            System.setProperty(name, value);
        }
    }

    protected void initializeAdminServices() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    Services.getInstance().start();
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                      "There was an error in initalizing admin services.\n" +
                      " The admin services may not be available.", e);
                }
            }
        }, 3000); 
    }

    protected void initializeClusterStatusUpdate() {
        ClusterInfoManager man = ClusterInfoManager.getInstance();
        try {
            man.updateSelfUptime();
        } catch (UpdateException e) {
            logger.log(Level.WARNING, "error updating boot time of node.", e);
        }
        StatusUpdater.initialize();
    }


}
