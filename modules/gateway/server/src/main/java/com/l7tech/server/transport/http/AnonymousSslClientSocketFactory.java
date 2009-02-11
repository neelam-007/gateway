package com.l7tech.server.transport.http;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.util.logging.Logger;

/**
 * 
 */
public class AnonymousSslClientSocketFactory extends SslClientSocketFactorySupport {

    //- PUBLIC

    public synchronized static void setTrustManager( final X509TrustManager trustManager ) {
        logger.info("SSL Client TrustManager initialized.");
        if (trustManager == null) throw new NullPointerException();
        AnonymousSslClientSocketFactory.trustManager = trustManager;
    }

    /**
     * This is bizarre but it's Just The Way It Is(tm)
     */
    public static SSLSocketFactory getDefault() {
        return SingletonHolder.defaultInstance;
    }

    //- PROTECTED

    protected final X509TrustManager getTrustManager() {
        return trustManager;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AnonymousSslClientSocketFactory.class.getName());

    private static X509TrustManager trustManager;

    private static final class SingletonHolder {
        private static AnonymousSslClientSocketFactory defaultInstance = new AnonymousSslClientSocketFactory();
    }
}
