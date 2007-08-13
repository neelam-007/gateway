/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * {@link SSLSocketFactory} implementation used by {@link com.l7tech.server.identity.ldap.LdapIdentityProvider}s and
 * {@link com.l7tech.server.policy.assertion.alert.ServerEmailAlertAssertion} to provide a bridge between the bizarro
 * {@link SSLSocketFactory#getDefault} mechanism and our non-singleton {@link SslClientTrustManager}.
 * @author alex
 */
public class SslClientSocketFactory extends SSLSocketFactory implements Comparator {
    private static final Logger logger = Logger.getLogger(SslClientSocketFactory.class.getName());
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;
    private final SSLContext sslContext;
    private static X509TrustManager trustManager;

    public synchronized static void setTrustManager(X509TrustManager trustManager) {
        logger.info("Got SSL Client TrustManager");
        if (trustManager == null) throw new NullPointerException();
        SslClientSocketFactory.trustManager = trustManager;
    }

    private static class SingletonHolder {
        private static SslClientSocketFactory singleton = new SslClientSocketFactory();
    }

    /**
     * This is bizarre but it's Just The Way It Is(tm)
     */
    public static SSLSocketFactory getDefault() {
        return SingletonHolder.singleton;
    }

    private SslClientSocketFactory() {
        logger.info("Initializing LDAP client SSL context");
        try {
            if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { trustManager } , null);
            int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            sslContext.getClientSessionContext().setSessionTimeout(timeout);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Couldn't initialize LDAP client SSL context", e);
        }
    }

    /**
     * Check socket factories for equivalence.
     *
     * <p>This factory is the same as any other SslClientSocketFactory</p>
     *
     * <p>This method is used with connection pooling, if removed connections will not be pooled.</p>
     *
     * <p>NOTE: the actual objects passed are Strings ie. "com.l7tech.server.transport.http.SslClientSocketFactory" </p>
     */
    public int compare(Object o1, Object o2) {
        return o1!=null && o2!=null && o1.equals(o2) ? 0 : -1;
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
