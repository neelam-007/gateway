/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.admin;

import com.l7tech.server.KeystoreUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.User;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.cert.CertificateException;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;

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
                    "        width=\"100%\" height=\"100%\" mayscript>\n";

    private static final String APPLET_CLOSE =
            "\n" +
                    "Java runtime 5.0 or higher is required.  You can <a href=\"http://www.java.com/getjava/\">download it for free</a>.\n" +
                    "</applet>";

    private WebApplicationContext applicationContext;
    private KeystoreUtils keystoreUtils;
    private AdminSessionManager adminSessionManager;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        keystoreUtils = (KeystoreUtils) getBean("keystore", null);
        adminSessionManager = (AdminSessionManager) getBean("adminSessionManager", AdminSessionManager.class);
    }

    private WebApplicationContext getContext() throws ServletException {
        if (applicationContext != null) return applicationContext;
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null)
            throw new ServletException("Configuration error; could not get application context");
        return applicationContext;
    }

    private Object getBean(String name, Class clazz) throws ServletException {
        Object got = clazz == null ? getContext().getBean(name) : getContext().getBean(name, clazz);
        if (got == null)
            throw new ServletException("Configuration error; could not get " + name);
        return got;
    }

    protected void doGet(HttpServletRequest hreq, HttpServletResponse hresp)
            throws ServletException, IOException
    {
        Object userObj = hreq.getAttribute(ManagerAppletFilter.PROP_USER);
        if (!(userObj instanceof User))
            throw new ServletException("ManagerAppletServlet: request was not authenticated"); // shouldn't be possible

        // Establish a new admin session for the authenticated user
        String sessionId = adminSessionManager.createSession((User)userObj);
        Cookie sessionCookie = new Cookie(ManagerAppletFilter.SESSION_ID_COOKIE_NAME, sessionId);
        sessionCookie.setSecure(true);

        hresp.setContentType("text/html");
        hresp.setStatus(200);
        hresp.addCookie(sessionCookie);
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
            emitParam(ps, "sessionId", sessionId);
            emitServerCertParam(ps);
            ps.println(APPLET_CLOSE);
            ps.println(PAGE_CLOSE);
        } finally {
            ps.close();
        }
    }

    private void emitServerCertParam(PrintStream ps) throws IOException {
        try {
            emitParam(ps, "gatewayCert", HexUtils.encodeBase64(keystoreUtils.getSslCert().getEncoded(), true));
        } catch (CertificateException e) {
            // Leave it out
            logger.log(Level.WARNING, "Unable to provide gatewayCert to applet: " + ExceptionUtils.getMessage(e), e);
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
