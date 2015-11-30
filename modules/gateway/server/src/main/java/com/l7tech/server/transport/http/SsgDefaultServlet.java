package com.l7tech.server.transport.http;

import org.apache.catalina.servlets.DefaultServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SsgDefaultServlet extends DefaultServlet {
    @Override
    protected void serveResource( HttpServletRequest request, HttpServletResponse response, boolean content ) throws IOException, ServletException {
        String path = getRelativePath(request);
        if ( path != null && path.toLowerCase().contains( "web-inf" ) ) {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }
        super.serveResource( request, response, content );
    }
}
