/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 15, 2008
 * Time: 4:30:36 PM
 */
package com.l7tech.standardreports.demo;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

public class StaticHandler extends AbstractHandler {

    public void handle(String s, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, int i) throws IOException, ServletException {
        if(s.equals("/index.html")){
            Request base_request = (httpServletRequest instanceof Request) ? (Request) httpServletRequest : HttpConnection.getCurrentConnection().getRequest();
            base_request.setHandled(true);
            httpServletResponse.setContentType("text/html");
            
            httpServletResponse.getWriter().println("<h3>UI Html</h3>");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
    }
}
