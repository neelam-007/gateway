/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * New socket factory for SSL with the Jakarta Commons HTTP client.
 */
public class ClientProxySecureProtocolSocketFactory extends SSLSocketFactory implements SecureProtocolSocketFactory {
    private final SSLSocketFactory defaultSslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();

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

    private static SSLSocketFactory socketFactory() {
        SslContextHaver peer = CurrentSslPeer.get();
        if (peer == null)
            throw new IllegalStateException("Unable to create SSL client socket: No SSL peer is available in this thread");
        return peer.getSslContext().getSocketFactory();
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException
    {
        return (SSLSocket) socketFactory().createSocket(socket, host, port, autoClose);
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException, UnknownHostException
    {
        return (SSLSocket) socketFactory().createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException
    {
        return (SSLSocket) socketFactory().createSocket(inetAddress, i, inetAddress1, i1);
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return (SSLSocket) socketFactory().createSocket(host, port);
    }

    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return (SSLSocket) socketFactory().createSocket(inetAddress, i);
    }
}
