/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class BootServlet extends HttpServlet {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private ServerComponentLifecycle _boot;

    public void init() throws ServletException {
        boolean failure = false;
        try {
            System.getProperties().remove("java.protocol.handler.pkgs");
            //hack: remove what catalina added as java.protocol.handler.pkgs and let the default
            // jdk protocol handler resolution
            ApplicationContext acx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            if (acx == null) {
                throw new ServletException("Configuration error; could not get application context");
            }
            _boot = (ServerComponentLifecycle)acx.getBean("ssgBoot");
            _boot.start();
            logger.fine("Boot process completed");
        } catch ( Throwable e ) {
            logger.log(Level.SEVERE, "ERROR IN BOOT SERVLET", e);
            failure = true;
            throw new ServletException(e);
        } finally {
            if (failure) destroy();
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
        super.destroy();
        try {
            if ( _boot != null ) {
                try {
                    _boot.stop();
                } finally {
                    _boot.close();
                }
            }
        } catch ( LifecycleException e ) {
            logger.warning( "Caught exception during server shutdown" );
        }
    }


}
