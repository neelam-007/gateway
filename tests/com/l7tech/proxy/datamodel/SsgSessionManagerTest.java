/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.common.security.xml.Session;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.PasswordAuthentication;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Test ability to establish session.  Requires access to a running SSG w/ a session manager.
 * User: mike
 * Date: Aug 28, 2003
 * Time: 2:45:43 PM
 */
public class SsgSessionManagerTest extends TestCase {
    private static Logger log = Logger.getLogger(SsgSessionManagerTest.class.getName());
    private static final String DEFAULT_SSG_BASE_URL = "https://sybok:8443";  // *** CONFIGURE ME ***
    private static final String TEST_USERNAME = "mike";
    private static final char[] TEST_PASSWORD = "asdfasdf".toCharArray();
    private static URL ssgBaseUrl = null;

    private static URL getSsgUrl() {
        if (ssgBaseUrl == null)
            try {
                ssgBaseUrl = new URL(DEFAULT_SSG_BASE_URL);
                log.info("Using SSG url: " + DEFAULT_SSG_BASE_URL);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e); // can't happen
            }
        return ssgBaseUrl;
    }

    private void configureSslToTrustEveryone() throws KeyManagementException, NoSuchAlgorithmException
    {
        log.warning("Configuring jakarta socket factory to use SSL context that trusts everyone");
        KeyManager km = new X509KeyManager() {
            public PrivateKey getPrivateKey(String s) {
                return null;
            }

            public X509Certificate[] getCertificateChain(String s) {
                return new X509Certificate[0];
            }

            public String[] getClientAliases(String s, Principal[] principals) {
                return new String[0];
            }

            public String[] getServerAliases(String s, Principal[] principals) {
                return new String[0];
            }

            public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                return null;
            }

            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                return null;
            }
        };

        TrustManager tm = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }
        };

        // Set up SSL context
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(new KeyManager[] { km }, new TrustManager[] { tm }, null);

        SecureProtocolSocketFactory socketFactory = new SecureProtocolSocketFactory() {
            public Socket createSocket(
                    Socket socket,
                    String host,
                    int port,
                    boolean autoClose
                    ) throws IOException, UnknownHostException
            {
                return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
            }

            public Socket createSocket(
                    String host,
                    int port,
                    InetAddress clientHost,
                    int clientPort
                    ) throws IOException, UnknownHostException
            {
                return sslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
            }

            public Socket createSocket(
                    String host,
                    int port
                    ) throws IOException, UnknownHostException
            {
                return sslContext.getSocketFactory().createSocket(host, port);
            }
        };

        Protocol https = new Protocol("https", socketFactory, 443);
        Protocol.registerProtocol("https", https);
    }

    public SsgSessionManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SsgSessionManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testGetSession() throws Exception {
        // Disable trust manager for the test
        configureSslToTrustEveryone();

        Managers.setCredentialManager(new CredentialManagerAdapter() {
            public PasswordAuthentication getCredentials(Ssg ssg) throws OperationCanceledException {
                log.info("Using SSG test username: " + TEST_USERNAME);
                ssg.setUsername(TEST_USERNAME);
                log.info("Using SSG test password: " + TEST_PASSWORD);
                ssg.cmPassword(TEST_PASSWORD);
                return new PasswordAuthentication(ssg.getUsername(), ssg.cmPassword());
            }

            public PasswordAuthentication getNewCredentials(Ssg ssg) throws OperationCanceledException {
                throw new OperationCanceledException("Old credentials failed, and we have no others to try");
            }
        });

        Ssg ssg = new Ssg();
        ssg.setSsgAddress(getSsgUrl().getHost());
        ssg.setSslPort(getSsgUrl().getPort());

        Session session = SsgSessionManager.getOrCreateSession(ssg, "123");

        assertTrue(session != null);
        assertTrue(session.getId() != 0);
        assertTrue(session.getCreationTimestamp() > 0);
        assertTrue(session.getKeyReq() != null);
        assertTrue(session.getKeyRes() != null);

        assertTrue(ssg.session() != null);
        assertTrue(ssg.session() == session);

        log.info("Succesfully established session.  id = " + session.getId());
    }
}
