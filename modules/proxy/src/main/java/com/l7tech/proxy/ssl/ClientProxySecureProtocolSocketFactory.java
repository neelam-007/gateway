/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * New socket factory for SSL with the Jakarta Commons HTTP client.
 */
//TODO com.l7tech.proxy.util.SslUtilsTest.testPasswordChange() may failed because this class not implements SecureProtocolSocketFactory anymore
public class ClientProxySecureProtocolSocketFactory extends SSLSocketFactory {
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

    public Socket createSocket() throws IOException {
        return configSocket((SSLSocket) socketFactory().createSocket());
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException {
        return configSocket((SSLSocket) socketFactory().createSocket(socket, host, port, autoClose));
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException {
        return configSocket((SSLSocket) socketFactory().createSocket(host, port, clientHost, clientPort));
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException
    {
        return configSocket((SSLSocket) socketFactory().createSocket(inetAddress, i, inetAddress1, i1));
    }

    public Socket createSocket(String host, int port) throws IOException {
        return configSocket((SSLSocket) socketFactory().createSocket(host, port));
    }

    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return configSocket((SSLSocket) socketFactory().createSocket(inetAddress, i));
    }

    /**
     * New method in JDK8: Creates a server mode Socket layered over an existing connected socket, and is able to read
     * data which has already been consumed/removed from the Socket's underlying InputStream.
     * @param s Socket
     * @param consumed InputStream
     * @param autoClose boolean
     * @return  ssl socket
     * @throws IOException
     */
    @Override
    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException{
        return configSocket((SSLSocket) socketFactory().createSocket(s, consumed, autoClose));
    }

    private SSLSocket configSocket(SSLSocket s) {
        // TODO add configuration of enabled XVC cipher suites and TLS versions here
        //s.setEnabledCipherSuites(...);
        //s.setEnabledProtocols(...);
        s.setWantClientAuth(true);
        return s;
    }
}
