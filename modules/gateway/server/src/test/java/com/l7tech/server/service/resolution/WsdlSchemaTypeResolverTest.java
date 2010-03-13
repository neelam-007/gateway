package com.l7tech.server.service.resolution;

import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.wsdl.PrettyGoodWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.InputSource;

import javax.wsdl.BindingOperation;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 
 */
public class WsdlSchemaTypeResolverTest {

    //- PUBLIC

    @BugNumber(8510)
    @Test
    public void testWsdlQNamesForMessageTypes() throws Exception {
        Wsdl wsdl = Wsdl.newInstance( new PrettyGoodWSDLLocator(new URL("http://localhost/transfer.wsdl"), new Functions.UnaryThrows<InputSource,URL, IOException>(){
            @Override
            public InputSource call( final URL url ) throws IOException {
                InputSource in = new InputSource(url.toString());
                if ( in.getSystemId().equals( "http://localhost/transfer.wsdl") ) {
                    in.setCharacterStream( new StringReader(TRANSFER_WSDL) );
                } else if ( in.getSystemId().equals( "http://localhost/transfer.xsd") ) {
                    in.setCharacterStream( new StringReader(TRANSFER_XSD) );
                } else {
                    in.setCharacterStream( new StringReader(BOOKS_XSD) );
                }
                return in;
            }
        }) );

        final WsdlSchemaTypeResolver typeResolver = new WsdlSchemaTypeResolver(wsdl) {
            @Override
            protected String resolveSchema( final String uri ) {
                String schemaXml = null;
                if ( uri.equals( "http://localhost/transfer.xsd") ) {
                    schemaXml = TRANSFER_XSD;
                } else if ( uri.equals( "http://localhost/books.xsd") ) {
                    schemaXml = BOOKS_XSD;
                }
                return schemaXml;
            }
        };

        for ( final BindingOperation bop : wsdl.getBindingOperations() ) {
            final Set<List<QName>> names = SoapUtil.getOperationPayloadQNames( bop, Wsdl.STYLE_DOCUMENT, null, typeResolver );
            if ( "Put".equals( bop.getName() ) ) {
                 assertEquals( "Put binding operation QNames", Collections.singleton( Collections.singletonList( new QName("urn:books","book") )), names);
            } else if ( "Delete".equals( bop.getName() ) ) {
                assertEquals( "Delete binding operation QNames", Collections.singleton(Collections.<QName>emptyList()), names);
            } else {
                fail("unexpected binding operation " + bop.getName());
            }
        }
    }

    //- PRIVATE

    private static final String TRANSFER_WSDL =
            "<wsdl:definitions \n" +
            "    targetNamespace=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" \n" +
            "    xmlns:tns=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" \n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
            "    xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" \n" +
            "    xmlns:wsoap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\"\n" +
            "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            " \n" +
            "  <wsdl:types>\n" +
            "    <xs:schema namespace=\"urn:uuid:e8cb0863-236e-4d16-a4ac-bc0c42ad01a5\">\n" +
            "      <xs:import namespace=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" schemaLocation=\"transfer.xsd\" />\n" +
            "      <xs:import namespace=\"urn:books\" schemaLocation=\"books.xsd\" />\n" +
            "    </xs:schema>\n" +
            "  </wsdl:types>\n" +
            " \n" +
            "  <wsdl:message name=\"EmptyMessage\"/>\n" +
            "  <wsdl:message name=\"AnyXmlMessage\">\n" +
            "    <wsdl:part name=\"Body\" type=\"tns:AnyXmlType\"/>\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:message name=\"OptionalXmlMessage\">\n" +
            "    <wsdl:part name=\"Body\" type=\"tns:AnyXmlOptionalType\"/>\n" +
            "  </wsdl:message>\n" +
            " \n" +
            "  <wsdl:portType name=\"ResourcePortType\">\n" +
            "    <wsdl:operation name=\"Put\">\n" +
            "      <wsdl:input \n" +
            "        message=\"tns:AnyXmlMessage\"\n" +
            "        wsa:Action=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/Put\" />\n" +
            "      <wsdl:output \n" +
            "        message=\"tns:OptionalXmlMessage\"\n" +
            "        wsa:Action=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse\" />\n" +
            "    </wsdl:operation>\n" +
            "    <wsdl:operation name=\"Delete\">\n" +
            "      <wsdl:input \n" +
            "        message=\"tns:EmptyMessage\"\n" +
            "        wsa:Action=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete\" />\n" +
            "      <wsdl:output \n" +
            "        message=\"tns:OptionalXmlMessage\"\n" +
            "        wsa:Action=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse\" />\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:portType>\n" +
            " \n" +
            "  <wsdl:binding name=\"ResourceBinding\" type=\"tns:ResourcePortType\">\n" +
            "    <wsoap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\" style=\"document\"/>\n" +
            "    <wsdl:operation name=\"Put\">\n" +
            "      <wsoap12:operation soapAction=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/Put\"/>\n" +
            "      <wsdl:input>\n" +
            "        <wsoap12:body use=\"literal\"/>\n" +
            "      </wsdl:input>\n" +
            "      <wsdl:output>\n" +
            "        <wsoap12:body use=\"literal\"/>\n" +
            "      </wsdl:output>\n" +
            "    </wsdl:operation>\n" +
            "    <wsdl:operation name=\"Delete\">\n" +
            "      <wsoap12:operation soapAction=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete\"/>\n" +
            "      <wsdl:input>\n" +
            "        <wsoap12:body use=\"literal\"/>\n" +
            "      </wsdl:input>\n" +
            "      <wsdl:output>\n" +
            "        <wsoap12:body use=\"literal\"/>\n" +
            "      </wsdl:output>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:binding>\n" +
            "</wsdl:definitions>";
    public static final String TRANSFER_XSD =
            "<xs:schema\n" +
            "    elementFormDefault=\"qualified\"\n" +
            "    blockDefault=\"#all\"\n" +
            "    targetNamespace=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\"\n" +
            "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\n" +
            "  <xs:complexType name=\"AnyXmlType\">\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##other\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "\n" +
            "  <xs:complexType name=\"AnyXmlOptionalType\">\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##other\" processContents=\"lax\" minOccurs=\"0\"/>\n" +
            "    </xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "</xs:schema>";
    private static final String BOOKS_XSD =
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:books\">\n" +
            "  <xs:element name=\"book\">\n" +
            "    <xs:complexType>\n" +
            "      <xs:sequence>\n" +
            "        <xs:element name=\"title\" type=\"xs:string\"/>\n" +
            "        <xs:element name=\"author\" type=\"xs:string\"/>\n" +
            "        <xs:element name=\"character\" minOccurs=\"0\" maxOccurs=\"unbounded\">\n" +
            "          <xs:complexType>\n" +
            "            <xs:sequence>\n" +
            "              <xs:element name=\"name\" type=\"xs:string\"/>\n" +
            "              <xs:element name=\"friend-of\" type=\"xs:string\" minOccurs=\"0\"\n" +
            "                               maxOccurs=\"unbounded\"/>\n" +
            "              <xs:element name=\"since\" type=\"xs:date\"/>\n" +
            "              <xs:element name=\"qualification\" type=\"xs:string\"/>\n" +
            "            </xs:sequence>\n" +
            "          </xs:complexType>\n" +
            "        </xs:element>\n" +
            "      </xs:sequence>\n" +
            "      <xs:attribute name=\"isbn\" type=\"xs:string\"/>\n" +
            "    </xs:complexType>\n" +
            "  </xs:element>\n" +
            "</xs:schema>";

}
