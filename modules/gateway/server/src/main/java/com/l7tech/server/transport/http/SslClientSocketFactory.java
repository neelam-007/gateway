/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.http;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.logging.Logger;
import java.util.Comparator;

/**
 * {@link SSLSocketFactory} implementation used by {@link com.l7tech.server.identity.ldap.LdapIdentityProvider}s
 *  to provide a bridge between the bizarro
 * {@link SSLSocketFactory#getDefault} mechanism and our non-singleton {@link SslClientTrustManager}.
 * @author alex
 */
public final class SslClientSocketFactory extends SslClientSocketFactorySupport implements Comparator {

    //- PUBLIC

    public synchronized static void setTrustManager( final X509TrustManager trustManager ) {
        logger.info("SSL Client TrustManager initialized.");
        if (trustManager == null) throw new NullPointerException();
        SslClientSocketFactory.trustManager = trustManager;
    }

    public synchronized static void setDefaultKeyManagers( final KeyManager[] keyManagers ) {
        logger.info("SSL Client KeyManager initialized.");
        SslClientSocketFactory.defaultKeyManagers = keyManagers;
    }

    /**
     * This is bizarre but it's Just The Way It Is(tm)
     */
    public static SSLSocketFactory getDefault() {
        return SingletonHolder.defaultInstance;
    }

    //- PROTECTED

    @Override
    protected final X509TrustManager getTrustManager() {
        return trustManager;
    }

    @Override
    protected final KeyManager[] getDefaultKeyManagers() {
        return defaultKeyManagers;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SslClientSocketFactory.class.getName());

    private static X509TrustManager trustManager;
    private static KeyManager[] defaultKeyManagers;

    private static final class SingletonHolder {
        private static SslClientSocketFactory defaultInstance = new SslClientSocketFactory();
    }
}
