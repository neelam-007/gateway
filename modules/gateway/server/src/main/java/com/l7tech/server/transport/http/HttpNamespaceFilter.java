/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.http;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpNamespaceFilter implements Filter {
    public static final String PARAM_PASSTHROUGH_PREFIXES = "passthroughPrefixes";
    public static final String PARAM_ROUTERS = "soapRouters";
    public static final String PARAM_ROUTERSERVLET = "routerServlet";

    public static final String DEFAULT_PASSTHROUGH_PREFIXES = "/, /ssg, /classserver, /index.html";
    public static final String DEFAULT_ROUTERS = "/ssg/soap, /ssg/servlet/soap";
    public static final String DEFAULT_ROUTERSERVLET = "SoapMessageProcessingServlet";

    public void init( FilterConfig filterConfig ) throws ServletException {
        this.config = filterConfig;

        String pprefixes = filterConfig.getInitParameter(PARAM_PASSTHROUGH_PREFIXES);
        if ( pprefixes == null || pprefixes.length() == 0 ) pprefixes = DEFAULT_PASSTHROUGH_PREFIXES;
        String[] aprefixes = pprefixes.split(",\\s*");
        for ( int i = 0; i < aprefixes.length; i++ ) {
            prefixes.add(aprefixes[i]);
        }
        logger.info("Passthrough prefixes = '" + prefixes + "'");

        String prouters = filterConfig.getInitParameter(PARAM_ROUTERS);
        if ( prouters == null || prouters.length() == 0 ) prouters = DEFAULT_ROUTERS;
        String[] arouters = prouters.split(",\\s*");
        for ( int i = 0; i < arouters.length; i++ ) {
            routers.add(arouters[i]);
        }
        logger.info("Router mappings = '" + routers + "'");

        String pservlet = filterConfig.getInitParameter(PARAM_ROUTERSERVLET);
        if ( pservlet == null || pservlet.length() == 0 ) pservlet = DEFAULT_ROUTERSERVLET;
        routerServlet = pservlet;
        logger.info("Router servlet name = '" + routerServlet + "'");
    }

    public void doFilter( ServletRequest srequest,
                          ServletResponse sresponse,
                          FilterChain chain ) throws ServletException, IOException
    {
        HttpServletRequest request = (HttpServletRequest)srequest;
        HttpServletResponse response = (HttpServletResponse)sresponse;

        String uri = request.getRequestURI();

        boolean isRouter = routers.contains(uri);
        if ( !isRouter ) {
            for ( Iterator i = routers.iterator(); i.hasNext(); ) {
                String router = (String) i.next();
                isRouter = matches(router, uri);
            }
        }

        if ( !isRouter ) {
            for ( Iterator i = prefixes.iterator(); i.hasNext(); ) {
                String prefix = (String) i.next();
                if ( matches(prefix, uri) ) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }

        RequestDispatcher rd = config.getServletContext().getNamedDispatcher(routerServlet);
        rd.forward(request, response);
    }

    private boolean matches( String prefix, String candidate ) {
        if (candidate.equals(prefix)) return true;
        if (candidate.charAt(0) == '/' && candidate.substring(1).equals(prefix)) return true;
        if (!candidate.startsWith(prefix)) return false;
        final int plen = prefix.length();
        if (plen == 1) return false;
        switch (candidate.charAt(plen)) {
            case '/':
            case '?':
            case ';':
                return true;
            default:
                return false;
        }
    }

    public void destroy() {
    }

    private Set prefixes = new HashSet();
    private Set routers = new HashSet();
    private String routerServlet;

    private FilterConfig config;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
