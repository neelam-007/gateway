package com.l7tech.server.transport.http;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import java.util.Comparator;
import java.util.logging.Logger;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 *
 */
public class SslClientHostnameAwareSocketFactory extends SslClientSocketFactorySupport implements Comparator {

    //- PUBLIC

    public synchronized static void setTrustManager( final X509TrustManager trustManager ) {
        logger.info("SSL Client TrustManager initialized.");
        if (trustManager == null) throw new NullPointerException();
        SslClientHostnameAwareSocketFactory.trustManager = trustManager;
    }

    public synchronized static void setDefaultKeyManagers( final KeyManager[] keyManagers ) {
        logger.info("SSL Client KeyManager initialized.");
        SslClientHostnameAwareSocketFactory.defaultKeyManagers = keyManagers;
    }

    public synchronized static void setHostnameVerifier( final HostnameVerifier hostnameVerifier ) {
        logger.info("SSL Client HostnameVerifier initialized.");
        SslClientHostnameAwareSocketFactory.hostnameVerifier = hostnameVerifier;
    }

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

    @Override
    protected final Socket notifyCreated( final Socket socket,
                                          final String host,
                                          final InetAddress address,
                                          final int port,
                                          final InetAddress localAddress,
                                          final int localPort ) throws IOException {
        final HostnameVerifier verifier = hostnameVerifier;
        if ( verifier != null && socket instanceof SSLSocket ) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            return doVerifyHostname( verifier, sslSocket, host, address );
        }

        return socket;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SslClientSocketFactory.class.getName());

    private static X509TrustManager trustManager;
    private static KeyManager[] defaultKeyManagers;
    private static HostnameVerifier hostnameVerifier;

    private static final class SingletonHolder {
        private static SslClientHostnameAwareSocketFactory defaultInstance = new SslClientHostnameAwareSocketFactory();
    }
}
