package com.l7tech.gateway.api;

import com.l7tech.gateway.api.IdentityProviderMO.LdapIdentityProviderMapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.util.HexUtils;
import org.w3c.dom.Document;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Factory for managed objects.
 *
 * <p>The factory is used to create new instances of managed objects and other
 * API classes.</p>
 *
 * <p>The factory provides persistence for managed objects.</p>
 *
 * @see ManagedObject
 */
public class ManagedObjectFactory {

    //- PUBLIC

    /**
     * Create a new HttpMapping instance.
     *
     * @return The new instance.
     */
    public static ServiceDetail.HttpMapping createHttpMapping() {
        return new ServiceDetail.HttpMapping();
    }

    /**
     * Create a new SoapMapping instance.
     *
     * @return The new instance.
     */
    public static ServiceDetail.SoapMapping createSoapMapping() {
        return new ServiceDetail.SoapMapping();
    }

    /**
     * Create a new ServiceDetail instance.
     *
     * @return The new instance.
     */
    public static ServiceDetail createServiceDetail() {
        return new ServiceDetail();
    }

    /**
     * Create a new ServiceMO instance.
     *
     * @return The new instance.
     */
    public static ServiceMO createService() {
        return new ServiceMO();
    }

    /**
     * Create a new PolicyDetail instance.
     *
     * @return The new instance.
     */
    public static PolicyDetail createPolicyDetail() {
        return new PolicyDetail();
    }

    /**
     * Create a new PolicyMO instance.
     *
     * @return The new instance.
     */
    public static PolicyMO createPolicy() {
        return new PolicyMO();
    }

    /**
     * Create a new CertificateData instance.
     *
     * @return The new instance.
     */
    public static CertificateData createCertificateData() {
        return new CertificateData();    
    }

    /**
     * Create a new CertificateData instance.
     *
     * @param base64 The encoded certificate
     * @return The new instance.
     * @see CertificateData#getEncoded()
     */
    public static CertificateData createCertificateData( final String base64 ) {
        CertificateData data = createCertificateData();
        if ( base64 != null ) {
            data.setEncoded( HexUtils.decodeBase64( base64, true ) );
        }
        return data;
    }

    /**
     * Create a new CertificateData instance.
     *
     * <p>All CertificateData properties are initialized using the given
     * certificate.</p>
     *
     * @param certificate The X.509 certificate
     * @return The new instance.
     */
    public static CertificateData createCertificateData( final X509Certificate certificate ) throws FactoryException {
        CertificateData data = createCertificateData();
        if ( certificate != null ) {
            data.setIssuerName( certificate.getIssuerX500Principal().getName( X500Principal.RFC2253, ValidationUtils.getOidKeywordMap() ) );
            data.setSerialNumber( certificate.getSerialNumber() );
            data.setSubjectName( certificate.getSubjectX500Principal().getName() );
            try {
                data.setEncoded( certificate.getEncoded() );
            } catch ( CertificateEncodingException e ) {
                throw new FactoryException( "Error getting encoded certificate data.", e );
            }
        }
        return data;
    }

    /**
     * Create a new TrustedCertificateMO instance.
     *
     * @return The new instance.
     */
    public static TrustedCertificateMO createTrustedCertificate() {
        return new TrustedCertificateMO();
    }

    /**
     * Create a new ClusterPropertyMO instance.
     *
     * @return The new instance.
     */
    public static ClusterPropertyMO createClusterProperty() {
        return new ClusterPropertyMO();
    }

    /**
     * Create a new FolderMO instance.
     *
     * @return The new instance.
     */
    public static FolderMO createFolder() {
        return new FolderMO();
    }

    /**
     * Create a new IdentityProviderMO instance.
     *
     * @return The new instance.
     */
    public static IdentityProviderMO createIdentityProvider() {
        return new IdentityProviderMO();
    }

    /**
     * Create a new LdapIdentityProviderMapping instance.
     *
     * @return The new instance.
     */
    public static LdapIdentityProviderMapping createLdapIdentityProviderMapping() {
        return new LdapIdentityProviderMapping();
    }

    /**
     * Create a new JDBCConnectionMO instance.
     *
     * @return The new instance.
     */
    public static JDBCConnectionMO createJDBCConnection() {
        return new JDBCConnectionMO();
    }

    /**
     * Create a new JMSConnection instance.
     *
     * @return The new instance.
     */
    public static JMSConnection createJMSConnection() {
        return new JMSConnection();
    }

    /**
     * Create a new JMSDestinationDetails instance.
     *
     * @return The new instance.
     */
    public static JMSDestinationDetail createJMSDestinationDetails() {
        return new JMSDestinationDetail();
    }

    public static JMSDestinationDetail.JmsEndpointMessagePropertyRuleList createJmsEndpointMessagePropertyRuleList() {
        return new JMSDestinationDetail.JmsEndpointMessagePropertyRuleList();
    }

    public static JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport createJmsEndpointMessagePropertyRule() {
        return new JMSDestinationDetail.JmsEndpointMessagePropertyRuleSupport();
    }

    /**
     * Create a new JMSDestinationMO instance.
     *
     * @return The new instance.
     */
    public static JMSDestinationMO createJMSDestination() {
        return new JMSDestinationMO();
    }

    /**
     * Create a new PolicyExportResult instance.
     *
     * @return The new instance.
     */
    public static PolicyExportResult createPolicyExportResult() {
        return new PolicyExportResult();
    }

    public static EncapsulatedAssertionExportResult createEncapsulatedAssertionExportResult() {
        return new EncapsulatedAssertionExportResult();
    }

    /**
     * Create a new PolicyImportResult instance.
     *
     * @return The new instance.
     */
    public static PolicyImportResult createPolicyImportResult() {
        return new PolicyImportResult();
    }

    /**
     * Create a new ImportedPolicyReference instance.
     *
     * @return The new instance.
     */
    public static PolicyImportResult.ImportedPolicyReference createImportedPolicyReference() {
        return new PolicyImportResult.ImportedPolicyReference();
    }

    /**
     * Create a new PolicyReferenceInstruction instance.
     *
     * @return The new instance.
     */
    public static PolicyReferenceInstruction createPolicyReferenceInstruction() {
        return new PolicyReferenceInstruction();
    }

    /**
     * Create a new PolicyValidationResult instance.
     *
     * @return The new instance.
     */
    public static PolicyValidationResult createPolicyValidationResult() {
        return new PolicyValidationResult();
    }

    /**
     * Create a new PolicyValidationMessage instance.
     *
     * @return The new instance.
     */
    public static PolicyValidationResult.PolicyValidationMessage createPolicyValidationMessage() {
        return new PolicyValidationResult.PolicyValidationMessage();
    }

    /**
     * Create a new AssertionDetail instance.
     *
     * @return The new instance.
     */
    public static PolicyValidationResult.AssertionDetail createAssertionDetail() {
        return new PolicyValidationResult.AssertionDetail();
    }

    /**
     * Create a new PrivateKeyMO instance.
     *
     * @return The new instance.
     */
    public static PrivateKeyMO createPrivateKey() {
        return new PrivateKeyMO();        
    }

    /**
     * Create a new PrivateKeyCreationContext instance.
     *
     * @return The new instance.
     */
    public static PrivateKeyCreationContext createPrivateKeyCreationContext() {
        return new PrivateKeyCreationContext();
    }

    /**
     * Create a new Resource instance.
     *
     * @return The new instance.
     */
    public static Resource createResource() {
        return new Resource();
    }

    /**
     * Create a new ResourceSet instance.
     *
     * @return The new instance.
     */
    public static ResourceSet createResourceSet() {
        return new ResourceSet();
    }

    /**
     * Create a new ResourceDocumentMO instance.
     *
     * @return The new instance.
     */
    public static ResourceDocumentMO createResourceDocument() {
        return new ResourceDocumentMO();
    }

    /**
     * Create a new RevocationCheckingPolicyMO instance.
     *
     * @return The new instance.
     */
    public static RevocationCheckingPolicyMO createRevocationCheckingPolicy() {
        return new RevocationCheckingPolicyMO();
    }

    /**
     * Create a new ListenPortMO instance.
     *
     * @return The new instance.
     */
    public static ListenPortMO createListenPort() {
        return new ListenPortMO();
    }

    /**
     * Create a new TlsSettings instance for an ListenPortMO.
     *
     * @return The new instance.
     */
    public static ListenPortMO.TlsSettings createTlsSettings() {
        return new ListenPortMO.TlsSettings();
    }

    /**
     * Create a new InterfaceTagMO instance.
     *
     * @return The new instance.
     */
    public static InterfaceTagMO createInterfaceTag() {
        return new InterfaceTagMO();
    }

    /**
     * Create a new StoredPasswordMO instance.
     *
     * @return The new instance.
     */
    public static StoredPasswordMO createStoredPassword() {
        return new StoredPasswordMO();
    }

    /**
     * Create a new EncapsulatedAssertionMO instance.
     * <p/>
     * The new instance will have the default (not-yet-saved) OID, no name, no GUID, and no backing policy.
     *
     * @return The new instance.
     */
    public static EncapsulatedAssertionMO createEncapsulatedAssertion() {
        return new EncapsulatedAssertionMO();
    }

    /**
     * Create a new GenericEntityMO instance.
     *
     * @return The new instance
     */
    public static GenericEntityMO createGenericEntity() {
        return new GenericEntityMO();
    }

    /**
     * Create a new CustomKeyValueStoreMO instance.
     *
     * @return The new instance
     */
    public static CustomKeyValueStoreMO createCustomKeyValueStore() {
        return new CustomKeyValueStoreMO();
    }

    /**
     * Create a new ActiveConnectorMO instance.
     *
     * @return The new instance
     */
    public static ActiveConnectorMO createActiveConnector() {
        return new ActiveConnectorMO();
    }

    /**
     * Create a new SecurityZoneMO instance.
     *
     * @return The new instance
     */
    public static SecurityZoneMO createSecurityZone() {
        return new SecurityZoneMO();
    }

    /**
     * Create a new SiteMinderConfigurationMO instance.
     *
     * @return The new instance
     */
    public static SiteMinderConfigurationMO createSiteMinderConfiguration() {
        return new SiteMinderConfigurationMO();
    }

    /**
     * Create a new AssertionSecurityZoneMO instance.
     *
     * @return The new instance
     */
    public static AssertionSecurityZoneMO createAssertionAccess() {
        return new AssertionSecurityZoneMO();
    }

    /**
     * Create a new HttpConfigurationMO instance.
     *
     * @return The new instance
     */
    public static HttpConfigurationMO createHttpConfiguration() {
        return new HttpConfigurationMO();
    }

    /**
     * Create a new PolicyAliasMO instance.
     *
     * @return The new instance
     */
    public static PolicyAliasMO createPolicyAlias() {
        return new PolicyAliasMO();
    }


    /**
     * Create a new ServiceAliasMO instance.
     *
     * @return The new instance
     */
    public static ServiceAliasMO createServiceAlias() {
        return new ServiceAliasMO();
    }

    /**
     * Create a new EmailListenerMO instance.
     *
     * @return The new instance
     */
    public static EmailListenerMO createEmailListener() {
        return new EmailListenerMO();
    }
    
    /**
     * Create a new RbacRoleMO instance.
     *
     * @return The new instance
     */
    public static RbacRoleMO createRbacRoleMO() {
        return new RbacRoleMO();
    }

    /**
     * Create a new RbacRoleAssignmentMO instance.
     *
     * @return The new instance
     */
    public static RbacRoleAssignmentMO createRbacRoleAssignmentMO() {
        return new RbacRoleAssignmentMO();
    }

    /**
     * Create a new RbacRolePermissionMO instance.
     *
     * @return The new instance
     */
    public static RbacRolePermissionMO createRbacRolePermissionMO() {
        return new RbacRolePermissionMO();
    }

    /**
     * Create a new RbacRolePredicateMO instance.
     *
     * @return The new instance
     */
    public static RbacRolePredicateMO createRbacRolePredicateMO() {
        return new RbacRolePredicateMO();
    }


    /**
     * Create a new Password instance.
     *
     * @return The new instance
     */
    public static Password createPassword() {
        return new Password();
    }


    /**
     * Create a new PrivateKeyRestExport instance.
     *
     * @return The new instance
     */
    public static PrivateKeyRestExport createPrivateKeyRestExportMO() {
        return new PrivateKeyRestExport();
    }

    /**
     * Create a new UserMO instance.
     *
     * @return The new instance
     */
    public static UserMO createUserMO() {
        return new UserMO();
    }

    /**
     * Create a new GroupMO instance.
     *
     * @return The new instance
     */
    public static GroupMO createGroupMO() {
        return new GroupMO();
    }

    /**
     * Read a managed object from the given data.
     *
     * @param in The input data.
     * @param objectClass The class of the resulting managed object.
     * @return The managed object.
     * @throws IOException If an error occurs.
     */
    public static <MO extends ManagedObject> MO read( final String in,
                                                      final Class<MO> objectClass ) throws IOException {
        final StreamSource source = new StreamSource( new StringReader(in) );
        return MarshallingUtils.unmarshal( objectClass, source );
    }

    /**
     * Read a managed object from the given document.
     *
     * @param document The input document
     * @param objectClass The class of the resulting managed object.
     * @return The managed object.
     * @throws IOException If an error occurs.
     */
    public static <MO extends ManagedObject> MO read( final Document document,
                                                      final Class<MO> objectClass ) throws IOException {
        final DOMSource source = new DOMSource( document );
        return MarshallingUtils.unmarshal( objectClass, source );
    }

    /**
     * Read a managed object from the given stream.
     *
     * @param in The input stream
     * @param objectClass The class of the resulting managed object.
     * @return The managed object.
     * @throws IOException If an error occurs.
     */
    public static <MO extends ManagedObject> MO read( final InputStream in,
                                                      final Class<MO> objectClass ) throws IOException {
        final StreamSource source = new StreamSource( in );
        return MarshallingUtils.unmarshal( objectClass, source );
    }

    /**
     * Write a managed object to a document.
     *
     * @param mo The managed object to write.
     * @return The document.
     * @throws IOException If an error occurs.
     */
    public static Document write( final ManagedObject mo ) throws IOException {
        final DOMResult result = new DOMResult();
        MarshallingUtils.marshal( mo, result, false );
        return (Document) result.getNode();
    }

    /**
     * Write a managed object to an output stream.
     *
     * @param mo The managed object to write.
     * @param out The stream to write to.
     * @throws IOException If an error occurs.
     */
    public static void write( final ManagedObject mo, final OutputStream out ) throws IOException {
        final StreamResult result = new StreamResult( out );
        MarshallingUtils.marshal( mo, result, false );
    }

    public static JAXBContext getJAXBContext() throws JAXBException {
        return MarshallingUtils.getJAXBContext();
    }

    public static Class[] getAllManagedObjectClasses(){
        return new Class[]{
                ClusterPropertyMO.class,
                CustomKeyValueStoreMO.class,
                EncapsulatedAssertionMO.class,
                FolderMO.class,
                GenericEntityMO.class,
                IdentityProviderMO.class,
                InterfaceTagMO.class,
                JDBCConnectionMO.class,
                JMSDestinationMO.class,
                ListenPortMO.class,
                ServiceAliasMO.class,
                PolicyAliasMO.class,
                PolicyMO.class,
                PrivateKeyMO.class,
                ResourceDocumentMO.class,
                RevocationCheckingPolicyMO.class,
                ServiceMO.class,
                StoredPasswordMO.class,
                TrustedCertificateMO.class,
                CustomKeyValueStoreMO.class,
                ActiveConnectorMO.class,
                SecurityZoneMO.class,
                SiteMinderConfigurationMO.class,
                AssertionSecurityZoneMO.class,
                HttpConfigurationMO.class,
                RbacRoleMO.class,
                RbacRoleAssignmentMO.class,
                RbacRolePermissionMO.class,
                RbacRolePredicateMO.class,
                DependencyListMO.class,
                DependencyMO.class,
                Item.class,
                ItemsList.class,
                Mapping.class,
                Link.class,
                PolicyVersionMO.class
        };
    }

    public static DependencyListMO createDependencyListMO() {
        return new DependencyListMO();
    }

    public static DependencyMO createDependencyMO() {
        return new DependencyMO();
    }

    public static Mapping createMapping() {
        return new Mapping();
    }

    public static Mapping createMapping(Mapping mapping) {
        return new Mapping(mapping);
    }

    public static Mappings createMappings(List<Mapping> mappings) {
        return new Mappings(mappings);
    }

    public static Link createLink(String rel, String uri){
        return new Link(rel, uri);
    }

    public static PolicyVersionMO createPolicyVersionMO() {
        return new PolicyVersionMO();
    }

    public static Bundle createBundle(){
        return new Bundle();
    }

    public static ErrorResponse createErrorResponse(){
        return new ErrorResponse();
    }

    /**
     * General purpose exception for factory errors.
     */
    @SuppressWarnings( { "serial" } )
    public static class FactoryException extends ManagementException {
        public FactoryException( final String message ) {
            super( message );
        }

        public FactoryException( final String message, final Throwable cause ) {
            super( message, cause );
        }

        public FactoryException( final Throwable cause ) {
            super( cause );
        }
    }

}
