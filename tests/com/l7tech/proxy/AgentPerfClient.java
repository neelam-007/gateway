/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import com.l7tech.common.util.HexUtils;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author mike
 */
public class AgentPerfClient {
    private static class Logger { void info(String msg) { System.out.println(msg); } }
    private static Logger logger = new Logger();

    public static void main(String[] args) throws Exception {
        if (args.length < 6)
            throw new IllegalArgumentException("Usage: AgentPerfClient gatewayHostname username password soapAction postFile iterationCount");
        String gateway = args[0];
        String username = args[1];
        char[] password = args[2].toCharArray();
        String soapAction = args[3];
        String postfile = args[4];
        int iterationCount = Integer.parseInt(args[5]);

        String postXml = new String(HexUtils.slurpStream(new FileInputStream(postfile)));

        SecureSpanBridgeOptions options = new SecureSpanBridgeOptions(gateway, username, password);

        logger.info("Configuring test Agent to unconditionally trust the server certificate");
        options.setGatewayCertificateTrustManager(new SecureSpanBridgeOptions.GatewayCertificateTrustManager() {
            public boolean isGatewayCertificateTrusted(X509Certificate[] gatewayCertificateChain) {
                return true;
            }
        });

        logger.info("Configuring test Agent to avoid SSL on initial request");
        options.setUseSslByDefault(Boolean.FALSE);

        SecureSpanBridge ssb = SecureSpanBridgeFactory.createSecureSpanBridge(options);

        // Do an untimed initial request
        logger.info("Doing initial request...");
        SecureSpanBridge.Result result = ssb.send(soapAction, postXml);
        logger.info("Initial result: response code " + result.getHttpStatus());

        int[] results = new int[1000];
        Arrays.fill(results, 0);

        logger.info("Timing " + iterationCount + " requests with concurrency 1...");
        long before = System.currentTimeMillis();
        for (int i = 0; i < iterationCount; ++i) {
            result = ssb.send(soapAction, postXml);
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
}
