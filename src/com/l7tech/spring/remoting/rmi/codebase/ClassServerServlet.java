/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.spring.remoting.rmi.codebase;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.Regex;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * The servlet that works as a class server.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ClassServerServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ClassServerServlet.class.getName());
    private static final String DEFAULT_WHITELIST = "defaultWhitelist.dat";
    public static final String PARAM_WHITELIST = "WhitelistFile";

    private Pattern[] uriMatchers = null;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String whitelist = config.getInitParameter(PARAM_WHITELIST);
        if (whitelist == null || whitelist.trim().length() < 1)
            whitelist = DEFAULT_WHITELIST;

        List patternList = new ArrayList();
        InputStream is = getClass().getClassLoader().getResourceAsStream(whitelist);
        try {
            if (is == null)
                is = new FileInputStream(whitelist);
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() < 1 || trimmed.startsWith("#"))
                    continue;
                try {
                    Pattern pattern = Pattern.compile(trimmed);
                    patternList.add(pattern);
                } catch (PatternSyntaxException e) {
                    throw new ServletException("Class server disabled: syntax error in URI whitelist: " + ExceptionUtils.getMessage(e), e);
                }
            }
            uriMatchers = (Pattern[])patternList.toArray(new Pattern[0]);
        } catch (IOException e) {
            throw new ServletException("Class server disabled: unable to read URI whitelist: " + ExceptionUtils.getMessage(e), e);
        } finally {
            if (is != null) //noinspection EmptyCatchBlock
                try { is.close(); } catch (IOException e) {}
        }
    }


    public void service(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        final String requestURI = request.getRequestURI();
        logger.fine("The request URI is " + requestURI);

        if (uriMatchers == null || uriMatchers.length < 1)
            throw new ServletException("Class server disabled: no URI whitelist has been loaded");

        if (!requestURI.startsWith(URI_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean approved = false;
        for (int i = 0; i < uriMatchers.length; i++) {
            Pattern uriMatcher = uriMatchers[i];
            if (uriMatcher.matcher(requestURI).matches()) {
                approved = true;
                break;
            }
        }

        if (!approved) {
            logger.severe("Class Server blocked attempt to access non-whitelisted URI: " + requestURI);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        byte[] bytes;
        InputStream in = null;
        try {
            String resource = null;
            for (int i = 0; i < 2; i++) {
                resource = requestURI.substring(URI_PREFIX.length() - i);
                logger.fine("Lookup for the resource '" + resource + "'");
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                in = cl.getResourceAsStream(resource);
                if (in != null) {
                    break;
                }
            }

            if (null == in) {
                logger.fine("Unable to locate the resource '" + resource + "'");
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
