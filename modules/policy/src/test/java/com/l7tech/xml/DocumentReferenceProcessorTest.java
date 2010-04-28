package com.l7tech.xml;

import com.l7tech.common.io.XmlUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;

/**
 * 
 */
public class DocumentReferenceProcessorTest {

    private static final String TEST_WSDL =
            "<wsdl:definitions xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "    <wsdl:import location=\"other.wsdl\"/>\n" +
            "    <wsdl:types>\n" +
            "        <s:schema>\n" +
            "            <s:import namespace=\"urn:imported\" schemaLocation=\"schema.xsd\"/>\n" +
            "        </s:schema>\n" +
            "    </wsdl:types>" +
            "</wsdl:definitions>";

    private static final String TEST_SCHEMA =
            "<s:schema xmlns:s=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <s:import namespace=\"urn:namespace1\"/>\n" +
            "    <s:import schemaLocation=\"imported.xsd\" namespace=\"urn:namespace2\"/>\n" +
            "    <s:include schemaLocation=\"included.xsd\"/>\n" +
            "    <s:redefine schemaLocation=\"redefined.xsd\"/>\n" +
            "</s:schema>";

    @Test
    public void testFindSchemaDependencies() throws Exception {
        final Document schemaDoc = XmlUtil.parse( TEST_SCHEMA );
        final java.util.List<Element> dependencyElements = new ArrayList<Element>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaDoc, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                dependencyElements.add( (Element)node );
                return null;
            }
        } );

        assertFalse("No dependencies found", dependencyElements.isEmpty());
        assertEquals("Dependency count", 4, dependencyElements.size());
    }

    @Test
    public void testRewriteReferences() throws Exception {
        final DocumentReferenceProcessor documentReferenceProcessor = new DocumentReferenceProcessor();
        final DocumentReferenceProcessor.ReferenceCustomizer customizer = new DocumentReferenceProcessor.ReferenceCustomizer() {
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                return referenceInfo.getReferenceUrl() == null ? null : referenceInfo.getReferenceUrl()+"_REWRITTEN";
            }
        };

        final Document wsdlDoc = XmlUtil.parse( TEST_WSDL );
        documentReferenceProcessor.processDocumentReferences( wsdlDoc, customizer );
        final String updatedWsdlDoc = XmlUtil.nodeToString( wsdlDoc );
        assertTrue( "Reference not rewritten", updatedWsdlDoc.contains( "other.wsdl_REWRITTEN" ));
        assertTrue( "Reference not rewritten", updatedWsdlDoc.contains( "schema.xsd_REWRITTEN" ));

        final Document schemaDoc = XmlUtil.parse( TEST_SCHEMA );
        documentReferenceProcessor.processDocumentReferences( schemaDoc, customizer );
        final String updatedSchemaDoc = XmlUtil.nodeToString( schemaDoc );
        assertTrue( "Reference not rewritten", updatedSchemaDoc.contains( "imported.xsd_REWRITTEN" ));
        assertTrue( "Reference not rewritten", updatedSchemaDoc.contains( "included.xsd_REWRITTEN" ));
        assertTrue( "Reference not rewritten", updatedSchemaDoc.contains( "redefined.xsd_REWRITTEN" ));
    }
}
