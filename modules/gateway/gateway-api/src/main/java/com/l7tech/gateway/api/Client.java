package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.ResourceTracker;
import com.sun.ws.management.transport.HttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
import java.net.Authenticator;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * 
 */
public class Client implements Closeable {

    //- PUBLIC

    public <MO extends ManagedObject> Accessor<MO> getAccessor( final Class<MO> managedObjectClass ) {
        return AccessorFactory.createAccessor( 
                managedObjectClass,
                url,
                tracker );
    }

    @Override
    public void close() throws IOException {
        tracker.close();
    }

    //- PACKAGE

    Client( final String url,
            final Authenticator authenticator,
            final HostnameVerifier hostnameVerifier,
            final X509TrustManager trustManager ) {
        this.url = url;

        if ( authenticator != null ) {
            HttpClient.setAuthenticator( authenticator );
        }

        if ( hostnameVerifier != null ) {
            HttpClient.setHostnameVerifier( hostnameVerifier );
        }

        if ( trustManager != null ) {
            try {
                HttpClient.setTrustManager( trustManager );
            } catch ( NoSuchAlgorithmException e ) {
                throw new ManagementRuntimeException( e );
            } catch ( KeyManagementException e ) {
                throw new ManagementRuntimeException( e );
            }
        }
    }

    //- PRIVATE

    private final ResourceTracker tracker = new ResourceTracker();
    private final String url;

}
