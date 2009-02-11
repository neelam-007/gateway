package com.l7tech.server.identity.ldap;

import com.l7tech.server.transport.http.SslClientSocketFactorySupport;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
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
    protected X509TrustManager getTrustManager() {
        // method not invoked when buildSSLContext() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    protected SSLContext buildSSLContext() {
        // method should be replaced by codegen
        throw new UnsupportedOperationException();
    }

    //- PRIVATE

    private static LdapSSLSocketFactory instance;    

}
