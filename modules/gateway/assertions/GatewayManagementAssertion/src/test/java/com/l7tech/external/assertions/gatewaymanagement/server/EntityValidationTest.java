package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.beans.PropertyDescriptor;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.*;

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

        // Test name
        checkNull( clusterProperty, "name", false );
        checkSize( clusterProperty, "name", 1, 128 );

        // Test value
        checkNull( clusterProperty, "value", false );

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
        checkNull( jmsEndpoint, "name", false );
        checkSize( jmsEndpoint, "name", 1, 128 );

        // Test destination name
        checkNull( jmsEndpoint, "destinationName", false );
        checkSize( jmsEndpoint, "destinationName", 1, 128 );

        jmsEndpoint.setDestinationName( "     " );
        invalid( jmsEndpoint, "all spaces destination name" );

        jmsEndpoint.setDestinationName( "name" );

        // Test username
        checkSize( jmsEndpoint, "username", 0, 255 );

        // Test password
        checkSize( jmsEndpoint, "password", 0, 255 );

        // Test reply to queue name
        checkSize( jmsEndpoint, "replyToQueueName", 1, 128 );

        jmsEndpoint.setReplyToQueueName( "     " );
        invalid( jmsEndpoint, "all spaces reply to queue name" );

        jmsEndpoint.setReplyToQueueName( null );

        // Test failure queue name
        checkSize( jmsEndpoint, "failureDestinationName", 1, 128 );

        jmsEndpoint.setFailureDestinationName( "     " );
        invalid( jmsEndpoint, "all spaces failure queue name" );

        jmsEndpoint.setFailureDestinationName( null );

        // Test template
        jmsEndpoint.setTemplate( true );
        valid( jmsEndpoint, "basic template" );

        jmsEndpoint.setDestinationName( null );
        valid( jmsEndpoint, "template null destination name" );

        jmsEndpoint.setDestinationName( "name" );
        jmsEndpoint.setReplyType( JmsReplyType.REPLY_TO_OTHER );
        jmsEndpoint.setReplyToQueueName( null );
        valid( jmsEndpoint, "template reply to queue name" );
    }
    
    @Test
    public void testJmsConnection() {
        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setName( "name" );
        jmsConnection.setJndiUrl( "ldap://host" );
        jmsConnection.setInitialContextFactoryClassname( "some.Class" );
        valid( jmsConnection, "basic JMS connection" );

        // Test name
        checkNull( jmsConnection, "name", false );
        checkSize( jmsConnection, "name", 1, 128 );

        // Test provider type
        jmsConnection.setProviderType( JmsProviderType.Tibco );
        valid( jmsConnection, "non null provider type" );

        jmsConnection.setProviderType( null );

        // Test JNDI url
        checkNull( jmsConnection, "jndiUrl", false );
        checkSize( jmsConnection, "jndiUrl", 1, 255 );

        // Test initial context factory
        checkNull( jmsConnection, "initialContextFactoryClassname", false );
        checkSize( jmsConnection, "initialContextFactoryClassname", 1, 255 );

        // Test username
        checkSize( jmsConnection, "username", 0, 255 );

        // Test password
        checkSize( jmsConnection, "password", 0, 255 );

        // Test destination factory url
        checkSize( jmsConnection, "destinationFactoryUrl", 1, 255 );

        // Test queue factory url
        checkSize( jmsConnection, "queueFactoryUrl", 1, 255 );

        // Test topic factory url
        checkSize( jmsConnection, "topicFactoryUrl", 1, 255 );

        // Test template
        jmsConnection.setTemplate( true );
        valid( jmsConnection, "basic template" );

        jmsConnection.setJndiUrl( null );
        valid( jmsConnection, "template null jndiUrl" );

        jmsConnection.setJndiUrl( "ldap://host" );
        jmsConnection.setInitialContextFactoryClassname( null );
        valid( jmsConnection, "template null initial context classname" );

        jmsConnection.setInitialContextFactoryClassname( "some.Class" );
        jmsConnection.setQueueFactoryUrl( null );
        valid( jmsConnection, "template null queue factory url" );
    }

    @Test
    public void testPolicy() {
        Policy policy = getPolicy();
        valid( policy, "basic policy" );

        // Test name
        checkNull( policy, "name", false );
        checkSize( policy, "name", 1, 255 );

        // Test GUID
        checkNull( policy, "guid", false );
        checkSize( policy, "guid", 36, 36 );

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

        // Test name
        checkNull( service, "name", false );
        checkSize( service, "name", 1, 255 );

        // Test WSDL url 255 chars
        service.setWsdlUrl( "http://GW/" + string(4086,'a') );
        service.setWsdlXml( "" );
        valid( service, "wsdl url 4096 ");

        service.setWsdlUrl( "http://GW/" + string(4087,'a') );
        service.setWsdlXml( "" );
        invalid( service, "wsdl url 4097" );

        service.setWsdlUrl( null );
        service.setWsdlXml( "" );

        // Test WSDL xml required for SOAP
        service.setWsdlXml( null );
        invalid( service, "null wsdl xml" );

        service.setSoap( false );
        valid( service, "null wsdl xml" );

        service.setSoap( true );
        service.setWsdlXml( "" );

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

        service.setRoutingUri( "/service/23141123541535232/any/path/here" );
        valid( service, "service with routing uri " + service.getRoutingUri() );
    }

    @Test
    public void testResourceEntry() {
        ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setUri( "http://someurl" );
        resourceEntry.setType( ResourceType.XML_SCHEMA );
        resourceEntry.setContentType( ResourceType.XML_SCHEMA.getMimeType() );
        resourceEntry.setContent( "<schema/>" );
        valid( resourceEntry, "basic resource entry" );        

        //Test uri
        checkNull( resourceEntry, "uri", false );
        checkSize( resourceEntry, "uri", 1, 4096 );

        // Test description
        checkSize( resourceEntry, "description", 0, 255 );

        // Test keys
        checkSize( resourceEntry, "resourceKey1", 0, 4096 );
        checkSize( resourceEntry, "resourceKey2", 0, 4096 );
        checkSize( resourceEntry, "resourceKey3", 0, 4096 );

        // Test content not null
        checkNull( resourceEntry, "content", false );
    }

    @Test
    public void testTrustedCert() throws Exception {
        final TrustedCert trustedCert = new TrustedCert();
        trustedCert.setName( "Alice" );
        trustedCert.setCertificate( TestDocuments.getWssInteropAliceCert() );
        trustedCert.setRevocationCheckPolicyType( TrustedCert.PolicyUsageType.USE_DEFAULT );
        valid( trustedCert, "basic trusted certificate" );

        // test name
        checkNull( trustedCert, "name", false );
        checkSize( trustedCert, "name", 1, 128 );

        // test subject DN
        checkNull( trustedCert, "subjectDn", true );
        checkSize( trustedCert, "subjectDn", 0, 500 );

        // test cert b64
        checkNull( trustedCert, "certBase64", false );

        // test revocation check type
        checkNull( trustedCert, "revocationCheckPolicyType", false );

        // test thumbprint
        checkNull( trustedCert, "thumbprintSha1", true );
        checkSize( trustedCert, "thumbprintSha1", 0, 64 );

        // test ski
        checkNull( trustedCert, "ski", true );
        checkSize( trustedCert, "ski", 0, 64 );

        // test issuer DN
        checkNull( trustedCert, "issuerDn", true );
        checkSize( trustedCert, "issuerDn", 0, 500 );

        // test serial
        checkNull( trustedCert, "serial", true );
    }

    @Test
    public void testEncapsulatedAssertionConfig() throws Exception {
        final EncapsulatedAssertionConfig eac = new EncapsulatedAssertionConfig();
        eac.setName( "encassconfig1" );
        eac.setGuid( "abcd-0001" );

        EncapsulatedAssertionArgumentDescriptor arg1 = new EncapsulatedAssertionArgumentDescriptor();
        arg1.setOrdinal( 1 );
        arg1.setArgumentName( "arg1" );
        arg1.setArgumentType( "boolean" );
        arg1.setGuiLabel( "Arg One" );
        arg1.setGuiPrompt( true );
        arg1.setEncapsulatedAssertionConfig( eac );
        eac.setArgumentDescriptors( new LinkedHashSet<EncapsulatedAssertionArgumentDescriptor>( Arrays.asList( arg1 ) ) );

        EncapsulatedAssertionResultDescriptor ret1 = new EncapsulatedAssertionResultDescriptor();
        ret1.setResultName( "ret1" );
        ret1.setResultType( "string" );
        ret1.setEncapsulatedAssertionConfig( eac );
        eac.setResultDescriptors( new LinkedHashSet<EncapsulatedAssertionResultDescriptor>( Arrays.asList( ret1 ) ) );

        eac.setProperties( Collections.singletonMap( "a", "b" ) );

        // TODO override getName() and test name, confirm it doesn't break import/export via SSM
        //checkNull( eac, "name", false );
        //checkSize( eac, "name", 1, 128 );

        // TODO add additional validation and additional validation tests

    }

    @Test
    public void testPrivateKey() throws Exception {
        final SsgKeyEntry entry = new SsgKeyEntry( 1L, "alias", new X509Certificate[]{ TestDocuments.getWssInteropAliceCert() }, null );
        valid( entry, "basic private key" );

        // test certificate chain
        checkNull( entry, "certificateChain", false );

        entry.setCertificateChain( new X509Certificate[0] );
        invalid( entry, "empty certificate chain" );

        entry.setCertificateChain( new X509Certificate[]{ TestDocuments.getWssInteropAliceCert() } );
    }

    @Test
    public void testJdbcConnection() {
        final JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName( "Test" );
        jdbcConnection.setDriverClass( "Test" );
        jdbcConnection.setJdbcUrl( "jdbc://mysql//ssg" );
        jdbcConnection.setUserName( "username" );
        jdbcConnection.setPassword( "password" );
        valid( jdbcConnection, "basic JDBC connection" );
        
        // test name
        checkNull( jdbcConnection, "name", false );
        checkSize( jdbcConnection, "name", 1, 128 );

        // test url
        checkNull( jdbcConnection, "driverClass", false );
        checkSize( jdbcConnection, "driverClass", 1, 256 );

        // test driver class
        checkNull( jdbcConnection, "jdbcUrl", false );
        checkSize( jdbcConnection, "jdbcUrl", 1, 4096 );

        // test username
        checkNull( jdbcConnection, "userName", false );
        checkSize( jdbcConnection, "userName", 0, 128 );

        // test password
        checkNull( jdbcConnection, "password", false );
        checkSize( jdbcConnection, "password", 0, 64 );
    }

    @Test
    public void testFolder() {
        final Folder rootFolder = new Folder( "Root Folder", null );
        final Folder folder = new Folder( "Test", rootFolder );
        valid( folder, "basic Folder" );

        // test name
        checkNull( folder, "name", false );
        checkSize( folder, "name", 1, 128 );

        // test folder
        checkNull( folder, "folder", false );
    }

    @Test
    public void testIdentityProviderConfig() {
        final IdentityProviderConfig config = new IdentityProviderConfig();
        config.setName( "Test" );
        valid( config, "basic Identity Provider Config" );

        // test name
        checkNull( config, "name", false );
        checkSize( config, "name", 1, 128 );
    }

    @Test
    public void testSsgConnector() {
        final SsgConnector ssgConnector = new SsgConnector();
        ssgConnector.setName( "test" );
        ssgConnector.setScheme( "http" );
        ssgConnector.setPort( 8080 );
        valid( ssgConnector, "basic connector" );

        // test name
        checkNull( ssgConnector, "name", false );
        checkSize( ssgConnector, "name", 1, 128 );

        // test port
        checkRange( ssgConnector, "port", 1025, 65535 );

        // test name
        checkNull( ssgConnector, "scheme", false );
        checkSize( ssgConnector, "scheme", 1, 128 );

        // test client authentication
        checkRange( ssgConnector, "clientAuth", 0, 2 );

        // test key alias
        checkNull( ssgConnector, "keyAlias", true );
        checkSize( ssgConnector, "keyAlias", 1, 255 );
    }

    @Test
    public void testSecurePassword() {
        final SecurePassword securePassword = new SecurePassword();
        securePassword.setName( "test" );
        securePassword.setType( SecurePassword.SecurePasswordType.PASSWORD );
        securePassword.setEncodedPassword( "PASS" );
        valid( securePassword, "basic secure password" );

        // test name
        checkNull( securePassword, "name", false );
        checkSize( securePassword, "name", 1, 128 );

        // test description
        checkNull( securePassword, "description", true );
        checkSize( securePassword, "description", 0, 256 );

        // test encoded password
        checkNull( securePassword, "encodedPassword", false );
        checkSize( securePassword, "encodedPassword", 0, 65535 );

        // test type
        checkNull( securePassword, "type", false );
    }

    //- PRIVATE

    private final EntityPropertiesHelper helper = new EntityPropertiesHelper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private void valid( final Object entity, final String description ) {
        final Set<ConstraintViolation<Object>> violations = validator.validate( entity, helper.getValidationGroups(entity) );

        assertEquals( "Validation errors for " + description, Collections.<ConstraintViolation<Object>>emptySet(), violations );
    }

    private void invalid( final Object entity, final String description ) {
        final Set<ConstraintViolation<Object>> violations = validator.validate( entity, helper.getValidationGroups(entity) );

        assertTrue( "Expected validation error(s) for " + description, !violations.isEmpty() );
    }

    private String string( final int count, final char character ) {
        char[] chars = new char[count];
        Arrays.fill( chars, character );
        return new String( chars );
    }

    private void checkNull( final Object entity,
                            final String field,
                            final boolean permitted ) {
        final Object originalValue = get( entity, field );
        set( entity, field, null );
        if ( permitted ) {
            valid( entity, "null " + field );
        } else {
            invalid( entity, "null " + field );
        }
        set( entity, field, originalValue );
    }

    private void checkSize( final Object entity,
                            final String field,
                            final int minSize,
                            final int maxSize ) {
        final Object originalValue = get( entity, field );
        set( entity, field,  "" );
        if ( minSize <= 0 ) {
            valid( entity, "empty " + field );
        } else {
            invalid( entity, "empty " + field );
        }

        if ( minSize > 1 ) {
            set( entity, field,  string(minSize-1, 'a') );
            invalid( entity, "short " + field );
        }

        set( entity, field,  string(minSize, 'a') );
        valid( entity, "shortest " + field );

        set( entity, field,  string(maxSize, 'a') );
        valid( entity, "longest " + field );

        set( entity, field,  string(maxSize+1, 'a') );
        invalid( entity, "long " + field );

        set( entity, field, originalValue );
    }

    private void checkRange( final Object entity,
                             final String field,
                             final int minValue,
                             final int maxValue ) {
        final Object originalValue = get( entity, field );
        set( entity, field,  0 );
        if ( minValue <= 0 ) {
            valid( entity, "zero " + field );
        } else {
            invalid( entity, "zero " + field );
        }

        if ( minValue > Integer.MIN_VALUE ) {
            set( entity, field,  minValue - 1 );
            invalid( entity, "low " + field );
        }

        set( entity, field,  minValue );
        valid( entity, "lowest " + field );

        set( entity, field, maxValue );
        valid( entity, "highest " + field );

        if ( minValue < Integer.MAX_VALUE ) {
            set( entity, field,  maxValue+1 );
            invalid( entity, "high " + field );
        }

        set( entity, field, originalValue );
    }

    private Object get( final Object entity,
                        final String field ) {
        try {
            return prop(entity,field).getReadMethod().invoke( entity );
        } catch ( Exception e ) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private void set( final Object entity,
                      final String field,
                      final Object value ) {
        try {
            prop(entity,field).getWriteMethod().invoke( entity, value );
        } catch ( Exception e ) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private PropertyDescriptor prop( final Object entity,
                                     final String field ) {
        PropertyDescriptor pd = null;
        for ( final PropertyDescriptor current : BeanUtils.getProperties( entity.getClass() ) ) {
            if ( current.getName().equals( field ) ) {
                pd = current;
                break;
            }
        }

        if ( pd == null ) fail( "Property '"+field+"', not found for bean with type '"+entity.getClass()+"'." );

        return pd;
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
        service.setWsdlXml( "" );
        return service;
    }
}
