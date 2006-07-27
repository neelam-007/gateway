/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.admin;

import com.l7tech.policy.assertion.credential.LoginCredentials;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * Servlet that provides access to the Manager applet.
 */
public class ManagerAppletServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ManagerAppletServlet.class.getName());

    private static final String PAGE_OPEN = "<html><head><title>TITLE</title></head>\n<body>";
    private static final String PAGE_CLOSE = "</body></html>";

    private static final String APPLET_OPEN =
            "<applet codebase=\"CODEBASE\"  archive=\"Manager.jar\"\n" +
                    "code=\"com.l7tech.console.AppletMain.class\"\n" +
                    "        width=\"100%\" height=\"100%\">\n";

    private static final String APPLET_CLOSE =
            "\n" +
                    "Java runtime 5.0 or higher is required.  You can <a href=\"http://www.java.com/getjava/\">download it for free</a>.\n" +
                    "</applet>";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hresp)
            throws ServletException, IOException
    {

        Object credsObj = hreq.getAttribute(ManagerAppletFilter.PROP_CREDS);
        if (!(credsObj instanceof LoginCredentials))
            throw new ServletException("ManagerAppletServlet: request was not authenticated"); // shouldn't be possible

        LoginCredentials creds = (LoginCredentials)credsObj;
        String login = creds.getLogin();
        char[] pass = creds.getCredentials();

        if (login == null || login.length() < 1 || pass == null)
            throw new ServletException("ManagerAppletServlet: request was not authenticated (missing login or creds)"); // shouldn't be possible

        hresp.setContentType("text/html");
        hresp.setStatus(200);
        OutputStream os = hresp.getOutputStream();
        PrintStream ps = new PrintStream(os);
        try {
            String codebase = hreq.getScheme() + "://" + hreq.getServerName() + ":" + hreq.getServerPort() + "/ssg/webadmin/";

            String pageOpen = PAGE_OPEN;
            pageOpen = pageOpen.replaceAll("TITLE", hreq.getServerName());

            String appletOpen = APPLET_OPEN;

            appletOpen = appletOpen.replaceAll("CODEBASE", codebase);

            ps.println(pageOpen);
            ps.println(appletOpen);
            //emitParam(ps, "image", "loading.png");
            //emitParam(ps, "progressbar", "true");
            //emitParam(ps, "boxmessage", "Loading SecureSpan Manager...");
            //emitParam(ps, "boxbgcolor", "darkred");
            //emitParam(ps, "boxfgcolor", "white");
            //emitParam(ps, "progresscolor", "black");
            emitParam(ps, "cache_option", "Plugin");
            emitParam(ps, "hostname", hreq.getServerName());
            emitParam(ps, "username", login);
            emitParam(ps, "password", new String(pass));
            ps.println(APPLET_CLOSE);
            ps.println(PAGE_CLOSE);
        } finally {
            ps.close();
        }
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private void emitParam(PrintStream ps, String username, String password) {
        username = urlEncode(username);
        password = urlEncode(password);
        ps.print("<PARAM NAME=\"");
        ps.print(username);
        ps.print("\" VALUE=\"");
        ps.print(password);
        ps.print("\"/>");
        ps.println();
    }

    protected void doPost(HttpServletRequest hreq, HttpServletResponse hresp)
            throws ServletException, IOException
    {
        throw new ServletException("Not implemented");
    }
}
