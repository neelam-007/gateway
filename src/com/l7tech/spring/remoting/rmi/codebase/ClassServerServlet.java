/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.spring.remoting.rmi.codebase;

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

        if (!requestURI.startsWith(URI_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        byte[] bytes;
        InputStream in = null;
        try {
            String resource = null;
            for (int i = 1; i >= 0; i--) {
                resource = requestURI.substring(URI_PREFIX.length() - i);
                logger.fine("Lookup for the resource '" + resource + "'");
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                in = cl.getResourceAsStream(resource);
                if (in != null) {
                    break;
                }
            }

            if (null == in) {
                logger.warning("Unable to locate the resource '" + resource + "'");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                logger.fine("The resource '" + resource + "' is not found");
                return;
            }

            bytes = HexUtils.slurpStream(in, 32768);
            response.setContentLength(bytes.length);
            response.setContentType("application/java");
            response.getOutputStream().write(bytes);
            response.setStatus(HttpServletResponse.SC_OK);
        } finally {
            if (null != in) {
                in.close();
            }
        }
    }

    static final String URI_PREFIX = "/classserver/";
}
