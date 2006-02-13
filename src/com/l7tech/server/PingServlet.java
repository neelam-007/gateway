/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.BuildInfo;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.hibernate.SessionFactory;
import org.hibernate.Session;

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
    private static final Logger logger = Logger.getLogger(PingServlet.class.getName());
    private WebApplicationContext applicationContext;

    public void init( ServletConfig config ) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }

    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        Writer out = null;
        try {
            SessionFactory sessionFactory = (SessionFactory)applicationContext.getBean("sessionFactory");
            Session s = sessionFactory.openSession();
            if ( s.isOpen() ) {
                response.setStatus( HttpServletResponse.SC_OK );
                response.setContentType("text/html; charset=utf-8");
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
