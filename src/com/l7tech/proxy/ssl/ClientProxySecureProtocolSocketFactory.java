/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Category;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * New socket factory for SSL with the Jakarta Commons HTTP client.
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:48:21 PM
 */
public class ClientProxySecureProtocolSocketFactory implements SecureProtocolSocketFactory {
    private static final Category log = Category.getInstance(ClientProxySecureProtocolSocketFactory.class);
    private SSLContext sslContext;

    public ClientProxySecureProtocolSocketFactory(SSLContext ctx) {
        this.sslContext = ctx;
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException
    {
        final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        return sock;
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException, UnknownHostException
    {
        final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
        return sock;
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        final SSLSocket sock = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port);
        return sock;
    }
}
