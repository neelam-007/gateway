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
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        servletResponse.setContentType(servletRequest.getContentType());
        servletResponse.setStatus(HttpServletResponse.SC_OK);
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
