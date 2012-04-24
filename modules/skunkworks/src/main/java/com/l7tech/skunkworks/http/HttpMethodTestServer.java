package com.l7tech.skunkworks.http;

import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A small test HTTP server, based on Jetty, for testing unusual combinations of request methods
 * (such as DELETE with a body, per Bug #12168).
 * <p/>
 * This server does not depend on any other code (aside from Jetty and layer7-util), does not require any core
 * code changes, and simply echoes back the request content type and body (while also printing it to stdout).
 */
public class HttpMethodTestServer {
    public static void main(String[] args) throws Exception {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                final Request req = (Request) request;
                final String requestBody = new String(IOUtils.slurpStream(req.getInputStream()), Charsets.UTF8);
                response.setContentType("text/plain");
                response.setStatus(200);
                final String responseString = "Request method=" + req.getMethod() + " contenttype=" + req.getContentType() + " body:\n\n" + requestBody;
                System.out.println(responseString);
                response.getWriter().println(responseString);
                req.setHandled(true);
            }
        };

        Server server = new Server(4111);
        server.setHandler(handler);
        server.start();
    }
}
