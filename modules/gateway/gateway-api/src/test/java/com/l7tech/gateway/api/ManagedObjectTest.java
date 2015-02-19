package com.l7tech.gateway.api;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.impl.*;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.l7tech.util.CollectionUtils.list;
import static org.junit.Assert.*;

/**
 * Serialization tests for managed objects.
 */
public class ManagedObjectTest {

    //- PUBLIC

    private static final String MANAGEMENT_NS = "http://ns.l7tech.com/2010/04/gateway-management";
    // Managed objects added in rev 2 of the API, these won't work with a rev 1 schema
    private static final Collection<Class<? extends ManagedObject>> MANAGED_OBJECTS_2 = list(
            InterfaceTagMO.class,
            ListenPortMO.class,
            PrivateKeyCreationContext.class,
            PrivateKeyExportContext.class,
            PrivateKeyExportResult.class,
            PrivateKeyGenerateCsrContext.class,
            PrivateKeyGenerateCsrResult.class,
            PrivateKeyImportContext.class,
            PrivateKeySpecialPurposeContext.class,
            RevocationCheckingPolicyMO.class,
            StoredPasswordMO.class
    );
    private static final Collection<Class<? extends ManagedObject>> MANAGED_OBJECTS = list(
            ClusterPropertyMO.class,
            FolderMO.class,
            IdentityProviderMO.class,
            InterfaceTagMO.class,
            JDBCConnectionMO.class,
            JMSDestinationMO.class,
            ListenPortMO.class,
            PolicyExportResult.class,
            PolicyImportContext.class,
            PolicyImportResult.class,
            PolicyMO.class,
            PolicyValidationContext.class,
            PolicyValidationResult.class,
            PrivateKeyCreationContext.class,
            PrivateKeyExportContext.class,
            PrivateKeyExportResult.class,
            PrivateKeyGenerateCsrContext.class,
            PrivateKeyGenerateCsrResult.class,
            PrivateKeyImportContext.class,
            PrivateKeySpecialPurposeContext.class,
            PrivateKeyMO.class,
            ResourceDocumentMO.class,
            RevocationCheckingPolicyMO.class,
            ServiceMO.class,
            StoredPasswordMO.class,
            TrustedCertificateMO.class
    );
    private static final String CERT_BOB_PEM =
                "MIIDCjCCAfKgAwIBAgIQYDju2/6sm77InYfTq65x+DANBgkqhkiG9w0BAQUFADAw\n" +
                "MQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENB\n" +
                "MB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQDEOMAwGA1UECgwFT0FT\n" +
                "SVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQwwCgYDVQQDDANC\n" +
                "b2IwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMCquMva4lFDrv3fXQnKK8Ck\n" +
                "SU7HvVZ0USyJtlL/yhmHH/FQXHyYY+fTcSyWYItWJYiTZ99PAbD+6EKBGbdfuJNU\n" +
                "JCGaTWc5ZDUISqM/SGtacYe/PD/4+g3swNPzTUQAIBLRY1pkr2cm3s5Ch/f+mYVN\n" +
                "BR41HnBeIxybw25kkoM7AgMBAAGjgZMwgZAwCQYDVR0TBAIwADAzBgNVHR8ELDAq\n" +
                "MCiiJoYkaHR0cDovL2ludGVyb3AuYmJ0ZXN0Lm5ldC9jcmwvY2EuY3JsMA4GA1Ud\n" +
                "DwEB/wQEAwIEsDAdBgNVHQ4EFgQUXeg55vRyK3ZhAEhEf+YT0z986L0wHwYDVR0j\n" +
                "BBgwFoAUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wDQYJKoZIhvcNAQEFBQADggEBAIiV\n" +
                "Gv2lGLhRvmMAHSlY7rKLVkv+zEUtSyg08FBT8z/RepUbtUQShcIqwWsemDU8JVts\n" +
                "ucQLc+g6GCQXgkCkMiC8qhcLAt3BXzFmLxuCEAQeeFe8IATr4wACmEQE37TEqAuW\n" +
                "EIanPYIplbxYgwP0OBWBSjcRpKRAxjEzuwObYjbll6vKdFHYIweWhhWPrefquFp7\n" +
                "TefTkF4D3rcctTfWJ76I5NrEVld+7PBnnJNpdDEuGsoaiJrwTW3Ixm40RXvG3fYS\n" +
                "4hIAPeTCUk3RkYfUkqlaaLQnUrF2hZSgiBNLPe8gGkYORccRIlZCGQDEpcWl1Uf9\n" +
                "OHw6fC+3hkqolFd5CVI=";
    private JAXBContext context;
    private boolean debug = false;

    @Before
    public void init() throws Exception {
        context = JAXBContext.newInstance("com.l7tech.gateway.api:com.l7tech.gateway.api.impl");
    }

    @Test
    public void testCertificateDataSerialization() throws Exception {
        final CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        final byte[] bytes = new byte[]{ (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1 };
        certificateData.setEncoded( bytes );
        certificateData.setIssuerName("cn=Test Issuer");
        certificateData.setSerialNumber( BigInteger.valueOf( 123456789L ) );
        certificateData.setSubjectName("cn=Test Subject");

        final CertificateData roundTripped = roundTrip( certificateData );

        assertArrayEquals("encoded", bytes, roundTripped.getEncoded());
        assertEquals("issuer name", "cn=Test Issuer", roundTripped.getIssuerName());
        assertEquals("serial number", BigInteger.valueOf( 123456789L ), roundTripped.getSerialNumber());
        assertEquals("subject name", "cn=Test Subject", roundTripped.getSubjectName());
    }

    @Test
    public void testClusterPropertySerialization() throws Exception {
        final ClusterPropertyMO clusterProperty = ManagedObjectFactory.createClusterProperty();
        clusterProperty.setId( "1" );
        clusterProperty.setVersion( 33333 );
        clusterProperty.setName( "property" );
        clusterProperty.setValue( "   value with spaces      and \n newlines" );
        clusterProperty.setProperties( Collections.<String,Object>singletonMap( "prop", 4.4 ) );

        final ClusterPropertyMO roundTripped = roundTrip( clusterProperty );
        assertEquals("id", "1", roundTripped.getId());
        assertEquals("version", (Integer)33333, roundTripped.getVersion());
        assertEquals("name", "property", roundTripped.getName());
        assertEquals("value", "   value with spaces      and \n newlines", roundTripped.getValue());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", 4.4 ), roundTripped.getProperties());
    }

    @Test
    public void testFolderSerialization() throws Exception {
        final FolderMO folder = ManagedObjectFactory.createFolder();
        folder.setId( "2342" );
        folder.setFolderId( "12321" );
        folder.setVersion( 1231211 );
        folder.setName( "folder 1" );
        folder.setProperties( Collections.<String,Object>singletonMap( "prop", 4.432f ) );

        final FolderMO roundTripped = roundTrip( folder );
        assertEquals("id", "2342", roundTripped.getId());
        assertEquals("folder id", "12321", roundTripped.getFolderId());
        assertEquals("version", (Integer)1231211, roundTripped.getVersion());
        assertEquals("name", "folder 1", roundTripped.getName());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", 4.432f ), roundTripped.getProperties());
    }

    @Test
    public void testIdentityProviderSerializationFederated() throws Exception {
        final IdentityProviderMO identityProvider = ManagedObjectFactory.createIdentityProvider();
        identityProvider.setId( "identifier" );
        identityProvider.setVersion( -555 );
        identityProvider.setName( "my provider" );
        identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.LDAP );
        identityProvider.setProperties( Collections.<String,Object>singletonMap( "prop", 123 ) );
        final IdentityProviderMO.FederatedIdentityProviderDetail federatedDetails = identityProvider.getFederatedIdentityProviderDetail();
        federatedDetails.setCertificateReferences( Arrays.asList( "1", "2", "3" ) );

        final IdentityProviderMO roundTripped = roundTrip( identityProvider );
        assertEquals("id", "identifier", roundTripped.getId());
        assertEquals("version", Integer.valueOf(-555), roundTripped.getVersion());
        assertEquals("name", "my provider", roundTripped.getName());
        assertEquals("type", IdentityProviderMO.IdentityProviderType.LDAP, roundTripped.getIdentityProviderType());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", 123 ), roundTripped.getProperties());

        assertNotNull( "federated details", roundTripped.getFederatedIdentityProviderDetail() );
        assertEquals( "federated certificate references", Arrays.asList( "1", "2", "3" ), roundTripped.getFederatedIdentityProviderDetail().getCertificateReferences() );
    }

    @Test
    public void testIdentityProviderSerializationLdap() throws Exception {
        final IdentityProviderMO identityProvider = ManagedObjectFactory.createIdentityProvider();
        identityProvider.setId( "identifier" );
        identityProvider.setVersion( -555 );
        identityProvider.setName( "my provider" );
        identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.LDAP );
        identityProvider.setProperties( Collections.<String,Object>singletonMap( "prop", 123 ) );
        final IdentityProviderMO.LdapIdentityProviderDetail ldapDetails = identityProvider.getLdapIdentityProviderDetail();
        ldapDetails.setSourceType( "Active Directory" );
        ldapDetails.setServerUrls( Arrays.asList( "ldap://host1", "ldap://host2" ) );
        ldapDetails.setSearchBase( "ou=Base" );
        ldapDetails.setBindDn( "browse" );
        ldapDetails.setUseSslClientAuthentication( false );
        ldapDetails.setSpecifiedAttributes(  Arrays.asList( "attrib1", "attrib2" ) );

        final IdentityProviderMO roundTripped = roundTrip( identityProvider );
        assertEquals("id", "identifier", roundTripped.getId());
        assertEquals("version", Integer.valueOf(-555), roundTripped.getVersion());
        assertEquals("name", "my provider", roundTripped.getName());
        assertEquals("type", IdentityProviderMO.IdentityProviderType.LDAP, roundTripped.getIdentityProviderType());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", 123 ), roundTripped.getProperties());

        assertNotNull( "ldap details", roundTripped.getLdapIdentityProviderDetail() );
        assertEquals( "ldap source type", "Active Directory", roundTripped.getLdapIdentityProviderDetail().getSourceType() );
        assertEquals( "ldap server urls", Arrays.asList( "ldap://host1", "ldap://host2" ), roundTripped.getLdapIdentityProviderDetail().getServerUrls() );
        assertEquals( "ldap search base", "ou=Base", roundTripped.getLdapIdentityProviderDetail().getSearchBase() );
        assertEquals( "ldap bind dn", "browse", roundTripped.getLdapIdentityProviderDetail().getBindDn() );
        assertEquals( "ldap ssl client auth", false, roundTripped.getLdapIdentityProviderDetail().isUseSslClientClientAuthentication() );
    }

    @Test
    public void testInterfaceTagSerialization() throws Exception {
        final InterfaceTagMO interfaceTag = ManagedObjectFactory.createInterfaceTag();
        interfaceTag.setId( "1234567890" );
        interfaceTag.setVersion( 34 );
        interfaceTag.setName( "localhost" );
        interfaceTag.setAddressPatterns( list( "1", "2", "3") );
        interfaceTag.setProperties( Collections.<String,Object>singletonMap( "prop", 123 ) );

        final InterfaceTagMO roundTripped = roundTrip( interfaceTag );
        assertEquals("id", "1234567890", roundTripped.getId());
        assertEquals("version", Integer.valueOf(34), roundTripped.getVersion());
        assertEquals("name", "localhost", roundTripped.getName());
        assertEquals("address patterns", list( "1", "2", "3"), roundTripped.getAddressPatterns());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", 123 ), roundTripped.getProperties());
    }

    @Test
    public void testJDBCConnectionSerialization() throws Exception {
        final JDBCConnectionMO jdbcConnection = ManagedObjectFactory.createJDBCConnection();
        jdbcConnection.setId( "identifier" );
        jdbcConnection.setVersion( -555 );
        jdbcConnection.setEnabled( false );
        jdbcConnection.setName( "my provider" );
        jdbcConnection.setProperties( Collections.<String,Object>singletonMap( "prop", 0L ) );
        jdbcConnection.setDriverClass( "Test.cass" );
        jdbcConnection.setJdbcUrl( "jdbc://mysql/ssg" );
        jdbcConnection.setConnectionProperties( Collections.<String,Object>singletonMap( "prop2", "asdf" ) );

        final JDBCConnectionMO roundTripped = roundTrip( jdbcConnection );
        assertEquals("id", "identifier", roundTripped.getId());
        assertEquals("version", Integer.valueOf(-555), roundTripped.getVersion());
        assertEquals("enabled", false, roundTripped.isEnabled());
        assertEquals("name", "my provider", roundTripped.getName());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", 0L ), roundTripped.getProperties());
        assertEquals("driverClass", "Test.cass", roundTripped.getDriverClass());
        assertEquals("jdbcUrl", "jdbc://mysql/ssg", roundTripped.getJdbcUrl());
        assertEquals("connectionProperties", Collections.<String,Object>singletonMap( "prop2", "asdf" ), roundTripped.getConnectionProperties());
    }

    @Test
    public void testJMSDestinationSerialization() throws Exception {
        final JMSDestinationDetail jmsDestinationDetails = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDestinationDetails.setId( "1" );
        jmsDestinationDetails.setVersion( 0 );
        jmsDestinationDetails.setEnabled( true );
        jmsDestinationDetails.setName( "queue" );
        jmsDestinationDetails.setDestinationName( "queue" );
        jmsDestinationDetails.setInbound( false );
        jmsDestinationDetails.setProperties( Collections.<String,Object>singletonMap( "p1", "" ) );

        final JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setId( "1" );
        jmsConnection.setVersion( 0 );
        jmsConnection.setContextPropertiesTemplate( Collections.<String,Object>singletonMap( "p2", "v2" ) );
        jmsConnection.setProperties( Collections.<String,Object>singletonMap( "p3", "v3" ) );

        final JMSDestinationMO jmsDestination = ManagedObjectFactory.createJMSDestination();
        jmsDestination.setId( "1" );
        jmsDestination.setVersion( 0 );
        jmsDestination.setJmsDestinationDetail( jmsDestinationDetails );
        jmsDestination.setJmsConnection( jmsConnection );

        final JMSDestinationMO roundTripped = roundTrip( jmsDestination );
        assertEquals("id", "1", roundTripped.getId());
        assertEquals("version", Integer.valueOf( 0 ), roundTripped.getVersion());
        assertNotNull("details", roundTripped.getJmsDestinationDetail());
        assertNotNull("connection", roundTripped.getJmsConnection());

        assertEquals("details id", "1", roundTripped.getJmsDestinationDetail().getId());
        assertEquals("details version", Integer.valueOf( 0 ), roundTripped.getJmsDestinationDetail().getVersion());
        assertEquals("details enabled", true, roundTripped.getJmsDestinationDetail().isEnabled());
        assertEquals("details destination name", "queue", roundTripped.getJmsDestinationDetail().getDestinationName());
        assertEquals("details inbound", false, roundTripped.getJmsDestinationDetail().isInbound());
        assertEquals("details properties",Collections.<String,Object>singletonMap( "p1", "" ), roundTripped.getJmsDestinationDetail().getProperties());

        assertEquals("details id", "1", roundTripped.getJmsConnection().getId());
        assertEquals("details version", Integer.valueOf( 0 ), roundTripped.getJmsConnection().getVersion());
        assertEquals("details context properties",Collections.<String,Object>singletonMap( "p2", "v2" ), roundTripped.getJmsConnection().getContextPropertiesTemplate());
        assertEquals("details properties",Collections.<String,Object>singletonMap( "p3", "v3" ), roundTripped.getJmsConnection().getProperties());

    }

    @Test
    public void testListenPortSerialization() throws Exception {
        final ListenPortMO listenPortMO = ManagedObjectFactory.createListenPort();
        listenPortMO.setId( "3" );
        listenPortMO.setVersion( 22 );
        listenPortMO.setName( "Test" );
        listenPortMO.setEnabled( true );
        listenPortMO.setProtocol( "http" );
        listenPortMO.setInterface( "127.0.0.1" );
        listenPortMO.setPort( 8080 );
        listenPortMO.setEnabledFeatures( list( "Feature1", "Feature2" ) );
        listenPortMO.setTargetServiceId( Long.toString(Long.MAX_VALUE) );
        listenPortMO.setProperties( Collections.<String,Object>singletonMap( "a", "b" ) );

        final ListenPortMO.TlsSettings tlsSettings = ManagedObjectFactory.createTlsSettings();
        tlsSettings.setClientAuthentication( ListenPortMO.TlsSettings.ClientAuthentication.REQUIRED );
        tlsSettings.setPrivateKeyId( "-1:ssl" );
        tlsSettings.setEnabledVersions( Arrays.asList( "TLSv1", "TLSv1.1", "TLSv1.2" ) );
        tlsSettings.setEnabledCipherSuites( Arrays.asList( "TLS_RSA_WITH_AES_256_CBC_SHA" ) );
        listenPortMO.setTlsSettings( tlsSettings );

        final ListenPortMO roundTripped = roundTrip( listenPortMO );
        assertEquals("id", "3", roundTripped.getId());
        assertEquals("version", Integer.valueOf( 22 ), roundTripped.getVersion());
        assertEquals("name", "Test", roundTripped.getName());
        assertEquals("enabled", true, roundTripped.isEnabled());
        assertEquals("protocol", "http", roundTripped.getProtocol());
        assertEquals("interface", "127.0.0.1", roundTripped.getInterface());
        assertEquals("port", 8080L, (long) roundTripped.getPort() );
        assertEquals("enabled features", list("Feature1", "Feature2"), roundTripped.getEnabledFeatures() );
        assertEquals("target service id", Long.toString(Long.MAX_VALUE), roundTripped.getTargetServiceId());
        assertEquals("properties", Collections.<String,Object>singletonMap( "a", "b" ), roundTripped.getProperties());
        assertNotNull( "tls settings", roundTripped.getTlsSettings() );

        assertEquals( "tls client auth", ListenPortMO.TlsSettings.ClientAuthentication.REQUIRED, roundTripped.getTlsSettings().getClientAuthentication() );
        assertEquals( "tls private key id", "-1:ssl", roundTripped.getTlsSettings().getPrivateKeyId()  );
        assertEquals( "tls enabled versions", Arrays.asList( "TLSv1", "TLSv1.1", "TLSv1.2" ), roundTripped.getTlsSettings().getEnabledVersions()  );
        assertEquals( "tls enabled cipher suites", Arrays.asList( "TLS_RSA_WITH_AES_256_CBC_SHA" ), roundTripped.getTlsSettings().getEnabledCipherSuites()  );
    }

    /**
     * Test that an empty list of cipher suites is supported. This happens when 'Use Default List' is chosen in 'SSL/TLS Settings' tab.
     */
    @BugId("SSG-6159")
    @Test
    public void testListenPortSerialization_DefaultCipherSuites() throws Exception {
        final ListenPortMO listenPortMO = ManagedObjectFactory.createListenPort();
        listenPortMO.setId( "3" );
        listenPortMO.setVersion( 22 );
        listenPortMO.setName( "Test" );
        listenPortMO.setEnabled( true );
        listenPortMO.setProtocol( "http" );
        listenPortMO.setInterface( "127.0.0.1" );
        listenPortMO.setPort( 8080 );
        listenPortMO.setEnabledFeatures( list( "Feature1", "Feature2" ) );
        listenPortMO.setTargetServiceId( Long.toString(Long.MAX_VALUE) );
        listenPortMO.setProperties( Collections.<String,Object>singletonMap( "a", "b" ) );

        final ListenPortMO.TlsSettings tlsSettings = ManagedObjectFactory.createTlsSettings();
        tlsSettings.setClientAuthentication( ListenPortMO.TlsSettings.ClientAuthentication.REQUIRED );
        tlsSettings.setPrivateKeyId( "-1:ssl" );
        tlsSettings.setEnabledVersions( Arrays.asList( "TLSv1", "TLSv1.1", "TLSv1.2" ) );
        // do not set cipher suites!
        listenPortMO.setTlsSettings( tlsSettings );

        final ListenPortMO roundTripped = roundTrip( listenPortMO );
        assertNotNull( "tls settings", roundTripped.getTlsSettings() );
        assertTrue( "tls enabled cipher suites", roundTripped.getTlsSettings().getEnabledCipherSuites().isEmpty()  );
    }

    @Test
    public void testPolicySerialization() throws Exception {
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <test>test</test>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        final PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setId( "pol1" );
        policyDetail.setGuid( "e0913198-afbc-44f4-84e4-699ab38256c4" );
        policyDetail.setVersion( 17 );
        policyDetail.setName( "policy name" );
        policyDetail.setFolderId( "policy folder" );
        policyDetail.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
        policyDetail.setProperties( Collections.<String,Object>singletonMap( "", "value" ) );

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setType( "policy" );
        resource.setContent( policyXml );

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( "policy" );
        resourceSet.setResources( Collections.singletonList(resource) );

        final PolicyMO policy = ManagedObjectFactory.createPolicy();
        policy.setId( "pol1" );
        policy.setGuid( "e0913198-afbc-44f4-84e4-699ab38256c4" );
        policy.setVersion( 17 );
        policy.setPolicyDetail( policyDetail );
        policy.setResourceSets( Collections.singletonList( resourceSet ) );

        final PolicyMO roundTripped = roundTrip( policy );
        assertEquals("id", "pol1", roundTripped.getId());
        assertEquals("guid", "e0913198-afbc-44f4-84e4-699ab38256c4", roundTripped.getGuid());
        assertEquals("version", (Integer)17, roundTripped.getVersion());
        assertNotNull( "details", roundTripped.getPolicyDetail() );
        assertNotNull( "resource sets", roundTripped.getResourceSets() );

        assertEquals("details id", "pol1", roundTripped.getPolicyDetail().getId());
        assertEquals("details guid", "e0913198-afbc-44f4-84e4-699ab38256c4", roundTripped.getPolicyDetail().getGuid());
        assertEquals("details version", (Integer)17, roundTripped.getPolicyDetail().getVersion());
        assertEquals("details name", "policy name", roundTripped.getPolicyDetail().getName());
        assertEquals("details folder id", "policy folder", roundTripped.getPolicyDetail().getFolderId());
        assertEquals("details policy type", PolicyDetail.PolicyType.INCLUDE, roundTripped.getPolicyDetail().getPolicyType());
        assertEquals("details properties", Collections.<String,Object>singletonMap( "", "value" ), roundTripped.getPolicyDetail().getProperties());

        assertEquals( "resource sets size", 1L, (long) roundTripped.getResourceSets().size() );
        assertEquals( "resource set[0] tag", "policy", roundTripped.getResourceSets().get( 0 ).getTag() );
        assertNull( "resource set[0] root url", roundTripped.getResourceSets().get( 0 ).getRootUrl() );
        assertNotNull( "resource set[0] resources", roundTripped.getResourceSets().get( 0 ).getResources() );
        assertEquals( "resource set[0] resources size", 1L, (long) roundTripped.getResourceSets().get( 0 ).getResources().size() );
        assertEquals( "resource set[0] resources[0] type", "policy", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getType() );
        assertEquals( "resource set[0] resources[0] content", policyXml, roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getContent() );
        assertNull( "resource set[0] resources[0] id", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getId() );
        assertNull( "resource set[0] resources[0] source url", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getSourceUrl() );
    }

    @Test
    public void testPrivateKeySerialization() throws Exception {
        final PrivateKeyMO privateKey = ManagedObjectFactory.createPrivateKey();
        privateKey.setId( "1234:alias" );
        privateKey.setVersion( 13 );
        privateKey.setAlias( "alias" );
        privateKey.setKeystoreId( "1234" );
        privateKey.setCertificateChain( Arrays.asList( ManagedObjectFactory.createCertificateData( CERT_BOB_PEM )));
        privateKey.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyMO roundTripped = roundTrip( privateKey );
        assertEquals("id", "1234:alias", roundTripped.getId());
        assertEquals("version", Integer.valueOf(13), roundTripped.getVersion());
        assertEquals("alias", "alias", roundTripped.getAlias());
        assertEquals("keystore id", "1234", roundTripped.getKeystoreId());
        assertNotNull("certificate chain", roundTripped.getCertificateChain());
        assertEquals("certificate chain length", 1L, (long) roundTripped.getCertificateChain().size() );
        assertArrayEquals("certificate chain[0] encoded", HexUtils.decodeBase64( CERT_BOB_PEM, true ), roundTripped.getCertificateChain().get( 0 ).getEncoded());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeyCreationContextSerialization() throws Exception {
        final PrivateKeyCreationContext privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setId( "1234" );
        privateKeyCreationContext.setVersion( 13 );
        privateKeyCreationContext.setDn( "CN=name" );
        privateKeyCreationContext.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyCreationContext roundTripped = roundTrip( privateKeyCreationContext );
        assertEquals("id", "1234", roundTripped.getId());
        assertEquals("version", Integer.valueOf(13), roundTripped.getVersion());
        assertEquals("dn", "CN=name", roundTripped.getDn());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeyExportContextSerialization() throws Exception {
        final PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setId( "1234" );
        privateKeyExportContext.setVersion( 13 );
        privateKeyExportContext.setAlias( "alias" );
        privateKeyExportContext.setPassword( "password" );
        privateKeyExportContext.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyExportContext roundTripped = roundTrip( privateKeyExportContext );
        assertEquals("id", "1234", roundTripped.getId());
        assertEquals("version", Integer.valueOf(13), roundTripped.getVersion());
        assertEquals("alias", "alias", roundTripped.getAlias());
        assertEquals("password", "password", roundTripped.getPassword());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeyExportResultSerialization() throws Exception {
        final PrivateKeyExportResult privateKeyExportResult = new PrivateKeyExportResult();
        privateKeyExportResult.setId( "1234" );
        privateKeyExportResult.setVersion( 13 );
        privateKeyExportResult.setPkcs12Data( new byte[]{ (byte) 0, (byte) 1, (byte) 0, (byte) 1 } );
        privateKeyExportResult.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyExportResult roundTripped = roundTrip( privateKeyExportResult );
        assertEquals("id", "1234", roundTripped.getId());
        assertEquals("version", Integer.valueOf(13), roundTripped.getVersion());
        assertNotNull( "pkcs12Data", roundTripped.getPkcs12Data() );
        assertArrayEquals( "pkcs12Data", new byte[]{ (byte) 0, (byte) 1, (byte) 0, (byte) 1 }, roundTripped.getPkcs12Data() );
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeyGenerateCsrContextSerialization() throws Exception {
        final PrivateKeyGenerateCsrContext privateKeyGenerateCsrContext = new PrivateKeyGenerateCsrContext();
        privateKeyGenerateCsrContext.setId( "1234" );
        privateKeyGenerateCsrContext.setVersion( 13 );
        privateKeyGenerateCsrContext.setDn( "cn=test" );
        privateKeyGenerateCsrContext.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyGenerateCsrContext roundTripped = roundTrip( privateKeyGenerateCsrContext );
        assertEquals("id", "1234", roundTripped.getId());
        assertEquals( "version", Integer.valueOf( 13 ), roundTripped.getVersion() );
        assertEquals( "dn", "cn=test", roundTripped.getDn() );
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeyGenerateCsrResultSerialization() throws Exception {
        final PrivateKeyGenerateCsrResult privateKeyGenerateCsrResult = new PrivateKeyGenerateCsrResult();
        privateKeyGenerateCsrResult.setId( "1234" );
        privateKeyGenerateCsrResult.setVersion( 13 );
        privateKeyGenerateCsrResult.setCsrData( new byte[]{ (byte) 0, (byte) 1, (byte) 0, (byte) 1 } );
        privateKeyGenerateCsrResult.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyGenerateCsrResult roundTripped = roundTrip( privateKeyGenerateCsrResult );
        assertEquals("id", "1234", roundTripped.getId());
        assertEquals( "version", Integer.valueOf( 13 ), roundTripped.getVersion() );
        assertNotNull( "csrData", roundTripped.getCsrData() );
        assertArrayEquals( "csrData", new byte[]{ (byte) 0, (byte) 1, (byte) 0, (byte) 1 }, roundTripped.getCsrData() );
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeyImportContextSerialization() throws Exception {
        final PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setId( "1234" );
        privateKeyImportContext.setVersion( 13 );
        privateKeyImportContext.setAlias( "alias" );
        privateKeyImportContext.setPassword( "password" );
        privateKeyImportContext.setPkcs12Data( new byte[]{ (byte) 0, (byte) 1, (byte) 0, (byte) 1 } );
        privateKeyImportContext.setProperties( Collections.<String,Object>singletonMap( "prop", false ) );

        final PrivateKeyImportContext roundTripped = roundTrip( privateKeyImportContext );
        assertEquals("id", "1234", roundTripped.getId());
        assertEquals("version", Integer.valueOf(13), roundTripped.getVersion());
        assertEquals("alias", "alias", roundTripped.getAlias());
        assertEquals("password", "password", roundTripped.getPassword());
        assertNotNull( "pkcs12Data", roundTripped.getPkcs12Data() );
        assertArrayEquals( "pkcs12Data", new byte[]{ (byte) 0, (byte) 1, (byte) 0, (byte) 1 }, roundTripped.getPkcs12Data() );
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testPrivateKeySpecialPurposeRequestSerialization() throws Exception {
        final PrivateKeySpecialPurposeContext privateKeySpecialPurposeRequest = new PrivateKeySpecialPurposeContext();
        privateKeySpecialPurposeRequest.setId( "1234" );
        privateKeySpecialPurposeRequest.setVersion( 13 );
        privateKeySpecialPurposeRequest.setSpecialPurposes( list( "1", "2", "3" ) );
        privateKeySpecialPurposeRequest.setProperties( Collections.<String, Object>singletonMap( "prop", false ) );

        final PrivateKeySpecialPurposeContext roundTripped = roundTrip( privateKeySpecialPurposeRequest );
        assertEquals( "id", "1234", roundTripped.getId() );
        assertEquals("version", Integer.valueOf(13), roundTripped.getVersion());
        assertEquals( "specialPurposes", list( "1", "2", "3" ), roundTripped.getSpecialPurposes() );
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", false ), roundTripped.getProperties());
    }

    @Test
    public void testResourceDocumentSerialization() throws Exception {
        final Resource resource = ManagedObjectFactory.createResource();
        resource.setId( "r1" );
        resource.setType( "xmlschema" );
        resource.setSourceUrl( "http://www.example.org/example.xsd" );
        resource.setContent( "<?xml version=\"1.0\"?><!-- \n -->\n<xs:schema targetNamespace=\"http://www.example.org/example.xsd\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" blockDefault=\"#all\"/>" );

        final ResourceDocumentMO resourceDocument = ManagedObjectFactory.createResourceDocument();
        resourceDocument.setId( "001" );
        resourceDocument.setVersion( Integer.MAX_VALUE );
        resourceDocument.setResource( resource );
        resourceDocument.setProperties( Collections.<String,Object>singletonMap( "prop", "value" ) );

        final ResourceDocumentMO roundTripped = roundTrip( resourceDocument );
        assertEquals("id", "001", roundTripped.getId());
        assertEquals("version", (Integer)Integer.MAX_VALUE, roundTripped.getVersion());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", "value" ), roundTripped.getProperties());
        assertNotNull("resource", roundTripped.getResource());

        assertEquals("resource id", "r1", roundTripped.getResource().getId());
        assertEquals("resource type", "xmlschema", roundTripped.getResource().getType());
        assertEquals("resource sourceUrl", "http://www.example.org/example.xsd", roundTripped.getResource().getSourceUrl());
        assertEquals("resource content", "<?xml version=\"1.0\"?><!-- \n -->\n<xs:schema targetNamespace=\"http://www.example.org/example.xsd\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" blockDefault=\"#all\"/>", roundTripped.getResource().getContent());
    }

    @Test
    public void testRevocationCheckingPolicySerialization() throws Exception {
        final RevocationCheckingPolicyMO policy = ManagedObjectFactory.createRevocationCheckingPolicy();
        policy.setId( "4" );
        policy.setVersion( 5 );
        policy.setName( "Policy" );
        policy.setProperties( Collections.<String,Object>singletonMap( "prop", "value" ) );

        final RevocationCheckingPolicyMO roundTripped = roundTrip( policy );
        assertEquals("id", "4", roundTripped.getId());
        assertEquals("version", (Integer)5, roundTripped.getVersion());
        assertEquals( "name", "Policy", policy.getName() );
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", "value" ), roundTripped.getProperties());
    }

    @Test
    public void testServiceSerialization() throws Exception {
        final String policyXml = "<Policy><PolicyContent/>                                                                                                         </Policy>";

        final ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setId( "1" );
        serviceDetail.setVersion( 3 );
        serviceDetail.setEnabled(true);
        serviceDetail.setName("TestService");
        serviceDetail.setServiceMappings( Arrays.asList(
            ManagedObjectFactory.createHttpMapping(),
            ManagedObjectFactory.createSoapMapping()
        ));
        serviceDetail.setProperties(new HashMap<String,Object>(){{
            put("wss.enabled", Boolean.TRUE);
            put("soap", Boolean.FALSE);
        }});
        ((ServiceDetail.HttpMapping)serviceDetail.getServiceMappings().get( 0 )).setUrlPattern( "/test" );
        ((ServiceDetail.HttpMapping)serviceDetail.getServiceMappings().get( 0 )).setVerbs( Arrays.asList("POST") );

        final Resource policyResource = ManagedObjectFactory.createResource();
        policyResource.setType("policy");
        policyResource.setContent( policyXml );

        final List<ResourceSet> resourceSets = new ArrayList<ResourceSet>();
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSets.add( resourceSet );
        final List<Resource> resources = new ArrayList<Resource>();
        resources.add( policyResource );
        resourceSet.setTag( "policy" );
        resourceSet.setResources( resources );

        final ServiceMO service = ManagedObjectFactory.createService();
        service.setId( "1" );
        service.setVersion( 1 );
        service.setServiceDetail( serviceDetail );
        service.setResourceSets( resourceSets );

        final ServiceMO roundTripped = roundTrip( service );

        assertEquals("id", "1", roundTripped.getId());
        assertEquals("version", (Integer)3, roundTripped.getVersion());
        assertNotNull("service detail", roundTripped.getServiceDetail());
        assertNotNull("resource sets", roundTripped.getResourceSets());

        assertEquals("detail id", "1", roundTripped.getServiceDetail().getId());
        assertEquals("detail version", (Integer)3, roundTripped.getServiceDetail().getVersion());
        assertEquals("detail enabled", true, roundTripped.getServiceDetail().getEnabled());
        assertEquals("detail name", "TestService", roundTripped.getServiceDetail().getName());
        assertNotNull("detail service mapping", roundTripped.getServiceDetail().getServiceMappings());
        assertEquals("detail service mapping size", 2L, (long) roundTripped.getServiceDetail().getServiceMappings().size() );

        assertEquals("detail service mapping[0] url pattern", "/test", ((ServiceDetail.HttpMapping)roundTripped.getServiceDetail().getServiceMappings().get(0)).getUrlPattern());
        assertEquals("detail service mapping[0] url verbs", Arrays.asList("POST"), ((ServiceDetail.HttpMapping)roundTripped.getServiceDetail().getServiceMappings().get(0)).getVerbs());
        assertEquals("detail service mapping[1] lax", false, ((ServiceDetail.SoapMapping)roundTripped.getServiceDetail().getServiceMappings().get( 1 )).isLax());

        assertEquals("detail props", new HashMap<String,Object>(){{
            put("wss.enabled", Boolean.TRUE);
            put("soap", Boolean.FALSE);
        }}, roundTripped.getServiceDetail().getProperties());

        assertEquals( "resource sets size", 1L, (long) roundTripped.getResourceSets().size() );
        assertEquals( "resource set[0] tag", "policy", roundTripped.getResourceSets().get( 0 ).getTag() );
        assertNull( "resource set[0] root url", roundTripped.getResourceSets().get( 0 ).getRootUrl() );
        assertNotNull( "resource set[0] resources", roundTripped.getResourceSets().get( 0 ).getResources() );
        assertEquals( "resource set[0] resources size", 1L, (long) roundTripped.getResourceSets().get( 0 ).getResources().size() );
        assertEquals( "resource set[0] resources[0] type", "policy", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getType() );
        assertEquals( "resource set[0] resources[0] content", policyXml, roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getContent() );
        assertNull( "resource set[0] resources[0] id", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getId() );
        assertNull( "resource set[0] resources[0] source url", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getSourceUrl() );
    }

    @Test
    public void testStorePasswordSerialization() throws Exception {
        final StoredPasswordMO storedPassword = ManagedObjectFactory.createStoredPassword();
        final Date updated = new Date();
        storedPassword.setId( "0" );
        storedPassword.setVersion( Integer.MAX_VALUE );
        storedPassword.setName( "test" );
        storedPassword.setPassword( "password" );
        storedPassword.setProperties( Collections.<String, Object>singletonMap( "lastUpdated", updated) );

        final StoredPasswordMO roundTripped = roundTrip( storedPassword );
        assertEquals("id", "0", roundTripped.getId());
        assertEquals("version", (Integer)Integer.MAX_VALUE, roundTripped.getVersion());
        assertEquals("name", "test", roundTripped.getName());
        assertEquals("properties", Collections.<String,Object>singletonMap( "lastUpdated", updated ), roundTripped.getProperties());
    }
    
    @Test
    public void testStoredPasswordCompatiblePre83Marshalling() throws Exception {
        // tests that
        final String xml =
                "<l7:StoredPassword id=\"7160eb7213c92e2e5df2690897ac67a9\" version=\"1\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:Name>test</l7:Name>\n" +
                        "    <l7:Password>password</l7:Password>\n" +
                        "    <l7:Properties>\n" +
                        "        <l7:Property key=\"description\">\n" +
                        "            <l7:StringValue>password</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"lastUpdated\">\n" +
                        "            <l7:DateValue>2015-01-07T12:22:52.906-08:00</l7:DateValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"type\">\n" +
                        "            <l7:StringValue>Password</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "    </l7:Properties>\n" +
                        "</l7:StoredPassword>";

        final StoredPasswordMO storedPassword = ManagedObjectFactory.read(xml,StoredPasswordMO.class);

        assertEquals("id","7160eb7213c92e2e5df2690897ac67a9", storedPassword.getId());
        assertEquals("version",1, storedPassword.getVersion().intValue());
        assertEquals("password","password", storedPassword.getPassword());
        assertEquals("password key",null, storedPassword.getPasswordBundleKey());
        assertEquals("name", "test", storedPassword.getName());
    }

    @Test
    public void testStorePasswordWithEncryptedPassword() throws Exception {
        final StoredPasswordMO storedPassword = ManagedObjectFactory.createStoredPassword();
        storedPassword.setPassword("pass","key");
        storedPassword.setName("name");
        storedPassword.setVersion(3);
        storedPassword.setProperties(CollectionUtils.<String,Object>mapBuilder().put("key","value").map());
        StoredPasswordMO passwordRoundtrip = ManagedObjectFactory.read(ManagedObjectFactory.write(storedPassword),StoredPasswordMO.class);

        Assert.assertEquals("id", storedPassword.getId(), passwordRoundtrip.getId());
        Assert.assertEquals("version", storedPassword.getVersion().intValue(), passwordRoundtrip.getVersion().intValue());
        Assert.assertEquals("password", storedPassword.getPassword(), passwordRoundtrip.getPassword());
        Assert.assertEquals("password key", storedPassword.getPasswordBundleKey(), passwordRoundtrip.getPasswordBundleKey());
        Assert.assertEquals("name", storedPassword.getName(), passwordRoundtrip.getName());
    }

    @Test
    public void testTrustedCertificateSerialization() throws Exception {
        final TrustedCertificateMO trustedCertificate = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificate.setId( "0" );
        trustedCertificate.setVersion( Integer.MIN_VALUE );
        trustedCertificate.setName( "Cert" );
        trustedCertificate.setCertificateData( ManagedObjectFactory.createCertificateData( cert(CERT_BOB_PEM) ) );
        trustedCertificate.setRevocationCheckingPolicyId( "3" );
        trustedCertificate.setProperties( Collections.<String,Object>singletonMap( "prop", null ) );

        final TrustedCertificateMO roundTripped = roundTrip( trustedCertificate );
        assertEquals("id", "0", roundTripped.getId());
        assertEquals("version", (Integer)Integer.MIN_VALUE, roundTripped.getVersion());
        assertEquals("name", "Cert", roundTripped.getName());
        assertEquals("properties", Collections.<String,Object>singletonMap( "prop", null ), roundTripped.getProperties());
        assertNotNull("certificate data", roundTripped.getCertificateData());
        assertEquals("revocationCheckingPolicyId", "3", roundTripped.getRevocationCheckingPolicyId());

        assertEquals("certificate issuer name", "CN=OASIS Interop Test CA,O=OASIS", roundTripped.getCertificateData().getIssuerName());
        assertEquals("certificate serial number", new BigInteger( "127901500862700997089151460209364726264" ), roundTripped.getCertificateData().getSerialNumber());
        assertEquals("certificate subject name", "CN=Bob,OU=OASIS Interop Test Cert,O=OASIS", roundTripped.getCertificateData().getSubjectName());
        assertArrayEquals("certificate encoded", HexUtils.decodeBase64( CERT_BOB_PEM, true ), roundTripped.getCertificateData().getEncoded());
    }

    @Test
    public void testPolicyExportResultSerialization() throws Exception {
        final String policyExport =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <test>test</test>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>";

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setId( "a" );
        resource.setVersion( 4 );
        resource.setSourceUrl( "urn:test:url" );
        resource.setType( "policyexport" );
        resource.setContent( policyExport );

        final PolicyExportResult policyExportResult = ManagedObjectFactory.createPolicyExportResult();
        policyExportResult.setId( "1" );
        policyExportResult.setVersion( 2 );
        policyExportResult.setResource( resource );

        final PolicyExportResult roundTripped = roundTrip( policyExportResult );
        assertEquals("id", "1", roundTripped.getId());
        assertEquals("version", (Integer)2, roundTripped.getVersion());
        assertNotNull("resource", roundTripped.getResource());
        assertEquals("resource id", "a", roundTripped.getResource().getId());
        assertEquals("resource version", (Integer)4, roundTripped.getResource().getVersion());
        assertEquals("resource source url", "urn:test:url", roundTripped.getResource().getSourceUrl());
        assertEquals("resource type", "policyexport", roundTripped.getResource().getType());
        assertEquals("resource content", policyExport, roundTripped.getResource().getContent());
    }

    @Test
    public void testPolicyImportContextSerialization() throws Exception {
        final String policyExport =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <test>test</test>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>";

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setId( "b" );
        resource.setVersion( 4 );
        resource.setSourceUrl( "urn:test:url" );
        resource.setType( "policyexport" );
        resource.setContent( policyExport );

        final List<PolicyReferenceInstruction> instructions =
                new ArrayList<PolicyReferenceInstruction>();
        final PolicyReferenceInstruction instruction1 = ManagedObjectFactory.createPolicyReferenceInstruction();
        instruction1.setReferenceId( "1" );
        instruction1.setReferenceType( "com.l7tech.console.policy.exporter.IdProviderReference" );
        instruction1.setPolicyReferenceInstructionType( PolicyReferenceInstruction.PolicyReferenceInstructionType.DELETE );
        instructions.add( instruction1 );
        final PolicyReferenceInstruction instruction2 = ManagedObjectFactory.createPolicyReferenceInstruction();
        instruction2.setReferenceId( "2:alias" );
        instruction2.setReferenceType( "com.l7tech.console.policy.exporter.PrivateKeyReference" );
        instruction2.setPolicyReferenceInstructionType( PolicyReferenceInstruction.PolicyReferenceInstructionType.IGNORE );
        instructions.add( instruction2 );

        final PolicyImportContext policyImportContext = new PolicyImportContext();
        policyImportContext.setId( "1" );
        policyImportContext.setVersion( 2 );
        policyImportContext.setProperties( Collections.<String,Object>singletonMap("force", true) );
        policyImportContext.setResource( resource );
        policyImportContext.setPolicyReferenceInstructions( instructions );

        final PolicyImportContext roundTripped = roundTrip( policyImportContext );
        assertEquals("id", "1", roundTripped.getId());
        assertEquals("version", (Integer)2, roundTripped.getVersion());
        assertEquals("properties", Collections.<String,Object>singletonMap("force", true), roundTripped.getProperties());
        assertNotNull("resource", roundTripped.getResource());
        assertEquals("resource id", "b", roundTripped.getResource().getId());
        assertEquals("resource version", (Integer)4, roundTripped.getResource().getVersion());
        assertEquals("resource source url", "urn:test:url", roundTripped.getResource().getSourceUrl());
        assertEquals("resource type", "policyexport", roundTripped.getResource().getType());
        assertEquals("resource content", policyExport, roundTripped.getResource().getContent());
        assertNotNull("instructions", roundTripped.getPolicyReferenceInstructions());
        assertEquals("instructions", 2L, (long) roundTripped.getPolicyReferenceInstructions().size() );
        assertNotNull("instructions[0]", roundTripped.getPolicyReferenceInstructions().get( 0 ));
        assertEquals("instructions[0] reference id", "1", roundTripped.getPolicyReferenceInstructions().get( 0 ).getReferenceId());
        assertEquals("instructions[0] reference type", "com.l7tech.console.policy.exporter.IdProviderReference", roundTripped.getPolicyReferenceInstructions().get( 0 ).getReferenceType());
        assertEquals("instructions[0] instruction type", PolicyReferenceInstruction.PolicyReferenceInstructionType.DELETE, roundTripped.getPolicyReferenceInstructions().get( 0 ).getPolicyReferenceInstructionType());
        assertNotNull("instructions[1]", roundTripped.getPolicyReferenceInstructions().get( 0 ));
        assertEquals("instructions[1] reference id", "2:alias", roundTripped.getPolicyReferenceInstructions().get( 1 ).getReferenceId());
        assertEquals("instructions[1] reference type", "com.l7tech.console.policy.exporter.PrivateKeyReference", roundTripped.getPolicyReferenceInstructions().get( 1 ).getReferenceType());
        assertEquals("instructions[1] instruction type", PolicyReferenceInstruction.PolicyReferenceInstructionType.IGNORE, roundTripped.getPolicyReferenceInstructions().get( 1 ).getPolicyReferenceInstructionType());
    }

    @Test
    public void testPolicyImportResultSerialization() throws Exception {
        final List<PolicyImportResult.ImportedPolicyReference> references = new ArrayList<PolicyImportResult.ImportedPolicyReference>();
        final PolicyImportResult.ImportedPolicyReference reference1 = ManagedObjectFactory.createImportedPolicyReference();
        reference1.setId( "1" );
        reference1.setReferenceType( "com.l7tech.console.policy.exporter.IncludedPolicyReference" );
        reference1.setReferenceId( "123" );
        reference1.setType( PolicyImportResult.ImportedPolicyReferenceType.CREATED );
        reference1.setGuid( "guidhere" );
        references.add( reference1 );
        final PolicyImportResult.ImportedPolicyReference reference2 = ManagedObjectFactory.createImportedPolicyReference();
        reference2.setId( "2" );
        reference2.setReferenceType( "com.l7tech.console.policy.exporter.IdProviderReference" );
        reference2.setReferenceId( "12" );
        reference2.setType( PolicyImportResult.ImportedPolicyReferenceType.MAPPED );
        references.add( reference2 );

        final PolicyImportResult policyImportResult = ManagedObjectFactory.createPolicyImportResult();
        policyImportResult.setId( "a1" );
        policyImportResult.setVersion( 20 );
        policyImportResult.setWarnings( Arrays.asList( "Unable to do X", "Error with Y" ));
        policyImportResult.setImportedPolicyReferences( references );

        final PolicyImportResult roundTripped = roundTrip( policyImportResult );
        assertEquals("id", "a1", roundTripped.getId());
        assertEquals("version", (Integer)20, roundTripped.getVersion());
        assertEquals("warnings", Arrays.asList( "Unable to do X", "Error with Y" ), roundTripped.getWarnings());
        assertNotNull("references", roundTripped.getImportedPolicyReferences());
        assertEquals("references", 2L, (long) roundTripped.getImportedPolicyReferences().size() );
        assertNotNull("references[0]", roundTripped.getImportedPolicyReferences().get( 0 ));
        assertEquals("references[0] id", "1", roundTripped.getImportedPolicyReferences().get( 0 ).getId());
        assertEquals("references[0] reference id", "123", roundTripped.getImportedPolicyReferences().get( 0 ).getReferenceId());
        assertEquals("references[0] reference type", "com.l7tech.console.policy.exporter.IncludedPolicyReference", roundTripped.getImportedPolicyReferences().get( 0 ).getReferenceType());
        assertEquals("references[0] type", PolicyImportResult.ImportedPolicyReferenceType.CREATED, roundTripped.getImportedPolicyReferences().get( 0 ).getType());
        assertEquals("references[0] guid", "guidhere", roundTripped.getImportedPolicyReferences().get( 0 ).getGuid());
        assertNotNull("references[1]", roundTripped.getImportedPolicyReferences().get( 1 ));
        assertEquals("references[1] id", "2", roundTripped.getImportedPolicyReferences().get( 1 ).getId());
        assertEquals("references[1] reference id", "12", roundTripped.getImportedPolicyReferences().get( 1 ).getReferenceId());
        assertEquals("references[1] reference type", "com.l7tech.console.policy.exporter.IdProviderReference", roundTripped.getImportedPolicyReferences().get( 1 ).getReferenceType());
        assertEquals("references[1] type", PolicyImportResult.ImportedPolicyReferenceType.MAPPED, roundTripped.getImportedPolicyReferences().get( 1 ).getType());
        assertNull("references[1] guid", roundTripped.getImportedPolicyReferences().get( 1 ).getGuid());
    }

    @Test
    public void testPolicyValidationContextSerialization() throws Exception {
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <test>test</test>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        final Resource policyResource = ManagedObjectFactory.createResource();
        policyResource.setType("policy");
        policyResource.setContent( policyXml );

        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( "policy" );
        resourceSet.setResources( Arrays.asList( policyResource ) );

        final PolicyValidationContext policyValidationContext = new PolicyValidationContext();
        policyValidationContext.setId( "123" );
        policyValidationContext.setVersion( 3331 );
        policyValidationContext.setProperties( Collections.<String,Object>singletonMap( "soap", true ) );
        policyValidationContext.setPolicyType( PolicyDetail.PolicyType.INCLUDE );
        policyValidationContext.setResourceSets( Arrays.asList( resourceSet ));

        final PolicyValidationContext roundTripped = roundTrip( policyValidationContext );
        assertEquals("id", "123", roundTripped.getId());
        assertEquals("version", (Integer)3331, roundTripped.getVersion());
        assertEquals("properties", Collections.<String,Object>singletonMap( "soap", true ), roundTripped.getProperties());
        assertEquals("policy type", PolicyDetail.PolicyType.INCLUDE, roundTripped.getPolicyType());
        assertEquals( "resource sets size", 1L, (long) roundTripped.getResourceSets().size() );
        assertEquals( "resource set[0] tag", "policy", roundTripped.getResourceSets().get( 0 ).getTag() );
        assertNull( "resource set[0] root url", roundTripped.getResourceSets().get( 0 ).getRootUrl() );
        assertNotNull( "resource set[0] resources", roundTripped.getResourceSets().get( 0 ).getResources() );
        assertEquals( "resource set[0] resources size", 1L, (long) roundTripped.getResourceSets().get( 0 ).getResources().size() );
        assertEquals( "resource set[0] resources[0] type", "policy", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getType() );
        assertEquals( "resource set[0] resources[0] content", policyXml, roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getContent() );
        assertNull( "resource set[0] resources[0] id", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getId() );
        assertNull( "resource set[0] resources[0] source url", roundTripped.getResourceSets().get( 0 ).getResources().get( 0 ).getSourceUrl() );
    }

    @Test
    public void testPolicyValidationResultSerialization() throws Exception {
        final List<PolicyValidationResult.PolicyValidationMessage> messages = new ArrayList<PolicyValidationResult.PolicyValidationMessage>();
        final PolicyValidationResult.AssertionDetail detail1 = ManagedObjectFactory.createAssertionDetail();
        detail1.setDescription( "Some assertion" );
        detail1.setPosition( 3 );
        final PolicyValidationResult.PolicyValidationMessage message1 = ManagedObjectFactory.createPolicyValidationMessage();
        message1.setMessage( "Something wrong" );
        message1.setAssertionOrdinal( 7 );
        message1.setLevel( "Warning" );
        message1.setAssertionDetails( Arrays.asList( detail1 ) );
        messages.add( message1 );

        final PolicyValidationResult.AssertionDetail detail2 = ManagedObjectFactory.createAssertionDetail();
        detail2.setDescription( "Some other assertion" );
        detail2.setPosition( 4 );
        final PolicyValidationResult.AssertionDetail detail3 = ManagedObjectFactory.createAssertionDetail();
        detail3.setDescription( "An assertion" );
        detail3.setPosition( 0 );
        final PolicyValidationResult.PolicyValidationMessage message2 = ManagedObjectFactory.createPolicyValidationMessage();
        message2.setMessage( "Something else wrong" );
        message2.setAssertionOrdinal( 9 );
        message2.setLevel( "Warning" );
        message2.setAssertionDetails( Arrays.asList( detail2, detail3 ) );
        messages.add( message2 );

        final PolicyValidationResult policyValidationResult = ManagedObjectFactory.createPolicyValidationResult();
        policyValidationResult.setId( "asf" );
        policyValidationResult.setVersion( 0 );
        policyValidationResult.setStatus( PolicyValidationResult.ValidationStatus.WARNING );
        policyValidationResult.setPolicyValidationMessages( messages );

        final PolicyValidationResult roundTripped = roundTrip( policyValidationResult );
        assertEquals("id", "asf", roundTripped.getId());
        assertEquals("version", (Integer)0, roundTripped.getVersion());
        assertEquals("status", PolicyValidationResult.ValidationStatus.WARNING , roundTripped.getStatus());
        assertNotNull("messages", policyValidationResult.getPolicyValidationMessages());
        assertEquals("messages size", 2L, (long) policyValidationResult.getPolicyValidationMessages().size() );
        assertEquals("messages[0] message", "Something wrong", policyValidationResult.getPolicyValidationMessages().get( 0 ).getMessage());
        assertEquals("messages[0] ordinal", 7L, (long) policyValidationResult.getPolicyValidationMessages().get( 0 ).getAssertionOrdinal() );
        assertEquals("messages[0] level", "Warning", policyValidationResult.getPolicyValidationMessages().get( 0 ).getLevel());
        assertNotNull("messages[0] details", policyValidationResult.getPolicyValidationMessages().get( 0 ).getAssertionDetails());
        assertEquals("messages[0] details size", 1L, (long) policyValidationResult.getPolicyValidationMessages().get( 0 ).getAssertionDetails().size() );
        assertEquals("messages[0] details[0] description", "Some assertion", policyValidationResult.getPolicyValidationMessages().get( 0 ).getAssertionDetails().get( 0 ).getDescription());
        assertEquals("messages[0] details[0] position", 3L, (long) policyValidationResult.getPolicyValidationMessages().get( 0 ).getAssertionDetails().get( 0 ).getPosition() );
        assertEquals("messages[1] message", "Something else wrong", policyValidationResult.getPolicyValidationMessages().get( 1 ).getMessage());
        assertEquals("messages[1] ordinal", 9L, (long) policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionOrdinal() );
        assertEquals("messages[1] level", "Warning", policyValidationResult.getPolicyValidationMessages().get( 1 ).getLevel());
        assertNotNull("messages[1] details", policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionDetails());
        assertEquals("messages[1] details size", 2L, (long) policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionDetails().size() );
        assertEquals("messages[1] details[0] description", "Some other assertion", policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionDetails().get( 0 ).getDescription());
        assertEquals("messages[1] details[0] position", 4L, (long) policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionDetails().get( 0 ).getPosition() );
        assertEquals("messages[1] details[1] description", "An assertion", policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionDetails().get( 1 ).getDescription());
        assertEquals("messages[1] details[1] position", 0L, (long) policyValidationResult.getPolicyValidationMessages().get( 1 ).getAssertionDetails().get( 1 ).getPosition() );
    }

    @Test
    public void testEncapsulatedAssertionSerialization() throws Exception {
        final EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setId( "3" );
        encapsulatedAssertionMO.setVersion( 77 );
        encapsulatedAssertionMO.setName( "Test" );
        encapsulatedAssertionMO.setGuid( "ddc3c2d8-fba6-4a86-9707-4e0c08bd0e60" );
        encapsulatedAssertionMO.setProperties( Collections.<String,String>singletonMap( "a", "b" ));
        encapsulatedAssertionMO.setPolicyReference( new ManagedObjectReference(PolicyMO.class, "1011") );

        final EncapsulatedAssertionMO.EncapsulatedArgument arg = new EncapsulatedAssertionMO.EncapsulatedArgument();
        arg.setOrdinal( 1 );
        arg.setArgumentName("input1");
        arg.setArgumentType("decimal");
        arg.setGuiPrompt(true);
        arg.setGuiLabel("Input1 Label");
        final EncapsulatedAssertionMO.EncapsulatedArgument arg2 = new EncapsulatedAssertionMO.EncapsulatedArgument();
        arg2.setOrdinal( 2 );
        arg2.setArgumentName("input2");
        arg2.setArgumentType("string");
        arg2.setGuiPrompt(false);
        arg2.setGuiLabel("Input2 Label");
        encapsulatedAssertionMO.setEncapsulatedArguments( list(arg, arg2) );

        final EncapsulatedAssertionMO.EncapsulatedResult result = new EncapsulatedAssertionMO.EncapsulatedResult();
        result.setResultName("result1");
        result.setResultType( "boolean" );
        final EncapsulatedAssertionMO.EncapsulatedResult result2 = new EncapsulatedAssertionMO.EncapsulatedResult();
        result2.setResultName("result2");
        result2.setResultType( "string" );
        final EncapsulatedAssertionMO.EncapsulatedResult result3 = new EncapsulatedAssertionMO.EncapsulatedResult();
        result3.setResultName("result3");
        result3.setResultType( "message" );
        encapsulatedAssertionMO.setEncapsulatedResults( list(result, result2, result3) );

        final EncapsulatedAssertionMO roundTripped = roundTrip( encapsulatedAssertionMO );

        assertEquals( "id", "3", roundTripped.getId() );
        assertEquals( "version", Integer.valueOf( 77 ), roundTripped.getVersion() );
        assertEquals( "name", "Test", roundTripped.getName() );
        assertEquals( "guid", "ddc3c2d8-fba6-4a86-9707-4e0c08bd0e60", roundTripped.getGuid() );
        assertEquals( "properties", Collections.<String,String>singletonMap( "a", "b" ), roundTripped.getProperties() );
        assertEquals( "policy reference resource uri", AccessorSupport.getResourceUri(PolicyMO.class), roundTripped.getPolicyReference().getResourceUri() );
        assertEquals( "policy reference id", "1011", roundTripped.getPolicyReference().getId() );

        assertEquals( "property name", "a", roundTripped.getProperties().keySet().iterator().next() );
        assertEquals( "property value", "b", roundTripped.getProperties().values().iterator().next() );

        assertEquals( "arg ordinal", 1, roundTripped.getEncapsulatedArguments().iterator().next().getOrdinal() );
        assertEquals( "arg name", "input1", roundTripped.getEncapsulatedArguments().iterator().next().getArgumentName() );
        assertEquals( "arg type", "decimal", roundTripped.getEncapsulatedArguments().iterator().next().getArgumentType() );
        assertEquals( "arg gui prompt", true, roundTripped.getEncapsulatedArguments().iterator().next().isGuiPrompt() );
        assertEquals( "arg gui label", "Input1 Label", roundTripped.getEncapsulatedArguments().iterator().next().getGuiLabel() );

        assertEquals( "result name", "result1", roundTripped.getEncapsulatedResults().iterator().next().getResultName() );
        assertEquals( "result type", "boolean", roundTripped.getEncapsulatedResults().iterator().next().getResultType() );
    }

    @Test
    public void testGenericEntitySerialization() throws Exception {
        debug = true;
        final GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("Test");
        genericEntityMO.setDescription("Test description");
        genericEntityMO.setEnabled(true);
        genericEntityMO.setEntityClassName("com.l7tech.gateway.api.ManagedObjectTest");
        genericEntityMO.setValueXml("<xml>Test value</xml>");

        final GenericEntityMO roundTripped = roundTrip(genericEntityMO);

        assertEquals("Test", roundTripped.getName());
        assertEquals("Test description", roundTripped.getDescription());
        assertEquals(true, roundTripped.getEnabled());
        assertEquals("com.l7tech.gateway.api.ManagedObjectTest", roundTripped.getEntityClassName());
        assertEquals("<xml>Test value</xml>", roundTripped.getValueXml());
    }

    @Test
    public void testCustomKeyValueStoreSerialization() throws Exception {
        final CustomKeyValueStoreMO customKeyValueStoreMO = ManagedObjectFactory.createCustomKeyValueStore();
        customKeyValueStoreMO.setStoreName("internalTransactional");
        customKeyValueStoreMO.setKey("key.prefix.key1");
        customKeyValueStoreMO.setValue("<xml>Test value</xml>".getBytes("UTF-8"));

        final CustomKeyValueStoreMO roundTripped = roundTrip(customKeyValueStoreMO);

        assertEquals("key.prefix.key1", roundTripped.getKey());
        assertEquals(true, Arrays.equals("<xml>Test value</xml>".getBytes("UTF-8"), roundTripped.getValue()));
    }

    @Test
    public void testMapSerialization() throws Exception {
        final Map<String,Object> properties = new HashMap<String,Object>();
        properties.put( "1", "string value" );
        properties.put( "2", true );
        properties.put( "3", 1 );
        properties.put( "4", 1L );
        properties.put( "5", 4.4 );

        final TrustedCertificateMO trustedCertificate = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificate.setName( "Name" );
        trustedCertificate.setCertificateData( ManagedObjectFactory.createCertificateData() );
        trustedCertificate.getCertificateData().setEncoded( new byte[]{ (byte) 0 } );
        trustedCertificate.setProperties( properties );

        final TrustedCertificateMO roundTripped = roundTrip( trustedCertificate, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals("String values", 1L, (long) document.getElementsByTagNameNS( MANAGEMENT_NS, "StringValue" ).getLength() );
                assertEquals("Boolean values", 1L, (long) document.getElementsByTagNameNS( MANAGEMENT_NS, "BooleanValue" ).getLength() );
                assertEquals("Integer values", 1L, (long) document.getElementsByTagNameNS( MANAGEMENT_NS, "IntegerValue" ).getLength() );
                assertEquals("Long values", 1L, (long) document.getElementsByTagNameNS( MANAGEMENT_NS, "LongValue" ).getLength() );
                assertEquals("Object values", 1L, (long) document.getElementsByTagNameNS( MANAGEMENT_NS, "Value" ).getLength() );
            }
        } );
        final Map<String,Object> rtp = roundTripped.getProperties();
        assertEquals( "1", "string value", rtp.get("1"));
        assertEquals( "2", true, rtp.get("2"));
        assertEquals( "3", 1, rtp.get("3"));
        assertEquals( "4", 1L, rtp.get("4"));
        assertEquals( "5", 4.4, rtp.get("5"));
    }

    @Test
    public void testUnmarshalFull() throws Exception {
        testUnmarshal("full");
    }

    //- PRIVATE

    @Test
    public void testUnmarshalMinimal() throws Exception {
        testUnmarshal("minimal");
    }

    @Test
    public void testManagedObjectFactoryReadWrite() throws Exception {
        for ( Class<? extends ManagedObject> managedObjectClass : MANAGED_OBJECTS ) {
            final String resourceName = ClassUtils.getClassName(managedObjectClass) + "_full.xml";
            System.out.println( "Processing resource '" + resourceName + "'" );

            // Read / write using stream
            {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = ManagedObject.class.getResourceAsStream( resourceName );
                    ManagedObject mo = ManagedObjectFactory.read( in, managedObjectClass );

                    assertEquals( "Expected object type", managedObjectClass, mo.getClass() );

                    out = new ByteArrayOutputStream(2048);
                    ManagedObjectFactory.write( mo, out );

                } finally {
                    ResourceUtils.closeQuietly( in );
                    ResourceUtils.closeQuietly( out );
                }
            }

            // Read using string, write as doc
            {
                ManagedObject mo = ManagedObjectFactory.read(
                        new String( IOUtils.slurpUrl( ManagedObject.class.getResource( resourceName ) ) ),
                        managedObjectClass );

                assertEquals( "Expected object type", managedObjectClass, mo.getClass() );

                assertNotNull( "Resource document null", ManagedObjectFactory.write( mo ) );
            }
        }
    }

    @Test
    public void testSchemaGeneration() throws Exception {
        final Source[] sources = ValidationUtils.getSchemaSources();
        for ( final Source source : sources ) {
            OutputStreamWriter out = null;
            try {
                IOUtils.copyStream( ((StreamSource)source).getReader(), out = new OutputStreamWriter(System.out){
                    @Override
                    public void close() throws IOException {
                        super.flush();
                    }
                } );
            } finally {
                ResourceUtils.closeQuietly( out );
            }
        }
    }

    @Test
    public void testPackagePrivateConstructor() throws Exception {
        final Collection<Class<?>> jaxbTypes = getJAXBTypes();

        for ( final Class<?> type : jaxbTypes ) {
            for ( final Constructor constructor : type.getConstructors() ) {
                final int modifiers = constructor.getModifiers();
                assertTrue("Constructor should be package private for : " + type.getName(), !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers) );
            }
        }
    }

    @Test
    public void testElementsOrdered() throws Exception {
        final Collection<Class<?>> jaxbTypes = getJAXBTypes();

        for ( final Class<?> type : jaxbTypes ) {
            boolean hasElementContent = false;
            for ( final Method method : type.getDeclaredMethods() ) {
                if ( method.getAnnotation( XmlElement.class ) != null ) {
                    hasElementContent = true;
                    break;
                }
            }

            if ( hasElementContent ) {
                XmlType xmlType = type.getAnnotation( XmlType.class );
                assertTrue("XmlType missing propOrder attribute: " + type.getName(), xmlType.propOrder().length !=0 && !xmlType.propOrder()[0].isEmpty() );

                if ( ManagedObject.class.isAssignableFrom(type) ) {
                    assertTrue("XmlType not extensible " + type.getName(), ArrayUtils.contains( xmlType.propOrder(), "extensions" ));
                }
            }
        }
    }

    @Test
    public void testTestMetadata() {
        assertTrue( "All v2 managed objects must be in the main list", MANAGED_OBJECTS.containsAll( MANAGED_OBJECTS_2 ) );
    }

    private Collection<Class<?>> getJAXBTypes() throws Exception {
        final Collection<Class<?>> typeClasses = new ArrayList<Class<?>>();
        final String packageResource = ManagedObject.class.getPackage().getName().replace( '.', '/' );
        for ( URL url : ClassUtils.listResources( ManagedObject.class, "jaxb.index" ) ) {
            final String path = url.getPath();
            final int index = path.indexOf( packageResource );
            if ( index > 0 ) {
                final String className = path.substring( index + packageResource.length() + 1 );
                final Class<?> moClass = Class.forName( ManagedObject.class.getPackage().getName() + "." + className );
                typeClasses.add( moClass );
            }
        }
        return typeClasses;
    }

    private void testUnmarshal( final String suffix ) throws Exception {
        // Test with schema deployed with customers via the /wsman interface
        testUnmarshal( suffix, getSchema( "gateway-management-6.1.xsd" ), MANAGED_OBJECTS_2 );
        // Test with automatically generated schema
        testUnmarshal( suffix, ValidationUtils.getSchema() );

        //All original MO's should continue to work with 7.1 schema.
        //todo add tests for encapsulated assertions and generic entities
        testUnmarshal( suffix, getSchema("gateway-management-7.1.xsd"), MANAGED_OBJECTS_2 );

    }

    /**
     * Services are allowed to support no verbs. If this happens the layer7 api needs to be able to processes these
     * services.
     */
    @BugId("SSG-6160")
    @Test
    public void testServiceNoVerbs() throws Exception {
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema(getSchema("gateway-management-7.1.xsd"));
        unmarshaller.setSchema(ValidationUtils.getSchema());

        unMarshallFileAndCheckType(unmarshaller, ServiceMO.class, "ServiceMO_HttpMapping_minimal.xml");
    }

    /**
     * Test that all TlsSettings in a ListenPort which are optional are allowed to be missing.
     */
    @BugId("SSG-6159")
    @Test
    public void testListenPort_TlsSettings_Minimal() throws Exception {
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema(getSchema("gateway-management-7.1.xsd"));
        unmarshaller.setSchema(ValidationUtils.getSchema());

        unMarshallFileAndCheckType(unmarshaller, ListenPortMO.class, "ListenPortMO_TLS_minimal.xml");
    }

    private void testUnmarshal( final String suffix,
                                final Schema schema ) throws Exception {
        testUnmarshal( suffix, schema, Collections.<Class<? extends ManagedObject>>emptyList() );
    }

    private void testUnmarshal( final String suffix,
                                final Schema schema,
                                final Collection<Class<? extends ManagedObject>> skip ) throws Exception {
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema( schema );

        for ( Class<? extends ManagedObject> managedObjectClass : MANAGED_OBJECTS ) {
            if ( skip.contains( managedObjectClass ) ) continue;
            final String resourceName = ClassUtils.getClassName(managedObjectClass) + "_" + suffix + ".xml";
            unMarshallFileAndCheckType(unmarshaller, managedObjectClass, resourceName);
        }
    }

    private void unMarshallFileAndCheckType(Unmarshaller unmarshaller, Class<? extends ManagedObject> managedObjectClass, String resourceName) throws JAXBException {
        System.out.println( "Processing resource '" + resourceName + "'" );
        final URL resource = ManagedObjectTest.class.getResource( resourceName );
        assertNotNull( "Missing test resource: " + resourceName, resource );
        ManagedObject mo = (ManagedObject) unmarshaller.unmarshal( resource );
        assertEquals( "Expected object type", managedObjectClass, mo.getClass() );
    }

    private Schema getSchema( final String name ) {
        final SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        try {
            return schemaFactory.newSchema( new Source[]{ new StreamSource( this.getClass().getResourceAsStream( name ) ) } );
        } catch ( SAXException e ) {
            throw ExceptionUtils.wrap( e );
        }
    }

    private X509Certificate cert( final String base64Encoded ) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate( new ByteArrayInputStream(HexUtils.decodeBase64( base64Encoded, true )) );
    }

    private <MO> MO roundTrip( final MO managedObject ) throws JAXBException {
        return roundTrip( managedObject, null );
    }

    @SuppressWarnings({ "unchecked" })
    private <MO> MO roundTrip( final MO managedObject, final Functions.UnaryVoid<Document> callback ) throws JAXBException {
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

        final StringWriter out = new StringWriter();
        marshaller.marshal( managedObject, out );

        final String xmlString = out.toString();
        if (debug)
            System.out.println(xmlString);

        if ( callback != null ) {
            final DOMResult domResult = new DOMResult();
            marshaller.marshal( managedObject, domResult );
            callback.call( (Document) domResult.getNode() );
        }

        final Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema( ValidationUtils.getSchema() );
        MO mo = (MO) unmarshaller.unmarshal(new StringReader(xmlString));

        if (debug)
            marshaller.marshal( mo, System.out );

        return mo;
    }
}
