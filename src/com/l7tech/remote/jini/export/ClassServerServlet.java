/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.remote.jini.export;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The servlet that works as a Jini class server.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ClassServerServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ClassServerServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void service(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
        logger.warning("Thew query string is "+request.getQueryString());
        logger.warning("Thew request URI is "+request.getRequestURI());
    }
}
