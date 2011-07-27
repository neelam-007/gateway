package com.l7tech.gateway.api.examples;

import com.l7tech.gateway.api.Accessor.AccessorException;
import com.l7tech.gateway.api.Client;
import com.l7tech.gateway.api.ClientFactory;
import com.l7tech.gateway.api.ClientFactory.InvalidOptionException;
import com.l7tech.gateway.api.ManagementRuntimeException;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.PrivateKeyMOAccessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Gateway Management API example for Private key export.
 */
public class ExportPrivateKey {

    /**
     * The URL of the Gateway Management Service to connect to.
     */
    private static final String GATEWAY_URL = "https://localhost:8443/wsman";

    /**
     * The credentials for an administrative user with permission to export the
     * private key.
     *
     * Note that export is typically only supported for private keys stored in
     * the software key store.
     */
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";

    /**
     * Application that writes a private key to file.
     *
     * @args The applications command line arguments
     */
    public static void main( final String[] args ) {
        FileOutputStream pkcs12Output = null;
        try {
            final ClientFactory factory = ClientFactory.newInstance();

            factory.setAttribute( ClientFactory.ATTRIBUTE_USERNAME, USERNAME );
            factory.setAttribute( ClientFactory.ATTRIBUTE_PASSWORD, PASSWORD );

            // For the example we disable certificate and hostname validation,
            // you would not do this in production
            factory.setFeature( ClientFactory.FEATURE_CERTIFICATE_VALIDATION, false );
            factory.setFeature( ClientFactory.FEATURE_HOSTNAME_VALIDATION, false );

            final Client client = factory.createClient( GATEWAY_URL );
            final PrivateKeyMOAccessor privateKeyAccessor = client.getAccessor( PrivateKeyMO.class, PrivateKeyMOAccessor.class );
            final byte[] pkcs12Data = privateKeyAccessor.exportKey( "2:ssl", "alias", "password" );
            final File pkcs12File = new File( "key.p12" );
            System.out.println( "Writing PKCS12 file: " + pkcs12File.getAbsolutePath() );
            pkcs12Output = new FileOutputStream( pkcs12File );
            pkcs12Output.write( pkcs12Data );
        } catch ( InvalidOptionException e ) {
            // Handle error due to an incorrect feature or attribute
            e.printStackTrace();
        } catch ( AccessorException e ) {
            // Handle "business logic" errors
            e.printStackTrace();
        } catch ( ManagementRuntimeException e ) {
            // Handle any runtime errors, not meaninfully handled by lower level code
            e.printStackTrace();
        } catch ( IOException e ) {
            // Handle any file output errors
            e.printStackTrace();
        } finally {
            if ( pkcs12Output != null ) {
                try {
                    pkcs12Output.close();
                } catch ( IOException e ) {
                    // Handle any file output errors
                    e.printStackTrace();
                }
            }
        }

    }

}
