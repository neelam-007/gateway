/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import com.l7tech.common.security.xml.TokenServiceClient;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;

/**
 * @author mike
 */
public class TokenServiceTest extends TestCase {
    private static Logger log = Logger.getLogger(TokenServiceTest.class.getName());

    public TokenServiceTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TokenServiceTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testTokenServiceClient() throws Exception {
        final TokenServiceClient tsc = new TokenServiceClient();
        Document requestMsg = tsc.createRequestSecurityTokenMessage(TestDocuments.getDotNetServerCertificate(),
                                                                    TestDocuments.getDotNetServerPrivateKey(),
                                                                    TokenServiceClient.TOKENTYPE_SECURITYCONTEXT);
        log.info("Decorated token request (reformatted): " + XmlUtil.nodeToFormattedString(requestMsg));
    }
}
