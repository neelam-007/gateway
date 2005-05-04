/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

public class AgentFilter implements Filter {
    private static final Logger log = Logger.getLogger(AgentFilter.class.getName());
    private WssProcessor trogdor;
    private KeyStore keystore;
    private PrivateKey privateKey;
    private X509Certificate cert;

    public void init(FilterConfig filterConfig) throws ServletException {
        trogdor = new WssProcessorImpl();
        String keystorePath = filterConfig.getInitParameter("keystorePath");
        String keystorePass = filterConfig.getInitParameter("keystorePassword");
        String keystoreType = filterConfig.getInitParameter("keystoreType");
        String keystoreAlias = filterConfig.getInitParameter("keystoreAlias");
        String keystoreKeyPass = filterConfig.getInitParameter("keystoreKeyPassword");

        try {
            keystore = KeyStore.getInstance(keystoreType);
            FileInputStream fis = new FileInputStream(keystorePath);
            keystore.load(fis, keystorePass.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keystoreAlias, keystoreKeyPass.toCharArray());
            cert = (X509Certificate) keystore.getCertificate(keystoreAlias);
        } catch (Exception e) {
            throw new ServletException("Keystore initialization failed", e);
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest;
        HttpServletResponse httpResponse;

        if (servletRequest instanceof HttpServletRequest) {
            httpRequest = (HttpServletRequest) servletRequest;
        } else throw new ServletException("Only HTTP requests supported!");

        if (servletResponse instanceof HttpServletResponse) {
            httpResponse = (HttpServletResponse) servletResponse;
        } else throw new ServletException("Only HTTP requests supported!");
    }

    public void destroy() {
    }
}
