/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.remote.jini.Services;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceManagerImp;
import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.StatusUpdater;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class BootServlet extends HttpServlet {
    // private final Logger logger = Logger.getLogger(BootServlet.class.getName());
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private final ServerConfig _config = ServerConfig.getInstance();

    public void init( ServletConfig sc ) throws ServletException {
        super.init( sc );

        boolean failure = false;
        try {
            setSystemProperties();

            initializeAdminServices();
            HibernatePersistenceManager.initialize();
            // make sure the ServiceManager is available
            if (Locator.getDefault().lookup(ServiceManager.class) == null) {
                logger.severe("Could not instantiate the ServiceManager");
            }

            PersistenceContext.getCurrent().beginTransaction();
            initializeClusterStatusUpdate();
            PersistenceContext.getCurrent().commitTransaction();
            PersistenceContext.getCurrent().close();


        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL ERROR IN BOOT SERVLET", e);
            failure = true;
            throw new ServletException(e);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "ERROR IN BOOT SERVLET", e);
            failure = true;
            throw new ServletException(e);
        } finally {
            if (failure) {
                destroy();
            }
        }

        logger.info( BuildInfo.getLongBuildString() );
        logger.info("Boot servlet complete.");
    }

    private void setSystemProperties() throws IOException {
        // Set system properties
        String sysPropsPath = _config.getSystemPropertiesPath();
        File propsFile = new File( sysPropsPath );
        Properties props = new Properties();

        // Set default properties
        props.setProperty("com.sun.jndi.ldap.connect.pool.timeout", new Integer( 30 * 1000 ).toString() );
        props.setProperty( ServerConfig.PROP_HOSTNAME, _config.getHostname() );

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

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        PrintWriter out = response.getWriter();
        out.println( "<b>The server has already been initialized!</b>" );
    }

    /**
     * Called by the servlet container to indicate to a
     * servlet that the servlet is being taken out of service.
     *
     */
    public void destroy() {
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
