/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A version of the Layer 7 SecureSpan Bridge that runs as a servlet.
 *
 */
public class BridgeServlet extends HttpServlet {
    private SecureSpanBridge secureSpanBridge;

    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException
    {
        super.doGet(servletRequest, servletResponse);
        // TODO make WSDL proxy work
    }

    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException
    {
        String soapAction = servletRequest.getHeader("SOAPAction");
        Document message = null;
        try {
            message = XmlUtil.parse(servletRequest.getInputStream());
            SecureSpanBridge.Result result = secureSpanBridge.send(soapAction, message);
            servletResponse.setStatus(result.getHttpStatus());
            servletResponse.setContentType("text/xml");
            final ServletOutputStream outputStream = servletResponse.getOutputStream();
            XmlUtil.nodeToOutputStream(result.getResponse(), outputStream);
            servletResponse.getOutputStream().close();
        } catch (SAXException e) {
            error(servletRequest, servletResponse, "Request was not well-formed XML", e);
        } catch (SecureSpanBridge.BadCredentialsException e) {
            error(servletRequest, servletResponse, "Bad credentials in config", e);
        } catch (SecureSpanBridge.CertificateAlreadyIssuedException e) {
            error(servletRequest, servletResponse, "Certificate already issued", e);
        } catch (SecureSpanBridge.SendException e) {
            error(servletRequest, servletResponse, "Unable to forward request to server", e);
        }
    }

    private void error(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String s, Throwable t) throws IOException {
        servletResponse.setStatus(500);
        servletResponse.setContentType("text/plain");
        servletResponse.getOutputStream().write((s + "\n").getBytes());
        servletResponse.getOutputStream().close();
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        String gatewayHostname = servletConfig.getInitParameter("gatewayHostname");
        char[] password = servletConfig.getInitParameter("password").toCharArray();
        String username = servletConfig.getInitParameter("username");
        SecureSpanBridgeOptions opt = new SecureSpanBridgeOptions(gatewayHostname, username, password);
        opt.setUseSslByDefault(Boolean.valueOf(servletConfig.getInitParameter("useSslByDefault")));

        int ssgId = 1;
        try {
            ssgId = Integer.valueOf(servletConfig.getInitParameter("ssgId")).intValue();
        } catch (NumberFormatException nfe) {
            // leave at default
        }

        opt.setId(ssgId);
        String keyStorePath = servletConfig.getInitParameter("keyStorePath");
        if (keyStorePath != null)
            opt.setKeyStorePath(keyStorePath);
        String certStorePath = servletConfig.getInitParameter("certStorePath");
        if (certStorePath != null)
            opt.setCertStorePath(certStorePath);

        opt.setGatewayCertificateTrustManager(new SecureSpanBridgeOptions.GatewayCertificateTrustManager() {
            public boolean isGatewayCertificateTrusted(X509Certificate[] gatewayCertificateChain) throws CertificateException {
                // TODO The world is full of happy elves and cute bunnies
                return true;
            }
        });

        secureSpanBridge = SecureSpanBridgeFactory.createSecureSpanBridge(opt);
    }
}
