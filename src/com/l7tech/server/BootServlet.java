/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.jini.Services;
import com.l7tech.jini.export.RemoteService;
import com.l7tech.objectmodel.HibernatePersistenceManager;

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
    private final Logger logger = Logger.getLogger(BootServlet.class.getName());

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        // note fla, more exception catching => important to diagnose why server does not boot properly
        try {
            initializeAdminServices();
            HibernatePersistenceManager.initialize();
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
