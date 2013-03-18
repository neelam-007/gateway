package com.l7tech.server.identity.ldap;

import com.l7tech.server.transport.http.SslClientSocketFactorySupport;
import com.l7tech.util.NonObfuscatable;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Comparator;

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
    protected final Socket notifyCreated( Socket socket,
                                          final String host,
                                          final InetAddress address,
                                          final int port,
                                          final InetAddress localAddress,
                                          final int localPort ) throws IOException {
        socket = wrapSocket(socket);
        return socket;
    }

    //- PRIVATE

    @NonObfuscatable
    private static LdapSSLSocketFactory instance;
    @NonObfuscatable
    private HostnameVerifier hostnameVerifier;

    @NonObfuscatable
    private HostnameVerifier getHostnameVerifier() {
        HostnameVerifier verifier = hostnameVerifier;

        if ( verifier == null ) {
            verifier = hostnameVerifier = LdapSslCustomizerSupport.getHostnameVerifier();
        }

        return verifier;
    }

    /**
     * Wrap the socket in an ldap-specific SSL socket if possible.
     */
    private Socket wrapSocket(Socket socket) {
        if (socket instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            socket = new HostnameVerifyingSSLSocketWrapper(sslSocket,LdapSslCustomizerSupport.getHostnameVerifier());
        }
        return socket;
    }
}
