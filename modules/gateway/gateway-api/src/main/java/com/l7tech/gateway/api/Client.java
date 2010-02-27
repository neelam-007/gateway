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
 * Client for access to managed objects for the target SecureSpan Gateway.
 *
 * <p>The client must be closed after use to free up any underlying resources.</p>
 *
 * @see ClientFactory#createClient(String)
 */
public class Client implements Closeable {

    //- PUBLIC

    /**
     * Get the accessor for the given type.
     *
     * @param accessibleObjectClass The type of accessible object
     * @return The accessor for the type
     */
    public <AO extends AccessibleObject> Accessor<AO> getAccessor( final Class<AO> accessibleObjectClass ) {
        return AccessorFactory.createAccessor( 
                accessibleObjectClass,
                url,
                tracker );
    }

    /**
     * Close the client.
     *
     * <p>The client should not be used after it has been closed.</p>
     *
     * @throws IOException If an error occurs
     */
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
