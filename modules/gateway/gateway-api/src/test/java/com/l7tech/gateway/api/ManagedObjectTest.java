package com.l7tech.gateway.api;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ClassUtils;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * TODO [steve] marshalling tests
 */
public class ManagedObjectTest {

    @Test
    public void testCertificateDataSerialization() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.l7tech.gateway.api");

        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        certificateData.setEncoded( new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1} );
        certificateData.setIssuerName("cn=Test Issuer");
        certificateData.setSerialNumber( BigInteger.valueOf( 123456789 ) );
        certificateData.setSubjectName("cn=Test Issuer");

        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

        StringWriter out = new StringWriter();
        marshaller.marshal(certificateData, out );

        String xmlString = out.toString();
        System.out.println(xmlString);

        final Unmarshaller unmarshaller = context.createUnmarshaller();
        CertificateData roundTripped = (CertificateData) unmarshaller.unmarshal(new StringReader(xmlString));

        marshaller.marshal( roundTripped, System.out );
    }

    @Test
    public void testServiceSerialization() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.l7tech.gateway.api");

        ServiceMO service = ManagedObjectFactory.createService();
        service.setId( "1" );
        service.setVersion( 1 );

        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
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
            put("soap", Boolean.TRUE);
        }});
        ((ServiceDetail.HttpMapping)serviceDetail.getServiceMappings().get( 0 )).setUrlPattern( "/test" );
        ((ServiceDetail.HttpMapping)serviceDetail.getServiceMappings().get( 0 )).setVerbs( Arrays.asList("POST") );

        service.setServiceDetail( serviceDetail );

        Resource policyResource = ManagedObjectFactory.createResource();
        policyResource.setId("1");
        policyResource.setType("policy");
        policyResource.setContent( "<Policy><PolicyContent/>                                                                                                         </Policy>" );

        List<ResourceSet> resourceSets = new ArrayList<ResourceSet>();
        ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSets.add( resourceSet );
        List<Resource> resources = new ArrayList<Resource>();
        resources.add( policyResource );
        resourceSet.setResources( resources );
        service.setResourceSets( resourceSets );

        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

        StringWriter out = new StringWriter();
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xsw = xof.createXMLStreamWriter( out );
        marshaller.marshal(service, wrap(xsw) );

        String xmlString = out.toString();
        System.out.println(xmlString);

        final Unmarshaller unmarshaller = context.createUnmarshaller();
        ServiceMO roundTripped = (ServiceMO) unmarshaller.unmarshal(new StringReader(xmlString));

        //System.out.println( roundTripped.getResources().get(0) );

        marshaller.marshal( roundTripped, System.out );
    }

    //TODO [steve] formatting CDATA aware XMLStreamWriter
    private XMLStreamWriter wrap( final XMLStreamWriter xmlStreamWriter ) {
        return new XMLStreamWriter(){
            @Override
            public void writeStartElement( final String localName ) throws XMLStreamException {
                xmlStreamWriter.writeCharacters( "\n" );
                xmlStreamWriter.writeStartElement( localName );
            }

            @Override
            public void writeStartElement( final String namespaceURI, final String localName ) throws XMLStreamException {
                xmlStreamWriter.writeCharacters( "\n" );
                xmlStreamWriter.writeStartElement( namespaceURI, localName );
            }

            @Override
            public void writeStartElement( final String prefix, final String localName, final String namespaceURI ) throws XMLStreamException {
                xmlStreamWriter.writeCharacters( "\n" );
                xmlStreamWriter.writeStartElement( prefix, localName, namespaceURI );
            }

            @Override
            public void writeEmptyElement( final String namespaceURI, final String localName ) throws XMLStreamException {
                xmlStreamWriter.writeCharacters( "\n" );
                xmlStreamWriter.writeEmptyElement( namespaceURI, localName );
            }

            @Override
            public void writeEmptyElement( final String prefix, final String localName, final String namespaceURI ) throws XMLStreamException {
                xmlStreamWriter.writeCharacters( "\n" );
                xmlStreamWriter.writeEmptyElement( prefix, localName, namespaceURI );
            }

            @Override
            public void writeEmptyElement( final String localName ) throws XMLStreamException {
                xmlStreamWriter.writeCharacters( "\n" );
                xmlStreamWriter.writeEmptyElement( localName );
            }

            @Override
            public void writeEndElement() throws XMLStreamException {
                xmlStreamWriter.writeEndElement();
            }

            @Override
            public void writeEndDocument() throws XMLStreamException {
                xmlStreamWriter.writeEndDocument();
            }

            @Override
            public void close() throws XMLStreamException {
                xmlStreamWriter.close();
            }

            @Override
            public void flush() throws XMLStreamException {
                xmlStreamWriter.flush();
            }

            @Override
            public void writeAttribute( final String localName, final String value ) throws XMLStreamException {
                xmlStreamWriter.writeAttribute( localName, value );
            }

            @Override
            public void writeAttribute( final String prefix, final String namespaceURI, final String localName, final String value ) throws XMLStreamException {
                xmlStreamWriter.writeAttribute( prefix, namespaceURI, localName, value );
            }

            @Override
            public void writeAttribute( final String namespaceURI, final String localName, final String value ) throws XMLStreamException {
                xmlStreamWriter.writeAttribute( namespaceURI, localName, value );
            }

            @Override
            public void writeNamespace( final String prefix, final String namespaceURI ) throws XMLStreamException {
                xmlStreamWriter.writeNamespace( prefix, namespaceURI );
            }

            @Override
            public void writeDefaultNamespace( final String namespaceURI ) throws XMLStreamException {
                xmlStreamWriter.writeDefaultNamespace( namespaceURI );
            }

            @Override
            public void writeComment( final String data ) throws XMLStreamException {
                xmlStreamWriter.writeComment( data );
            }

            @Override
            public void writeProcessingInstruction( final String target ) throws XMLStreamException {
                xmlStreamWriter.writeProcessingInstruction( target );
            }

            @Override
            public void writeProcessingInstruction( final String target, final String data ) throws XMLStreamException {
                xmlStreamWriter.writeProcessingInstruction( target, data );
            }

            @Override
            public void writeCData( final String data ) throws XMLStreamException {
                xmlStreamWriter.writeCData( data );
            }

            @Override
            public void writeDTD( final String dtd ) throws XMLStreamException {
                xmlStreamWriter.writeDTD( dtd );
            }

            @Override
            public void writeEntityRef( final String name ) throws XMLStreamException {
                xmlStreamWriter.writeEntityRef( name );
            }

            @Override
            public void writeStartDocument() throws XMLStreamException {
                xmlStreamWriter.writeStartDocument();
            }

            @Override
            public void writeStartDocument( final String version ) throws XMLStreamException {
                xmlStreamWriter.writeStartDocument( version );
            }

            @Override
            public void writeStartDocument( final String encoding, final String version ) throws XMLStreamException {
                xmlStreamWriter.writeStartDocument( encoding, version );
            }

            @Override
            public void writeCharacters( final String text ) throws XMLStreamException {
                if ( text.length() > 128 ) {
                    xmlStreamWriter.writeCData( text );
                } else {
                    xmlStreamWriter.writeCharacters( text );
                }
            }

            @Override
            public void writeCharacters( final char[] text, final int start, final int len ) throws XMLStreamException {
                if ( text.length > 128 ) {
                    xmlStreamWriter.writeCData( new String(text, start, len) );
                } else {
                    xmlStreamWriter.writeCharacters( text, start, len );
                }
            }

            @Override
            public String getPrefix( final String uri ) throws XMLStreamException {
                return xmlStreamWriter.getPrefix( uri );
            }

            @Override
            public void setPrefix( final String prefix, final String uri ) throws XMLStreamException {
                xmlStreamWriter.setPrefix( prefix, uri );
            }

            @Override
            public void setDefaultNamespace( final String uri ) throws XMLStreamException {
                xmlStreamWriter.setDefaultNamespace( uri );
            }

            @Override
            public void setNamespaceContext( final NamespaceContext context ) throws XMLStreamException {
                xmlStreamWriter.setNamespaceContext( context );
            }

            @Override
            public NamespaceContext getNamespaceContext() {
                return xmlStreamWriter.getNamespaceContext();
            }

            @Override
            public Object getProperty( final String name ) throws IllegalArgumentException {
                return xmlStreamWriter.getProperty( name );
            }
        };
    }

    @Test
    public void testSchemaGeneration() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.l7tech.gateway.api");

        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

//        StringWriter out = new StringWriter();
//        marshaller.marshal( service, out );

        final StringWriter schemaWriter = new StringWriter();
        context.generateSchema( new SchemaOutputResolver(){
            @Override
            public Result createOutput( final String namespaceUri, final String suggestedFileName ) throws IOException {
                System.out.println(suggestedFileName);
                StreamResult result = new StreamResult( schemaWriter );
                result.setSystemId(suggestedFileName);
                return result;
            }
        } );
        System.out.println( schemaWriter.toString() );

//        String xmlString = out.toString();
//        System.out.println(xmlString);

//        final Unmarshaller unmarshaller = context.createUnmarshaller();
//        ServiceDetail roundTripped = (ServiceDetail) unmarshaller.unmarshal(new StringReader(xmlString));

        //System.out.println( roundTripped.getResources().get(0) );

//        marshaller.marshal( roundTripped, System.out );
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

}
