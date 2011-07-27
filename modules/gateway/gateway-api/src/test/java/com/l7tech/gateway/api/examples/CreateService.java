package com.l7tech.gateway.api.examples;

import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.Accessor.AccessorException;
import com.l7tech.gateway.api.Client;
import com.l7tech.gateway.api.ClientFactory;
import com.l7tech.gateway.api.ClientFactory.InvalidOptionException;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ManagementRuntimeException;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceDetail.HttpMapping;
import com.l7tech.gateway.api.ServiceDetail.SoapMapping;
import com.l7tech.gateway.api.ServiceMO;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway Management API example for service creation.
 */
public class CreateService {

    /**
     * The URL of the Gateway Management Service to connect to.
     */
    private static final String GATEWAY_URL = "https://localhost:8443/wsman";

    /**
     * The credentials for an administrative user with permission to publish a service.
     */
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";

    /**
     * The policy to use for the service.
     *
     * This example is the default policy for a Gateway Management service.
     */
    private static final String policyXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SslAssertion/>\n" +
            "        <L7p:HttpBasic/>\n" +
            "        <L7p:Authentication>\n" +
            "            <L7p:IdentityProviderOid longValue=\"-2\"/>\n" +
            "        </L7p:Authentication>\n" +
            "        <L7p:GatewayManagement/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    /**
     * Application that publishes a service.
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
            final Accessor<ServiceMO> serviceAccessor = client.getAccessor( ServiceMO.class );

            // Create objects for the service
            final ServiceMO service = ManagedObjectFactory.createService();
            final ServiceDetail detail = ManagedObjectFactory.createServiceDetail();
            final HttpMapping httpMapping = ManagedObjectFactory.createHttpMapping();
            final SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
            final ResourceSet wsdlResourceSet = ManagedObjectFactory.createResourceSet();
            final ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
            final Resource policyResource = ManagedObjectFactory.createResource();

            // Configure the service details
            detail.setEnabled( true );
            detail.setName( "My Gateway Management Service" );
            httpMapping.setUrlPattern( "/mywsman" );
            httpMapping.setVerbs( Arrays.asList( "POST" ) );
            soapMapping.setLax( false );
            detail.setServiceMappings( Arrays.asList( httpMapping, soapMapping ) );
            final Map<String,Object> properties = new HashMap<String,Object>();
            properties.put( "soap", true );
            properties.put( "soapVersion", "1.2" );
            detail.setProperties( properties );

            // Configure the resources for the service
            //
            // In this example we only set the URL for the WSDL resource set.
            // Alternatively we could provide the WSDL resources as we do for
            // the policy resource set.
            //
            // This example assumes you have published a Gateway Management
            // service at the default location.
            //
            wsdlResourceSet.setTag( "wsdl" );
            wsdlResourceSet.setRootUrl( "http://localhost:8080/wsman?wsdl" ); // The URL of the WSDL for the service.
            policyResourceSet.setTag( "policy" );
            policyResource.setType( "policy" );
            policyResource.setContent( policyXml );
            policyResourceSet.setResources( Arrays.asList( policyResource ) );

            service.setServiceDetail( detail );
            service.setResourceSets( Arrays.asList( wsdlResourceSet, policyResourceSet ) );

            // Create the service and log the identifier
            final String identifier = serviceAccessor.create( service );
            System.out.println( "Created service with identifier: " + identifier );

            // Access the newly created service and log as XML
            final ServiceMO newService = serviceAccessor.get( identifier );
            System.out.println( "Service XML:" );
            ManagedObjectFactory.write( newService, System.out );
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
            // Handle error due to writing service
            e.printStackTrace();
        }
    }

}
