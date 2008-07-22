/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.http;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.Pair;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * {@link SSLSocketFactory} implementation used by {@link com.l7tech.server.identity.ldap.LdapIdentityProvider}s and
 * {@link com.l7tech.server.policy.assertion.alert.ServerEmailAlertAssertion} to provide a bridge between the bizarro
 * {@link SSLSocketFactory#getDefault} mechanism and our non-singleton {@link SslClientTrustManager}.
 * @author alex
 */
public final class SslClientSocketFactory extends SSLSocketFactory implements Comparator {
    private static final Logger logger = Logger.getLogger(SslClientSocketFactory.class.getName());
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

    private final SSLContext sslContext;

    private static X509TrustManager trustManager;
    private static KeyManager[] defaultKeyManagers;
    private static SsgKeyStoreManager ssgKeyStoreManager;

    private static final Map<Pair<Long, String>, SslClientSocketFactory> instancesByKeyEntryId = new HashMap<Pair<Long, String>, SslClientSocketFactory>();

    public synchronized static void setTrustManager(X509TrustManager trustManager) {
        logger.info("Got SSL Client TrustManager");
        if (trustManager == null) throw new NullPointerException();
        SslClientSocketFactory.trustManager = trustManager;
    }

    public synchronized static void setDefaultKeyManagers(KeyManager[] keyManagers) {
        SslClientSocketFactory.defaultKeyManagers = keyManagers;
    }

    public synchronized static void setSsgKeyStoreManager(SsgKeyStoreManager ssgKeyStoreManager) {
        SslClientSocketFactory.ssgKeyStoreManager = ssgKeyStoreManager;
    }

    private static class SingletonHolder {
        private static SslClientSocketFactory defaultInstance = new SslClientSocketFactory();
        private static SslClientSocketFactory anonymousInstance = new SslClientSocketFactory(null);
    }

    /**
     * This is bizarre but it's Just The Way It Is(tm)
     */
    public static SSLSocketFactory getDefault() {
        return SingletonHolder.defaultInstance;
    }

    public synchronized static SSLSocketFactory getInstance(final long keystoreId, final String alias)
        throws FindException, GeneralSecurityException
    {
        if (ssgKeyStoreManager == null) throw new IllegalStateException("SSG Keystore Manager must be set first");

        final Pair<Long, String> ssgKeyEntryId = new Pair<Long, String>(keystoreId, alias);
        // TODO read/write lock?
        SslClientSocketFactory instance = instancesByKeyEntryId.get(ssgKeyEntryId);
        if (instance == null) {
            SsgKeyEntry entry = ssgKeyStoreManager.lookupKeyByKeyAlias(alias, keystoreId);
            KeyManager km = new SingleCertX509KeyManager(entry.getCertificateChain(), entry.getPrivateKey());
            instance = new SslClientSocketFactory(km);
            instancesByKeyEntryId.put(ssgKeyEntryId, instance);
        }
        return instance;
    }

    public synchronized static SSLSocketFactory getAnonymous() {
        return SingletonHolder.anonymousInstance;
    }

    private SslClientSocketFactory() {
        logger.info("Initializing SSL Client Socket Factory");
        try {
            if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(defaultKeyManagers, new TrustManager[] { trustManager } , null);
            int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            context.getClientSessionContext().setSessionTimeout(timeout);
            this.sslContext = context;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Couldn't initialize LDAP client SSL context", e);
        }
    }

    /**
     * @param keyManager the KeyManager containing the keypair to use for client authentication, or null for an instance
     *        that will not support client authentication.
     */
    private SslClientSocketFactory(KeyManager keyManager) {
        if (keyManager == null) {
            logger.info("Initializing SSL Client Socket Factory without client authentication");
        } else {
            logger.info("Initializing SSL Client Socket Factory with Custom KeyManager");
        }

        try {
            if (trustManager == null) throw new IllegalStateException("TrustManager must be set before first use");
            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(keyManager == null ? null : new KeyManager[] { keyManager },
                         new TrustManager[] { trustManager } ,
                         null);
            int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT).intValue();
            context.getClientSessionContext().setSessionTimeout(timeout);
            this.sslContext = context;
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
