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
import com.l7tech.remote.jini.Services;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.service.ServiceManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootServlet extends HttpServlet {
    // private final Logger logger = Logger.getLogger(BootServlet.class.getName());
    private final Logger logger = LogManager.getInstance().getSystemLogger();

    public void init( ServletConfig config ) throws ServletException {
        ServerConfig.getInstance();
        super.init( config );
        try {
            initializeAdminServices();
            HibernatePersistenceManager.initialize();
            // make sure the ServiceManager is available
            if (Locator.getDefault().lookup(ServiceManager.class) == null) {
                logger.severe("Could not instantiate the ServiceManager");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL ERROR IN BOOT SERVLET", e);
            throw new ServletException(e);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "ERROR IN BOOT SERVLET", e);
            throw new ServletException(e);
        }
        logger.info( BuildInfo.getLongBuildString() );
        logger.info("Boot servlet complete.");
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
}
