/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.logging.LogManager;
import com.l7tech.jini.Services;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class BootServlet extends HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        // note fla, more exception catching => important to diagnose why server does not boot properly
        try {
            Services.getInstance().start();
            HibernatePersistenceManager.initialize();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL ERROR IN BOOT SERVLET", e);
            throw new ServletException(e);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "ERROR IN BOOT SERVLET", e);
            throw new ServletException(e);
        }
        logger.info("Boot servlet complete.");
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        PrintWriter out = response.getWriter();
        out.println( "<b>The server has already been initialized!</b>" );
    }
    private Logger logger = LogManager.getInstance().getSystemLogger();
}
