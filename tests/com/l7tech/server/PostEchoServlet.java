/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Echoes anything posted back to the response
 */
public class PostEchoServlet extends HttpServlet {
    private static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
            "<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\">\n" +
            "<web-app>\n" +
            "    <servlet>\n" +
            "        <servlet-name>PostEchoServlet</servlet-name>\n" +
            "        <servlet-class>com.l7tech.server.PostEchoServlet</servlet-class>\n" +
            "    </servlet>\n" +
            "\n" +
            "    <servlet-mapping>\n" +
            "        <servlet-name>PostEchoServlet</servlet-name>\n" +
            "        <url-pattern>/postecho</url-pattern>\n" +
            "    </servlet-mapping>\n" +
            "</web-app>";


    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException 
    {
        servletResponse.setContentType(servletRequest.getContentType());
        servletResponse.setStatus(HttpServletResponse.SC_OK);
        servletResponse.setContentLength(servletRequest.getContentLength());
        servletResponse.setBufferSize(0);
        InputStream in = servletRequest.getInputStream();
        OutputStream out = servletResponse.getOutputStream();
        byte[] buf = new byte[8192];
        int got;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
        }
        in.close();
        out.close();
    }
}
