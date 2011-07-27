package com.l7tech.gateway.api;

import com.l7tech.gateway.api.Accessor.AccessorException;
import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.ResourceTracker;
import com.l7tech.gateway.api.impl.TransportFactory;
import com.sun.ws.management.client.ResourceFactory;
import com.sun.ws.management.client.impl.TransportClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.PasswordAuthentication;

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
                resourceFactory,
                tracker );
    }

    /**
     * Get the accessor for the given type.
     *
     * @param accessibleObjectClass The class of accessible object
     * @param accessorClass The accessor class
     * @param <AO> The accessible object type
     * @param <AT> The type of the accessor class
     * @return The accessor for the type
     * @throws AccessorException If the accessor class is not compatible with the accessible object
     */
    public <AO extends AccessibleObject, AT extends Accessor<AO>> AT getAccessor( final Class<AO> accessibleObjectClass,
                                                                                  final Class<AT> accessorClass ) throws AccessorException {
        final Accessor<AO> accessor = getAccessor( accessibleObjectClass );

        if ( !accessorClass.isInstance( accessor ) ) {
            throw new AccessorException("Accessor of type " + accessorClass + " is not supported for class " + accessibleObjectClass);
        }

        return accessorClass.cast( accessor );
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
            final int connectTimeout,
            final int readTimeout,
            final PasswordAuthentication passwordAuthentication,
            final HostnameVerifier hostnameVerifier,
            final SSLSocketFactory sslSocketFactory ) {
        this.url = url;

        final TransportClient client = TransportFactory.newTransportClient(
                connectTimeout,
                readTimeout,
                passwordAuthentication,
                hostnameVerifier,
                sslSocketFactory );
        resourceFactory.setTransportClient( client );
    }

    //- PRIVATE

    private final ResourceTracker tracker = new ResourceTracker();
    private final ResourceFactory resourceFactory = ResourceFactory.newInstance();
    private final String url;

}
