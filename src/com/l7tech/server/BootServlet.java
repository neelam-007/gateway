/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.objectmodel.HibernatePersistenceManager;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public class BootServlet extends HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        try {
            HibernatePersistenceManager.initialize();
        } catch ( IOException ioe ) {
            throw new ServletException( ioe );
        } catch ( SQLException se ) {
            throw new ServletException( se );
        } catch ( NamingException ne ) {
            throw new ServletException( ne );
        }
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        PrintWriter out = response.getWriter();
        out.println( "<b>The server has already been initialized!</b>" );
    }
}
