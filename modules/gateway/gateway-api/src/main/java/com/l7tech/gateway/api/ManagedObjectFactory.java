package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.w3c.dom.Document;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class ManagedObjectFactory {

    //- PUBLIC

    public static ServiceDetail.HttpMapping createHttpMapping() {
        return new ServiceDetail.HttpMapping();
    }

    public static ServiceDetail.SoapMapping createSoapMapping() {
        return new ServiceDetail.SoapMapping();
    }

    public static ServiceDetail createServiceDetail() {
        return new ServiceDetail();
    }

    public static ServiceMO createService() {
        return new ServiceMO();
    }

    public static PolicyDetail createPolicyDetail() {
        return new PolicyDetail();
    }

    public static PolicyMO createPolicy() {
        return new PolicyMO();
    }

    public static CertificateData createCertificateData() {
        return new CertificateData();    
    }

    public static CertificateData createCertificateData( final String base64 ) {
        CertificateData data = createCertificateData();
        if ( base64 != null ) {
            data.setEncoded( HexUtils.decodeBase64( base64, true ) );
        }
        return data;
    }

    public static CertificateData createCertificateData( final X509Certificate certificate ) throws FactoryException {
        CertificateData data = createCertificateData();
        if ( certificate != null ) {
            data.setIssuerName( certificate.getIssuerX500Principal().getName( X500Principal.RFC2253, DN_OID_MAP ) );
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

    public static TrustedCertificateMO createCertificate() {
        return new TrustedCertificateMO();
    }

    public static ClusterPropertyMO createClusterProperty() {
        return new ClusterPropertyMO();
    }

    public static FolderMO createFolder() {
        return new FolderMO();
    }

    public static IdentityProviderMO createIdentityProvider() {
        return new IdentityProviderMO();
    }

    public static JDBCConnectionMO createJDBCConnection() {
        return new JDBCConnectionMO();
    }

    public static JMSConnection createJMSConnection() {
        return new JMSConnection();
    }

    public static JMSDestinationDetails createJMSDestinationDetails() {
        return new JMSDestinationDetails();
    }

    public static JMSDestinationMO createJMSDestination() {
        return new JMSDestinationMO();
    }

    public static PolicyExportResult createPolicyExportResult() {
        return new PolicyExportResult();
    }

    public static PolicyImportResult createPolicyImportResult() {
        return new PolicyImportResult();
    }

    public static PolicyImportResult.ImportedPolicyReference createImportedPolicyReference() {
        return new PolicyImportResult.ImportedPolicyReference();
    }

    public static PolicyAccessor.PolicyReferenceInstruction createPolicyReferenceInstruction() {
        return new PolicyAccessor.PolicyReferenceInstruction();
    }

    public static PolicyValidationContext createPolicyValidationContext() {
        return new PolicyValidationContext();
    }

    public static PolicyValidationResult createPolicyValidationResult() {
        return new PolicyValidationResult();
    }

    public static PolicyValidationResult.PolicyValidationMessage createPolicyValidationMessage() {
        return new PolicyValidationResult.PolicyValidationMessage();
    }

    public static PolicyValidationResult.AssertionDetail createAssertionDetail() {
        return new PolicyValidationResult.AssertionDetail();
    }

    public static PrivateKeyMO createPrivateKey() {
        return new PrivateKeyMO();        
    }

    public static Resource createResource() {
        return new Resource();
    }

    public static ResourceSet createResourceSet() {
        return new ResourceSet();
    }

    public static ResourceDocumentMO createResourceDocument() {
        return new ResourceDocumentMO();
    }

    public static <MO extends ManagedObject> MO read( final Document document, final Class<MO> objectClass ) throws IOException {
        final DOMSource source = new DOMSource( document );
        return unmarshal( objectClass, source );
    }

    public static <MO extends ManagedObject> MO read( final InputStream in, final Class<MO> objectClass ) throws IOException {
        final StreamSource source = new StreamSource( in );
        return unmarshal( objectClass, source );
    }

    public static Document write( final ManagedObject mo ) throws IOException {
        final DOMResult result = new DOMResult();
        marshal( mo, result );
        return (Document) result.getNode();
    }

    public static void write( final ManagedObject mo, final OutputStream out ) throws IOException {
        final StreamResult result = new StreamResult( out );
        marshal( mo, result );
    }

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

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ManagedObjectFactory.class.getName() );

    private static final Map<String,String> DN_OID_MAP = Collections.unmodifiableMap( new HashMap<String,String>(){{
        put( "1.2.840.113549.1.9.1", "EMAILADDRESS" );
        put( "2.5.4.4", "SURNAME" );
        put( "2.5.4.5", "SERIALNUMBER" );
        put( "2.5.4.12", "T" );
        put( "2.5.4.42", "GIVENNAME" );
        put( "2.5.4.43", "INITIALS" );
        put( "2.5.4.44", "GENERATION" );
        put( "2.5.4.46", "DNQ" );
    }} );

    private static void marshal( final ManagedObject mo, final Result result ) throws IOException {
        try {
            final JAXBContext context = createJAXBContext();
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            //TODO [steve] namespace mapping?
            //marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SchemaAnnotationNamespacePrefixMapper(ManagedObjectFactory.class.getPackage()) );

            try {
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            } catch ( PropertyException e) {
                logger.info( "Unable to set marshaller for formatted output '"+ ExceptionUtils.getMessage(e)+"'." );
            }

            marshaller.marshal(mo, result );
        } catch ( JAXBException e ) {
            throw new IOException( "Error writing object '"+ExceptionUtils.getMessage(e)+"'.", e );
        }
    }

    @SuppressWarnings( { "unchecked" } )
    private static <MO extends ManagedObject> MO unmarshal( final Class<MO> objectClass, final Source source ) throws IOException {
        try {
            final JAXBContext context = createJAXBContext();
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            unmarshaller.setSchema( ValidationUtils.getSchema() );
            unmarshaller.setEventHandler( new ValidationEventHandler(){
                @Override
                public boolean handleEvent( final ValidationEvent event ) {
                    return false;
                }
            } );

            Object read = unmarshaller.unmarshal( source );
            if ( !objectClass.isInstance(read) ) {
                throw new IOException("Unexpected object type '"+read.getClass().getName()+"'.");
            }
            return (MO) read;
        } catch ( JAXBException e ) {
            throw new IOException( "Error writing object '"+ ExceptionUtils.getMessage(e)+"'.", e );
        }
    }

    private static JAXBContext createJAXBContext() throws JAXBException {
        return JAXBContext.newInstance("com.l7tech.gateway.api");
    }
}
