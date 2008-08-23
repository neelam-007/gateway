package com.l7tech.server.transport.http;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.util.Comparator;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 * This factor creates a GLOBAL instance of an SSL client socket factory.
 *
 * <p>All instances of any subclass MUST be equivalent (for connection pooling purposes).</p>
 *
 * <p>DO NOT modify this class to add (for example) client certificate selection.</p>
 */
abstract class SslClientSocketFactorySupport extends SSLSocketFactory implements Comparator {

    //- PUBLIC

    /**
     * Check socket factories for equivalence.
     *
     * <p>This factory is the same as any other SslClientSocketFactory</p>
     *
     * <p>This method is used with connection pooling, if removed connections will not be pooled.</p>
     *
     * <p>NOTE: the actual objects passed are Strings ie. "com.l7tech.server.transport.http.SslClientSocketFactory" </p>
     */
    public final int compare(Object o1, Object o2) {
        return o1!=null && o2!=null && o1.equals(o2) ? 0 : -1;
    }

    public final String[] getDefaultCipherSuites() {
        return getSocketFactory().getDefaultCipherSuites();
    }

    public final String[] getSupportedCipherSuites() {
        return getSocketFactory().getSupportedCipherSuites();
    }

    @Override
    public final Socket createSocket() throws IOException {
        return getSocketFactory().createSocket();
    }

    public final Socket createSocket(Socket socket, String string, int i, boolean b) throws IOException {
        return getSocketFactory().createSocket(socket, string, i, b);
    }

    public final Socket createSocket(String string, int i) throws IOException {
        return getSocketFactory().createSocket(string, i);
    }

    public final Socket createSocket(String string, int i, InetAddress inetAddress, int i1) throws IOException {
        return getSocketFactory().createSocket(string, i, inetAddress, i1);
    }

    public final Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return getSocketFactory().createSocket(inetAddress, i);
    }

    public final Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return getSocketFactory().createSocket(inetAddress, i, inetAddress1, i1);
    }

    //- PACKAGE

    abstract X509TrustManager getTrustManager();

    KeyManager[] getDefaultKeyManagers() {
        return new KeyManager[0];
    }

    SslClientSocketFactorySupport() {
        logger.info("Initializing SSL Client Socket Factory");

        KeyManager[] keyManagers = getDefaultKeyManagers();
        if ( keyManagers == null )
            throw new IllegalStateException("KeyManagers must be set before first use");

        X509TrustManager trustManager = getTrustManager();
        if ( trustManager == null )
            throw new IllegalStateException("TrustManager must be set before first use");

        int timeout = Integer.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT);

        SSLContext context;
        try {
            context = SSLContext.getInstance("SSL");
            context.init( keyManagers, new TrustManager[]{ trustManager }, null );
            context.getClientSessionContext().setSessionTimeout(timeout);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Couldn't initialize LDAP client SSL context", e);
        }

        this.sslContext = context;
    }

    //- PRIVATE

    /**
     * This name is used for backwards compatibility, do not change.
     */
    private static final String PROP_SSL_SESSION_TIMEOUT = SslClientSocketFactory.class.getName() + ".sslSessionTimeoutSeconds";
    private static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final SSLContext sslContext;

    private SSLSocketFactory getSocketFactory() {
        return sslContext.getSocketFactory();
    }
}
