package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for entity validation annotations.
 *
 * <p>This test is currently part of the gateway management modular assertion
 * as that is the only user of the annotations. This test should be moved if
 * the annotations are used in another module.</p>
 */
public class EntityValidationTest {

    //- PUBLIC

    @Test
    public void testClusterProperty() {
        ClusterProperty clusterProperty = new ClusterProperty("name", "value");
        valid( clusterProperty, "basic cluster property" );

        // Test name not null
        clusterProperty.setName( null );
        invalid( clusterProperty, "null name" );

        // Test name length
        clusterProperty.setName( "" );
        invalid( clusterProperty, "empty name" );

        clusterProperty.setName( "a" );
        valid( clusterProperty, "short name" );

        clusterProperty.setName( string(128, 'a') );
        valid( clusterProperty, "longest name" );

        clusterProperty.setName( string(129, 'a') );
        invalid( clusterProperty, "long name" );

        clusterProperty.setName( "name" );

        // Test value not null
        clusterProperty.setValue( null );
        invalid( clusterProperty, "null value" );

        clusterProperty.setValue( "" );
        valid( clusterProperty, "empty value" );
    }

    @Test
    public void testJmsEndpoint() {
        JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setName( "name" );
        jmsEndpoint.setDestinationName( "name" );
        valid( jmsEndpoint, "basic JMS endpoint" );

        // Test name
        jmsEndpoint.setName( null );
        invalid( jmsEndpoint, "null name" );

        jmsEndpoint.setName( "" );
        invalid( jmsEndpoint, "empty name" );

        jmsEndpoint.setName( string(128, 'a') );
        valid( jmsEndpoint, "longest name" );

        jmsEndpoint.setName( string(129, 'a') );
        invalid( jmsEndpoint, "long name" );

        jmsEndpoint.setName( "name" );

        // Test destination name
        jmsEndpoint.setDestinationName( null );
        invalid( jmsEndpoint, "null destination name" );

        jmsEndpoint.setDestinationName( "" );
        invalid( jmsEndpoint, "empty destination name" );

        jmsEndpoint.setDestinationName( "     " );
        invalid( jmsEndpoint, "all spaces destination name" );

        jmsEndpoint.setDestinationName( string(128, 'a') );
        valid( jmsEndpoint, "longest destination name" );

        jmsEndpoint.setDestinationName( string(129, 'a') );
        invalid( jmsEndpoint, "long destination name" );

        jmsEndpoint.setDestinationName( "name" );

        // Test username
        jmsEndpoint.setUsername( "" );
        valid( jmsEndpoint, "empty username" );

        jmsEndpoint.setUsername( string(32, 'a') );
        valid( jmsEndpoint, "longest username" );

        jmsEndpoint.setUsername( string(33, 'a') );
        invalid( jmsEndpoint, "long username" );

        jmsEndpoint.setUsername( null );

        // Test password
        jmsEndpoint.setPassword( "" );
        valid( jmsEndpoint, "empty password" );

        jmsEndpoint.setPassword( string(32, 'a') );
        valid( jmsEndpoint, "longest password" );

        jmsEndpoint.setPassword( string(33, 'a') );
        invalid( jmsEndpoint, "long password" );

        jmsEndpoint.setPassword( null );

        // Test reply to queue name
        jmsEndpoint.setReplyToQueueName( "     " );
        invalid( jmsEndpoint, "all spaces reply to queue name" );

        jmsEndpoint.setReplyToQueueName( "" );
        invalid( jmsEndpoint, "empty reply to queue name" );

        jmsEndpoint.setReplyToQueueName( string(128, 'a') );
        valid( jmsEndpoint, "longest reply to queue name" );

        jmsEndpoint.setReplyToQueueName( string(129, 'a') );
        invalid( jmsEndpoint, "long reply to queue name" );

        jmsEndpoint.setReplyToQueueName( null );

        // Test failure queue name
        jmsEndpoint.setFailureDestinationName( "     " );
        invalid( jmsEndpoint, "all spaces failure queue name" );

        jmsEndpoint.setFailureDestinationName( "" );
        invalid( jmsEndpoint, "empty failure queue name" );

        jmsEndpoint.setFailureDestinationName( string(128, 'a') );
        valid( jmsEndpoint, "longest failure queue name" );

        jmsEndpoint.setFailureDestinationName( string(129, 'a') );
        invalid( jmsEndpoint, "long failure queue name" );
    }
    
    @Test
    public void testJmsConnection() {
        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setName( "name" );
        jmsConnection.setJndiUrl( "ldap://host" );
        jmsConnection.setInitialContextFactoryClassname( "some.Class" );
        valid( jmsConnection, "basic JMS connection" );

        // Test name
        jmsConnection.setName( null );
        invalid( jmsConnection, "null name" );

        jmsConnection.setName( "" );
        invalid( jmsConnection, "empty name" );

        jmsConnection.setName( string(128, 'a') );
        valid( jmsConnection, "longest name" );

        jmsConnection.setName( string(129, 'a') );
        invalid( jmsConnection, "long name" );

        jmsConnection.setName( "name" );

        // Test JNDI url
        jmsConnection.setJndiUrl( null );
        invalid( jmsConnection, "null jndiUrl" );

        jmsConnection.setJndiUrl( "" );
        invalid( jmsConnection, "empty jndiUrl" );

        jmsConnection.setJndiUrl( string(255, 'a') );
        valid( jmsConnection, "longest jndiUrl" );

        jmsConnection.setJndiUrl( string(256, 'a') );
        invalid( jmsConnection, "long jndiUrl" );

        jmsConnection.setJndiUrl( "ldap://host" );

        // Test initial context factory
        jmsConnection.setInitialContextFactoryClassname( null );
        invalid( jmsConnection, "null initial context classname" );

        jmsConnection.setInitialContextFactoryClassname( "" );
        invalid( jmsConnection, "empty initial context classname" );

        jmsConnection.setInitialContextFactoryClassname( string(255, 'a') );
        valid( jmsConnection, "longest initial context classname" );

        jmsConnection.setInitialContextFactoryClassname( string(256, 'a') );
        invalid( jmsConnection, "long initial context classname" );

        jmsConnection.setInitialContextFactoryClassname(  "some.Class" );

        // Test username
        jmsConnection.setUsername( "" );
        valid( jmsConnection, "empty username" );

        jmsConnection.setUsername( string(32, 'a') );
        valid( jmsConnection, "longest username" );

        jmsConnection.setUsername( string(33, 'a') );
        invalid( jmsConnection, "long username" );

        jmsConnection.setUsername( null );

        // Test password
        jmsConnection.setPassword( "" );
        valid( jmsConnection, "empty password" );

        jmsConnection.setPassword( string(32, 'a') );
        valid( jmsConnection, "longest password" );

        jmsConnection.setPassword( string(33, 'a') );
        invalid( jmsConnection, "long password" );

        jmsConnection.setPassword( null );

        // Test destination factory url
        jmsConnection.setDestinationFactoryUrl( "" );
        invalid( jmsConnection, "empty destination factory url" );

        jmsConnection.setDestinationFactoryUrl( "a" );
        valid( jmsConnection, "shortest destination factory url" );

        jmsConnection.setDestinationFactoryUrl( string(255, 'a') );
        valid( jmsConnection, "longest destination factory url" );

        jmsConnection.setDestinationFactoryUrl( string(256, 'a') );
        invalid( jmsConnection, "long destination factory url" );

        jmsConnection.setDestinationFactoryUrl( null );

        // Test queue factory url
        jmsConnection.setQueueFactoryUrl( "" );
        invalid( jmsConnection, "empty queue factory url" );

        jmsConnection.setQueueFactoryUrl( "a" );
        valid( jmsConnection, "shortest queue factory url" );

        jmsConnection.setQueueFactoryUrl( string(255, 'a') );
        valid( jmsConnection, "longest queue factory url" );

        jmsConnection.setQueueFactoryUrl( string(256, 'a') );
        invalid( jmsConnection, "long queue factory url" );

        jmsConnection.setQueueFactoryUrl( null );

        // Test topic factory url
        jmsConnection.setTopicFactoryUrl( "" );
        invalid( jmsConnection, "empty topic factory url" );

        jmsConnection.setTopicFactoryUrl( "a" );
        valid( jmsConnection, "shortest topic factory url" );

        jmsConnection.setTopicFactoryUrl( string(255, 'a') );
        valid( jmsConnection, "longest topic factory url" );

        jmsConnection.setTopicFactoryUrl( string(256, 'a') );
        invalid( jmsConnection, "long topic factory url" );
    }

    @Test
    public void testPolicy() {
        Policy policy = getPolicy();
        valid( policy, "basic policy" );

        // Test null name
        policy.setName( null );
        invalid( policy, "null name" );

        // Test name length
        policy.setName( "" );
        invalid( policy, "empty name" );

        policy.setName( "a" );
        valid( policy, "short name" );

        policy.setName( string(255,'a') );
        valid( policy, "longest name" );

        policy.setName( string(256,'a') );
        invalid( policy, "long name" );

        policy.setName( "Policy" );

        // Test GUID null
        policy.setGuid( null );
        invalid( policy, "null guid" );

        // Test GUID size
        policy.setGuid( "" );
        invalid( policy, "empty guid" );

        policy.setGuid( "1" );
        invalid( policy, "short guid" );
        
        policy.setGuid( string(37,'a') );
        invalid( policy, "long guid" );

        policy.setGuid( UUID.randomUUID().toString() );

        // Test policy type null
        policy.setType( null );
        invalid( policy, "null policy type" );

        policy.setType( PolicyType.INCLUDE_FRAGMENT );

        // Test XML null
        policy.setXml( null );
        invalid( policy, "null policy xml" );

        policy.setXml( "" );
        invalid( policy, "empty policy xml" );

        policy.setXml( "<policy/>" );
        
        // Test internal tag size
        policy.setInternalTag( "" );
        invalid( policy, "empty tag" );

        policy.setInternalTag( "1" );
        valid( policy, "short tag" );

        policy.setInternalTag( string(65, '1') );
        invalid( policy, "long tag" );

        policy.setInternalTag( string(64, '1') );
        valid( policy, "max length tag" );
    }

    @Test
    public void testPublishedService() throws Exception {
        PublishedService service = getService();
        valid( service, "basic service" );

        // Test null name
        service.setName( null );
        invalid( service, "null name" );

        // Test name
        service.setName( "" );
        invalid( service, "empty name" );

        service.setName( "a" );
        valid( service, "short name" );

        service.setName( string(256,'a') );
        invalid( service, "service name 256" );

        service.setName( string(255,'a') );
        valid( service, "service name 255" );

        service.setName( string(256,'a') );
        invalid( service, "service name 256" );

        service.setName( "Test Name" );
        
        // Test WSDL url 255 chars
        service.setWsdlUrl( "http://GW/" + string(245,'a') );
        valid( service, "wsdl url 255 ");

        service.setWsdlUrl( "http://GW/" + string(246,'a') );
        invalid( service, "wsdl url 256" );

        service.setWsdlUrl( null );

        // Test Default routing url 4096 chars
        service.setDefaultRoutingUrl( string(4096,'a') );
        valid( service, "default routing url 4096" );

        service.setDefaultRoutingUrl( string(4097,'a') );
        invalid( service, "default routing url 4097" );
    }

    @Test
    public void testPublishedServiceRoutingUri() {
        PublishedService service = getService();
        valid( service, "basic service" );

        // Test valid paths are permitted
        service.setRoutingUri( "/*" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/a" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/asdf" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/1path/2with/3lots/4OF/5FoLdErs/6?and=Some&Querystring-stuff__|#fragment" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/serviceuriwith128characters/123456789/123456789/123456789/123456789/123456789/123456789/123456789/123456789/123456789/123456789" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/serviceuriwith129characters/123456789/123456789/123456789/123456789/123456789/123456789/123456789/123456789/123456789/123456789/" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        // Test "/ssg" prefix is not permitted
        service.setRoutingUri( "/ssg" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/ssgE" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/a/ssg" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        // Test "/service/12321" uri is not permitted
        service.setRoutingUri( "/service/blah" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/service/123a" );
        valid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/ere/service/123" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/service/0" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/service/123" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/service/23141123541535232" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        // Test "*[?|&]serviceoid=3421" is not permitted
        service.setRoutingUri( "/path?serviceoid=123" );
        invalid( service, "service with routing uri " + service.getRoutingUri() );

        service.setRoutingUri( "/path?a=b&serviceoid=12y3&e=f" );
        valid( service, "service with routing uri " + service.getRoutingUri() );
    }

    @Test
    public void testSchemaEntry() {
        SchemaEntry schemaEntry = new SchemaEntry();
        schemaEntry.setSchema( "<schema/>" );
        schemaEntry.setTns( "urn:tns" );

        // Test name not null
        invalid( schemaEntry, "null name" );

        schemaEntry.setName( "http://someurl" );
        valid( schemaEntry, "basic schema entry" );

        // Test name length
        schemaEntry.setName( "" );
        invalid( schemaEntry, "empty name" );

        schemaEntry.setName( "a" );
        valid( schemaEntry, "short name" );

        schemaEntry.setName( string(4096, 'a') );
        valid( schemaEntry, "longest name" );

        schemaEntry.setName( string(4097, 'a') );
        invalid( schemaEntry, "long name" );

        schemaEntry.setName( "name" );

        // Test tns
        schemaEntry.setTns( null );
        valid( schemaEntry, "null tns" );

        schemaEntry.setTns( "" );
        valid( schemaEntry, "empty tns" );

        schemaEntry.setTns( string(128,'a') );
        valid( schemaEntry, "longest tns" );

        schemaEntry.setTns( string(129,'a') );
        invalid( schemaEntry, "long tns" );

        schemaEntry.setTns( "urn:tns" );

        // Test schema not null (DISABLED since the getter sets the value to non-null)
        //schemaEntry.setSchema( null );
        //invalid( schemaEntry, "null schema" );
    }

    //- PRIVATE

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private void valid( final Object entity, final String description ) {
        final Set<ConstraintViolation<Object>> violations = validator.validate( entity );

        assertEquals( "Validation errors for " + description, Collections.<ConstraintViolation<Object>>emptySet(), violations );
    }

    private void invalid( final Object entity, final String description ) {
        final Set<ConstraintViolation<Object>> violations = validator.validate( entity );

        assertTrue( "Expected validation error(s) for " + description, !violations.isEmpty() );
    }

    private String string( final int count, final char character ) {
        char[] chars = new char[count];
        Arrays.fill( chars, character );
        return new String( chars );
    }

    private Policy getPolicy() {
        Policy policy = new Policy( PolicyType.INCLUDE_FRAGMENT, "Policy", "<policy/>", true );
        policy.setGuid( UUID.randomUUID().toString() );
        return policy;
    }

    private PublishedService getService() {
        PublishedService service = new PublishedService();
        service.setName( "Test Service" );
        service.setPolicy( new Policy( PolicyType.INTERNAL, "Policy for test service", "<policy/>", true ) );
        service.getPolicy().setGuid( UUID.randomUUID().toString() );
        return service;
    }
}
