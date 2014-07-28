package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceHandler;
import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.util.IOUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility to generate the WSDL artifacts for the Gateway Management service.
 */
public class WsdlGenerator {

    /**
     *
     */
    @SuppressWarnings({"unchecked"})
    public static void main( final String[] args ) throws Exception {
        if ( args.length < 1 ) throw new IllegalArgumentException("Output directory not specified");

        final File outputDirectory = new File(args[0]);
        if ( !outputDirectory.isDirectory() ) {
            throw new IllegalArgumentException("Invalid output directory: " + outputDirectory.getAbsolutePath());
        }

        // Write XML Schemas
        final Source[] schemaSources = ValidationUtils.getSchemaSources();
        for ( final Source source : schemaSources ) {
            final String name = "schema1.xsd".equals( source.getSystemId() ) ? "gateway-management.xsd" : source.getSystemId();
            final File schemaFile = new File( outputDirectory, name );

            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(new FileOutputStream( schemaFile ));
                IOUtils.copyStream( ((StreamSource)source).getReader(), out );
            } finally {
                close( out );
            }
        }

        // Write XML input for XSL transform
        final List<String> factoryClasses = findResourceFactoryClassAttributes();
        PrintWriter out = null;

        try {
            out = new PrintWriter( new FileWriter( new File( outputDirectory, "resources.xml" ) ) );

            out.println("<resourceFactories namespace=\""+ResourceHandler.MANAGEMENT_NAMESPACE+"\" schemaLocation=\"gateway-management.xsd\" address=\"http://127.0.0.1:8080/wsman\">");
            for ( final String factoryClass : factoryClasses ) {
                out.println("<resourceFactory class=\"" + factoryClass + "\">");

                final Class<? extends ResourceFactory> factory = (Class<? extends ResourceFactory>) Class.forName( factoryClass );
                final ResourceFactory.ResourceType type = factory.getAnnotation( ResourceFactory.ResourceType.class );

                out.println( "<name>" + AccessorSupport.getResourceName(type.type()) + "</name>" );
                out.println( "<resource class=\"" + type.type().getName() + "\">" );
                out.println( "<element>" + type.type().getAnnotation( XmlRootElement.class).name()+ "</element>" );
                out.println( "<type>" + asSchemaType(type.type())+ "</type>" );
                out.println( "</resource>" );

                for ( final Method method : factory.getDeclaredMethods() ) {
                    final ResourceFactory.ResourceMethod resourceMethod = method.getAnnotation( ResourceFactory.ResourceMethod.class );
                    if ( resourceMethod != null ) {
                        out.println( "<resourceMethod>" );
                        out.println( "<name>" + resourceMethod.name() + "</name>" );
                        out.println( "<selectors>" + resourceMethod.selectors() + "</selectors>" );
                        out.println( "<resource>" + resourceMethod.resource() + "</resource>" );
                        if ( resourceMethod.resource() ) {
                            out.println("<request>");
                            Class<?> parameterType = method.getParameterTypes()[ resourceMethod.selectors() ? 1 : 0 ];
                            out.println( "<element>" + asElement(parameterType) +  "</element>" );
                            out.println( "<type>" + asSchemaType(parameterType) + "</type>");
                            out.println("</request>");
                        }
                        out.println("<response>");
                        out.println( "<element>" + asElement(method.getReturnType()) +  "</element>" );
                        out.println( "<type>" + asSchemaType(method.getReturnType()) + "</type>");
                        out.println("</response>");
                        out.println( "</resourceMethod>" );
                    }
                }

                out.println("</resourceFactory>");
            }
            out.println("</resourceFactories>");
        } finally {
            close( out );
        }
    }

    /**
     *
     */
    private static String asSchemaType( final Class<?> type ) {
        String schemaType = "wxf:AnyXmlMessage";

        XmlType xmlType = type.getAnnotation(XmlType.class);
        if ( xmlType != null ) {
            schemaType = "tns:"+xmlType.name();
        } else if ( Void.class.equals(type) ) {
            schemaType = "wxf:EmptyMessage";
        }

        return schemaType;
    }

    /**
     *
     */
    private static String asElement( final Class<?> type ) {
        String element = "";

        XmlRootElement xmlRootElement = type.getAnnotation(XmlRootElement.class);
        if ( xmlRootElement != null ) {
            element = xmlRootElement.name();
        }

        return element;
    }

    /**
     *
     */
    private static List<String> findResourceFactoryClassAttributes() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );
        final DocumentBuilder db = dbf.newDocumentBuilder();        
        final Document beansDoc = db.parse( ResourceFactory.class.getResource("gatewayManagementContext.xml").toString() );

        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext( new NamespaceContext(){
            @Override
            public String getNamespaceURI( final String prefix ) {
                if ( "spr".equals( prefix ) ) {
                    return "http://www.springframework.org/schema/beans";
                }
                return null;
            }

            @Override
            public String getPrefix( final String namespaceURI ) {
                return null;
            }

            @Override
            public Iterator getPrefixes( final String namespaceURI ) {
                return null;
            }
        } );
        final NodeList factoryRefs = (NodeList) xpath.evaluate("//spr:bean[@id='resourceFactoryRegistry']/spr:constructor-arg/spr:list/spr:ref/@local", beansDoc, XPathConstants.NODESET);
        final List<String> factoryClasses = new ArrayList<>(factoryRefs.getLength());
        for ( int i=0; i<factoryRefs.getLength(); i++ ) {
            final String factoryRef = ((Attr) factoryRefs.item(i)).getValue();

            final String factoryClass = (String) xpath.evaluate("//spr:bean[@id='"+factoryRef+"']/@class", beansDoc, XPathConstants.STRING);
            factoryClasses.add(factoryClass);
        }
        return factoryClasses;
    }

    /**
     *
     */
    public static void close( final Closeable closeable ) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch(IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

}
