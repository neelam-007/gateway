/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import java.net.PasswordAuthentication;

/**
 * Manual test of changing password and revoking cert, using hardcoded account on phlox.
 */
public class SslUtilsTest extends TestCase {
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
        Protocol https = new Protocol("https", (ProtocolSocketFactory) ClientProxySecureProtocolSocketFactory.getInstance(), 443);
        Protocol.registerProtocol("https", https);

        String username="mike";
        char[] password="asdfasdf".toCharArray();
        char[] newpassword="qwerqwer".toCharArray();
        PasswordAuthentication pw = new PasswordAuthentication(username, password);

        Ssg ssg = new Ssg(1);
        ssg.setSsgAddress("phlox.l7tech.com");
        ssg.setUsername(username);
        ssg.getRuntime().setCachedPassword(password);

        ssg.getRuntime().getSsgKeyStoreManager().installSsgServerCertificate(ssg, pw);

        SslUtils.changePasswordAndRevokeClientCertificate(ssg,
                                                          username,
                                                          password,
                                                          newpassword);
    }
}
