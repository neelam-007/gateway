/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.ldap;

import com.l7tech.server.transport.http.SslClientTrustManager;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class LdapClientSslSocketFactory extends SSLSocketFactory {
    private static final Logger logger = Logger.getLogger(LdapClientSslSocketFactory.class.getName());
    private final SSLContext sslContext;
    private static SslClientTrustManager trustManager;

    synchronized static void setTrustManager(SslClientTrustManager trustManager) {
        logger.info("Got SSL Client TrustManager");
        if (trustManager == null) throw new NullPointerException();
        LdapClientSslSocketFactory.trustManager = trustManager;
    }

    private static class SingletonHolder {
        private static LdapClientSslSocketFactory singleton = new LdapClientSslSocketFactory();
    }

    /**
     * This bizarre thing is just the way JNDI does it
     */
    public static SocketFactory getDefault() {
        return SingletonHolder.singleton;
    }

    private LdapClientSslSocketFactory() {
        logger.info("Initializing LDAP client SSL context");
        try {
            if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { trustManager } , null);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Couldn't initialize LDAP client SSL context", e);
        }
    }

    public String[] getDefaultCipherSuites() {
        return sslContext.getSocketFactory().getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return sslContext.getSocketFactory().getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }

    public Socket createSocket(Socket socket, String string, int i, boolean b) throws IOException {
        return sslContext.getSocketFactory().createSocket(socket, string, i, b);
    }

    public Socket createSocket(String string, int i) throws IOException {
        return sslContext.getSocketFactory().createSocket(string, i);
    }

    public Socket createSocket(String string, int i, InetAddress inetAddress, int i1) throws IOException {
        return sslContext.getSocketFactory().createSocket(string, i, inetAddress, i1);
    }

    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return sslContext.getSocketFactory().createSocket(inetAddress, i);
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return sslContext.getSocketFactory().createSocket(inetAddress, i, inetAddress1, i1);
    }
}
