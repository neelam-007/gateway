/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Category;
import sun.security.x509.X500Name;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * New socket factory for SSL with the Jakarta Commons HTTP client.
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:48:21 PM
 */
public class ClientProxySecureProtocolSocketFactory implements SecureProtocolSocketFactory {
    private static final Category log = Category.getInstance(ClientProxySecureProtocolSocketFactory.class);
    private SSLContext sslContext;

    /* My own hostname verifier goes here */
    private static class MyHandshakeCompletedListener implements HandshakeCompletedListener {
        private String expectedHostname;

        MyHandshakeCompletedListener(String expectedHostname) {
            this.expectedHostname = expectedHostname;
        }

        public void handshakeCompleted(HandshakeCompletedEvent event) {
            log.info("MyHandshakeCompletedListner: connection was made to " + expectedHostname);
            try {
                Certificate[] certs = event.getPeerCertificates();
                if (certs.length < 1)
                    throw new RuntimeException("Server presented no server certificates");
                /*if (certs.length > 1)
                    throw new RuntimeException("Server presented more than one certificate");*/
                if (!(certs[0] instanceof X509Certificate))
                    throw new RuntimeException("Server certificate was in the wrong format");
                X509Certificate cert = (X509Certificate) certs[0];
                String cn = null;
                try {
                    cn = new X500Name(cert.getSubjectX500Principal().toString()).getCommonName();
                } catch (IOException e) {
                    log.error(e);
                    // can't happen
                }
                if (!cn.equals(expectedHostname))
                    throw new HostnameMismatchException("Server certificate name (" + cn +
                                                        ") did not match the hostname we connected to (" +
                                                        expectedHostname + ")");
                log.info("Server hostname verified successfully");
            } catch (SSLPeerUnverifiedException e) {
                throw new RuntimeException("Server certificate was not verified");
            }
        }
    }

    public ClientProxySecureProtocolSocketFactory(SSLContext ctx) {
        this.sslContext = ctx;
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException
    {
        final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        sock.addHandshakeCompletedListener(new MyHandshakeCompletedListener(host));
        return sock;
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException, UnknownHostException
    {
        final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
        sock.addHandshakeCompletedListener(new MyHandshakeCompletedListener(host));
        return sock;
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port);
        sock.addHandshakeCompletedListener(new MyHandshakeCompletedListener(host));
        return sock;
    }
}
