/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.common.protocol.SecureSpanConstants;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * @author mike
 */
public class SslUtilsTest extends TestCase {
    private static Logger log = Logger.getLogger(SslUtilsTest.class.getName());

    public SslUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SslUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testPasswordChange() throws Exception {
        // Configure SSL for outgoing connections
        System.setProperty("httpclient.useragent", SecureSpanConstants.USER_AGENT);
        Protocol https = new Protocol("https", ClientProxySecureProtocolSocketFactory.getInstance(), 443);
        Protocol.registerProtocol("https", https);

        String username="mike";
        char[] password="asdfasdf".toCharArray();
        char[] newpassword="qwerqwer".toCharArray();
        PasswordAuthentication pw = new PasswordAuthentication(username, password);

        Ssg ssg = new Ssg(1);
        ssg.setSsgAddress("phlox.l7tech.com");
        ssg.setUsername(username);
        ssg.cmPassword(password);
        CurrentRequest.setCurrentSsg(ssg);

        SsgKeyStoreManager.installSsgServerCertificate(ssg, pw);

        SslUtils.changePasswordAndRevokeClientCertificate(ssg.getServerPasswordChangeUrl(),
                                                          "mike",
                                                          "asdfasdf".toCharArray(),
                                                          "qwerqwer".toCharArray());
    }
}
