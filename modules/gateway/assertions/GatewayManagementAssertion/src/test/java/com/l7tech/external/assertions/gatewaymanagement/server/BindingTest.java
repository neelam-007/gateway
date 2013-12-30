package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.NamespaceContextImpl;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BindingTest {

    @Test
    public void testSchemaGeneration() throws Exception {
        final Map<String, String> schemaResources = generateSchemas();
        final SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        final Schema schema = schemaFactory.newSchema( Functions.map(schemaResources.entrySet(), new Functions.Unary<Source,Map.Entry<String,String>>(){
            @Override
            public Source call( final Map.Entry<String, String> entry ) {
                return new StreamSource( new StringReader(entry.getValue()), entry.getKey() );
            }
        }).toArray( new Source[schemaResources.size()]));

        assertNotNull( "Schema", schema );
    }

    @Test
    public void testResourceFactories() throws Exception {
        final NodeList nodeList = findResourceFactoryRefAttributes();
        assertTrue( "Found factories", nodeList.getLength() > 0 );

        for ( int i=0; i<nodeList.getLength(); i++ ) {
            Attr attribute = (Attr) nodeList.item( i );
            String refId = attribute.getValue();
            NodeList refClass = findResourceFactoryClassAttributes(refId);
            assertEquals(refId, 1, refClass.getLength());
            Class<?> resourceFactoryClass = Class.forName(((Attr)refClass.item(0)).getValue());
            ResourceFactory.ResourceType resourceType = resourceFactoryClass.getAnnotation( ResourceFactory.ResourceType.class );
            resourceType.type();
        }
    }

    private static NodeList findResourceFactoryRefAttributes() throws IOException, SAXException, XPathExpressionException {
        final Document beansDoc = XmlUtil.parse(new ByteArrayInputStream( IOUtils.slurpUrl( BindingTest.class.getResource("gatewayManagementContext.xml") )));

        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext( new NamespaceContextImpl(XmlUtil.getNamespaceMap(beansDoc.getDocumentElement())) );
        return (NodeList) xpath.evaluate("//spr:bean[@id='resourceFactoryRegistry']/spr:constructor-arg/spr:list/spr:ref/@local", beansDoc, XPathConstants.NODESET);
    }

    private static NodeList findResourceFactoryClassAttributes(String refId) throws IOException, SAXException, XPathExpressionException {
        final Document beansDoc = XmlUtil.parse(new ByteArrayInputStream( IOUtils.slurpUrl( BindingTest.class.getResource("gatewayManagementContext.xml") )));

        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext( new NamespaceContextImpl(XmlUtil.getNamespaceMap(beansDoc.getDocumentElement())) );
        return (NodeList) xpath.evaluate("//spr:bean[@id='"+refId+"']/@class", beansDoc, XPathConstants.NODESET);
    }

    private static Map<String, String> generateSchemas() throws JAXBException, IOException {
        final Map<String,String> schemaResources = new HashMap<String,String>();

        final JAXBContext context = JAXBContext.newInstance( "com.l7tech.gateway.api" );
        context.generateSchema( new SchemaOutputResolver(){
            @Override
            public Result createOutput( final String namespaceUri, final String suggestedFileName ) throws IOException {
                final String systemId = ResourceHandler.MANAGEMENT_NAMESPACE.equals( namespaceUri ) ? "gateway-management.xsd" : suggestedFileName;
                return new StreamResult( systemId ){
                    {
                        setWriter( new StringWriter(){
                            @Override
                            public void close() throws IOException {
                                super.close();
                                schemaResources.put( systemId, toString() );
                            }
                        } );
                    }
                };
            }
        } );

//        for ( final Map.Entry<String,String> resourceEntry : schemaResources.entrySet() ) {
//            System.out.println( resourceEntry.getKey() + ":" );
//            System.out.println( resourceEntry.getValue() );
//        }
        
        return schemaResources;
    }
}
