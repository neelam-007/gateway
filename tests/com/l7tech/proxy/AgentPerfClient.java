/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.security.JceProvider;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author mike
 */
public class AgentPerfClient {

    private static class Logger { void info(String msg) { System.out.println(msg); } }
    private static Logger logger = new Logger();

    private static String postXml;
    private static Document postDocument;
    private static SecureSpanBridge ssb;

    /**
     * Initialize logging.  Attempts to mkdir ClientProxy.PROXY_CONFIG first, so the log file
     * will have somewhere to go.  Also calls JceProvider.init().
     */
    protected static void initLogging() {
        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

        // Prepare .l7tech directory before initializing logging (Bug #1288)
        new File(ClientProxy.PROXY_CONFIG).mkdirs(); // expected to fail on all but the very first execution

        JdkLoggerConfigurator.configure("com.l7tech.proxy", "com/l7tech/proxy/resources/logging.properties");
        JceProvider.init();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6)
            throw new IllegalArgumentException("Usage: AgentPerfClient gatewayHostname username password soapAction postFile iterationCount [preparseBoolean]");
        String gateway = args[0];
        String username = args[1];
        char[] password = args[2].toCharArray();
        String soapAction = args[3];
        String postfile = args[4];
        int iterationCount = Integer.parseInt(args[5]);
        boolean preparse = false;
        if (args.length > 6)
            preparse = Boolean.valueOf(args[6]).booleanValue();

        initLogging();

        postXml = new String(HexUtils.slurpStream(new FileInputStream(postfile)));
        if (preparse) {
            logger.info("Preparsing XML document");
            postDocument = XmlUtil.stringToDocument(postXml);
        }

        SecureSpanBridgeOptions options = new SecureSpanBridgeOptions(gateway, username, password);

        logger.info("Configuring test Agent to unconditionally trust the server certificate");
        options.setGatewayCertificateTrustManager(new SecureSpanBridgeOptions.GatewayCertificateTrustManager() {
            public boolean isGatewayCertificateTrusted(X509Certificate[] gatewayCertificateChain) {
                return true;
            }
        });

        logger.info("Configuring test Agent to avoid SSL on initial request");
        options.setUseSslByDefault(Boolean.FALSE);

        ssb = SecureSpanBridgeFactory.createSecureSpanBridge(options);

        // Do an untimed initial request
        logger.info("Doing initial request...");
        SecureSpanBridge.Result result = send(soapAction);
        logger.info("Initial result: response code " + result.getHttpStatus());

        int[] results = new int[1000];
        Arrays.fill(results, 0);

        logger.info("Timing " + iterationCount + " requests with concurrency 1...");
        long before = System.currentTimeMillis();
        for (int i = 0; i < iterationCount; ++i) {
            result = send(soapAction);
            results[result.getHttpStatus()]++; // allow IOOB exception for HTTP result code above 999
        }
        long after = System.currentTimeMillis();
        long total = after - before;

        logger.info("Total time " + total + " milliseconds: " + ((iterationCount * 1000d) / total) + " requests per sec");
        logger.info("Result codes:");
        for (int i = 0; i < results.length; ++i) {
            if (results[i] > 0)
                logger.info("  " + i + ": " + results[i]);
        }
    }

    private static SecureSpanBridge.Result send(String soapAction) throws SAXException, SecureSpanBridge.SendException, IOException, SecureSpanBridge.BadCredentialsException, SecureSpanBridge.CertificateAlreadyIssuedException {
        if (postDocument != null)
            return ssb.send(soapAction, postDocument);
        return ssb.send(soapAction, postXml);
    }
}
