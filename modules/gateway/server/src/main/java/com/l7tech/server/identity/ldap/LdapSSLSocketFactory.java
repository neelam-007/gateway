package com.l7tech.server.identity.ldap;

import com.l7tech.server.transport.http.SslClientSocketFactorySupport;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import java.util.Comparator;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 * SSLSocketFactory for use with LDAP connections.
 */
public class LdapSSLSocketFactory extends SslClientSocketFactorySupport implements Comparator {

    //- PUBLIC

    /**
     * This is bizarre but it's Just The Way It Is(tm)
     */
    public static synchronized SSLSocketFactory getDefault() {
        if ( instance == null ) {
            instance = new LdapSSLSocketFactory();
        }
        return instance;
    }

    //- PROTECTED

    @Override
    protected final X509TrustManager getTrustManager() {
        // method not invoked when buildSSLContext() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    protected final SSLContext buildSSLContext() {
        // method should be replaced by codegen
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Socket notifyCreated( final Socket socket,
                                          final String host,
                                          final InetAddress address,
                                          final int port,
                                          final InetAddress localAddress,
                                          final int localPort ) throws IOException {
        final HostnameVerifier verifier = getHostnameVerifier();
        if ( verifier != null && socket instanceof SSLSocket ) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            return doVerifyHostname( verifier, sslSocket, host, address );
        }

        return socket;
    }

    //- PRIVATE

    private static LdapSSLSocketFactory instance;
    private HostnameVerifier hostnameVerifier;

    private HostnameVerifier getHostnameVerifier() {
        HostnameVerifier verifier = hostnameVerifier;

        if ( verifier == null ) {
            verifier = hostnameVerifier = LdapSslCustomizerSupport.getHostnameVerifier();
        }

        return verifier;
    }
}
