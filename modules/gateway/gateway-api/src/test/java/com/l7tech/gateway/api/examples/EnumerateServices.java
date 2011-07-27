package com.l7tech.gateway.api.examples;

import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.Accessor.AccessorException;
import com.l7tech.gateway.api.Client;
import com.l7tech.gateway.api.ClientFactory;
import com.l7tech.gateway.api.ClientFactory.InvalidOptionException;
import com.l7tech.gateway.api.ManagementRuntimeException;
import com.l7tech.gateway.api.ServiceMO;

import java.util.Iterator;

/**
 * Gateway Management API example for Service enumeration.
 */
public class EnumerateServices {

    /**
     * The URL of the Gateway Management Service to connect to.
     */
    private static final String GATEWAY_URL = "https://localhost:8443/wsman";

    /**
     * The credentials for an administrative user with permission to read one
     * or more services.
     */
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";

    /**
     * Application that outputs the identifier and name of each published service.
     *
     * @args The applications command line arguments
     */
    public static void main( final String[] args ) {
        try {
            final ClientFactory factory = ClientFactory.newInstance();

            factory.setAttribute( ClientFactory.ATTRIBUTE_USERNAME, USERNAME );
            factory.setAttribute( ClientFactory.ATTRIBUTE_PASSWORD, PASSWORD );

            // For the example we disable certificate and hostname validation,
            // you would not do this in production
            factory.setFeature( ClientFactory.FEATURE_CERTIFICATE_VALIDATION, false );
            factory.setFeature( ClientFactory.FEATURE_HOSTNAME_VALIDATION, false );

            final Client client = factory.createClient( GATEWAY_URL );
            final Accessor<ServiceMO> serviceAccessor = client.getAccessor( ServiceMO.class );
            final Iterator<ServiceMO> serviceIterator = serviceAccessor.enumerate();
            while ( serviceIterator.hasNext() ) {
                final ServiceMO service = serviceIterator.next();
                System.out.println( "Id: " + service.getId() + ", Name: " + service.getServiceDetail().getName() );
            }

        } catch ( InvalidOptionException e ) {
            // Handle error due to an incorrect feature or attribute
            e.printStackTrace();
        } catch ( AccessorException e ) {
            // Handle "business logic" errors
            e.printStackTrace();
        } catch ( ManagementRuntimeException e ) {
            // Handle any runtime errors, not meaninfully handled by lower level code
            e.printStackTrace();
        }
    }

}
