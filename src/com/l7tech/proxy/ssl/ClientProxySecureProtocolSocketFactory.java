/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.Ssg;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 * New socket factory for SSL with the Jakarta Commons HTTP client.
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:48:21 PM
 */
public class ClientProxySecureProtocolSocketFactory extends SSLSocketFactory implements SecureProtocolSocketFactory {
    private static final Logger log = Logger.getLogger(ClientProxySecureProtocolSocketFactory.class.getName());
    private static final SSLSocketFactory defaultSslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();

    private static class InstanceHolder {
        private static final ClientProxySecureProtocolSocketFactory INSTANCE = new ClientProxySecureProtocolSocketFactory();
    }

    private ClientProxySecureProtocolSocketFactory() {}

    public String[] getDefaultCipherSuites() {
        return defaultSslSocketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return defaultSslSocketFactory.getSupportedCipherSuites();
    }

    public static ClientProxySecureProtocolSocketFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException
    {
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("Unable to create SSL client socket: No peer Gateway is available in this thread");
        final SSLSocket sock = (SSLSocket) ssg.sslContext().getSocketFactory().createSocket(socket, host, port, autoClose);
        return sock;
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException, UnknownHostException
    {
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("Unable to create SSL client socket: No peer Gateway is available in this thread");
        final SSLSocket sock = (SSLSocket) ssg.sslContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
        return sock;
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("Unable to create SSL client socket: No peer Gateway is available in this thread");
        final SSLSocket sock = (SSLSocket) ssg.sslContext().getSocketFactory().createSocket(inetAddress,
                                                                                            i,
                                                                                            inetAddress1,
                                                                                            i1);
        return sock;
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("Unable to create SSL client socket: No peer Gateway is available in this thread");
        final SSLSocket sock = (SSLSocket) ssg.sslContext().getSocketFactory().createSocket(host, port);
        return sock;
    }

    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("Unable to create SSL client socket: No peer Gateway is available in this thread");
        final SSLSocket sock = (SSLSocket) ssg.sslContext().getSocketFactory().createSocket(inetAddress, i);
        return sock;
    }
}
