/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.remote.jini.export;

import com.l7tech.common.util.HexUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * The servlet that works as a class server.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ClassServerServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ClassServerServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void service(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
        final String requestURI = request.getRequestURI();
        logger.fine("The request URI is " + requestURI);

        if (requestURI.startsWith(URI_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String resource = requestURI.substring(URI_PREFIX.length()+1);
        logger.fine("The resource is '"+resource+"'");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream resUrl = cl.getResourceAsStream(resource);

        if (null == resUrl) {
            logger.warning("Unable to read the resource '"+resource+"'");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.fine("The resource '"+resource+"' is not found");
            return;
        }

        byte[] bytes = HexUtils.slurpStream(resUrl, 32768);
         response.setContentLength(bytes.length);
        response.setContentType("application/java");
        response.getOutputStream().write(bytes);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    private static final String URI_PREFIX = "/classerver/";
}
