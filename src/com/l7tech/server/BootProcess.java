/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.common.util.Locator;
import com.l7tech.common.BuildInfo;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceManagerImp;
import com.l7tech.logging.LogManager;
import com.l7tech.logging.ServerLogManager;
import com.l7tech.remote.jini.Services;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.StatusUpdater;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public class BootProcess implements ServerComponentLifecycle {
    private Logger logger = LogManager.getInstance().getSystemLogger();

    public void init() throws LifecycleException {
        try {
            setSystemProperties();
        } catch ( IOException e ) {
            throw new LifecycleException( e.toString(), e );
        }
    }

    public void start() throws LifecycleException {
        try {
            initializeAdminServices();
            HibernatePersistenceManager.initialize();
            // make sure the ServiceManager is available. this will also build the service cache
            if (Locator.getDefault().lookup(ServiceManager.class) == null) {
                logger.severe("Could not instantiate the ServiceManager");
            }

            PersistenceContext.getCurrent().beginTransaction();
            // initialize the log dumper
            LogManager logManager = LogManager.getInstance();
            if (logManager instanceof ServerLogManager) {
                ((ServerLogManager)logManager).suscribeDBHandler();
            }
            // initialize the process that updates the cluster status info
            initializeClusterStatusUpdate();
            PersistenceContext.getCurrent().commitTransaction();
            PersistenceContext.getCurrent().close();

            logger.info( BuildInfo.getLongBuildString() );
            logger.info("Boot process complete.");
        } catch ( IOException ioe ) {
            throw new LifecycleException( ioe.toString(), ioe );
        } catch ( SQLException se ) {
            throw new LifecycleException( se.toString(), se );
        } catch ( TransactionException te ) {
            throw new LifecycleException( te.toString(), te );
        }
    }

    public void stop() throws LifecycleException {
    }

    public void close() throws LifecycleException {
        logger.info("Stopping admin services.");
        RemoteService.unexportAll();
        // stop cache integrity process if necessary
        ServiceManager serviceManager = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (serviceManager != null && serviceManager instanceof ServiceManagerImp) {
            ((ServiceManagerImp)serviceManager).destroy();
        }
        // if we were updating cluster status, stop doing it
        StatusUpdater.stopUpdater();
    }

    private void setSystemProperties() throws IOException {
        // Set system properties
        String sysPropsPath = ServerConfig.getInstance().getSystemPropertiesPath();
        File propsFile = new File( sysPropsPath );
        Properties props = new Properties();

        // Set default properties
        props.setProperty("com.sun.jndi.ldap.connect.pool.timeout", new Integer( 30 * 1000 ).toString() );

        if ( propsFile.exists() ) {
            FileInputStream fis = new FileInputStream( propsFile );
            props.load(fis);
        }

        for (Iterator i = props.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            String value = (String)props.get(name);
            System.setProperty( name, value );
        }
    }

    protected void initializeAdminServices() {
        try {
            Services.getInstance().start();
        } catch (Exception e) {
            logger.log(Level.WARNING,
              "There was an error in initalizing admin services.\n" +
              " The admin services may not be available.", e);
        }
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
