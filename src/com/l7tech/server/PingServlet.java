/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import net.sf.hibernate.Session;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class PingServlet extends HttpServlet {
    private static final Logger logger = LogManager.getInstance().getSystemLogger();

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        HibernatePersistenceContext context = null;
        Writer out = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session s = context.getSession();
            if ( s.isOpen() ) {
                response.setStatus( HttpServletResponse.SC_OK );
                out = response.getWriter();
                writePage( out, OK_TITLE, OK_MESSAGE );
            } else {
                String msg = "Got a bogus session from the persistence context";
                logger.info(msg);
                throw new IllegalStateException( msg );
            }
        } catch ( Exception e ) {
            response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            logger.log( Level.INFO, ERROR_TITLE, e );
            out = response.getWriter();
            writePage( out, ERROR_TITLE, ERROR_MESSAGE );
        } finally {
            if ( context != null ) try { context.close(); } catch ( Throwable e ) { }
            if ( out != null ) try { out.flush(); } catch ( Throwable e ) { }
            if ( out != null ) try { out.close(); } catch ( Throwable e ) { }
        }
    }

    private void writePage( Writer out, String title, String message ) throws IOException {
        out.write( "<html>\n<head><title>" );
        out.write( title );
        out.write( "</title></head>\n<body>\n<h1>" );
        out.write( message );
        out.write( "</h1>\n");
        out.write( "<hr/>\n" );
        out.write( "<i><font size=\"-2\">" );
        out.write( BuildInfo.getLongBuildString() );
        out.write( "</font></i>\n</body>\n" );
        out.write("</html>\n" );
    }

    public static final String OK_TITLE = "OK";
    public static final String OK_MESSAGE = "OK";
    public static final String ERROR_TITLE = "Internal Server Error";
    public static final String ERROR_MESSAGE = "The server is not in an operational state.";
}
