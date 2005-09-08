/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 29, 2005<br/>
 */
package com.l7tech.skunkworks.schemavalidation;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.IOException;

/**
 * A class that takes an xml document and decides whether it's a good schema or not.
 * Uses WSDP jars.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaCritic {

    // todo, move this to a proper junit test
    public static void main(String[] args) throws Exception {
        //        GOOD_SCHEMA
        //        BAD_SCHEMA1
        //        BAD_SCHEMA2 (can't find a way to catch errors in this schema)
        //        NOT_EVEN_A_SCHEMA
        //        NOT_EVEN_XML
        //        SCHEMA_WOTNS
        if (getSchemaTNS(SCHEMA_WOTNS) == null) {
            System.out.println("No tns");
        } else {
            System.out.println("All good");
        }
    }

    public static class BadSchemaException extends Exception {
        public BadSchemaException(String s){super(s);}
        public BadSchemaException(Throwable e){super(e.getMessage(), e);}
    }

    public static String getSchemaTNS(String schemaSrc) throws BadSchemaException {
        // 1. pass through the javax.xml.validation.SchemaFactory
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaSrc.getBytes())));
        } catch (Exception e) {
            throw new BadSchemaException(e);
        }
        // 2. pass through SchemaDocument
        SchemaDocument sdoc = null;
        try {
            sdoc = SchemaDocument.Factory.parse(new StringReader(schemaSrc));
        } catch (XmlException e) {
            throw new BadSchemaException(e);
        } catch (IOException e) {
            throw new BadSchemaException(e);
        }
        return sdoc.getSchema().getTargetNamespace();
    }

    private static final String GOOD_SCHEMA = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.acme.com/schemas/account\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  <s:element name=\"Account\">\n" +
            "    <s:complexType>\n" +
            "      <s:sequence>\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"CustomerId\" type=\"s:string\" />\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"Rep\" type=\"s:string\" />\n" +
            "      </s:sequence>\n" +
            "    </s:complexType>\n" +
            "  </s:element>\n" +
            "</s:schema>";

    private static final String BAD_SCHEMA1 =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\"\n" +
            "    targetNamespace=\"http://qaschematest9.layer7.com/\"\n" +
            "    xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\"\n" +
            "    xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\"\n" +
            "    xmlns:s=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:s0=\"http://qaschematest9.layer7.com/\"\n" +
            "    xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
            "    xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\">\n" +
            "    <s:element name=\"echoint\">\n" +
            "        <s:complexType>\n" +
            "            <s:attribute name=\"intinput\" type=\"s:int\" fixed=\"38\" default=\"38\"/>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"echointResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"echointResult\" type=\"s:int\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"int\" type=\"s:int\"/>\n" +
            "</s:schema>";

    private static final String BAD_SCHEMA2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\"\n" +
            "    targetNamespace=\"http://qaschematest.layer7.com/\"\n" +
            "    xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\"\n" +
            "    xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\"\n" +
            "    xmlns:s=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:s0=\"http://qaschematest.layer7.com/\"\n" +
            "    xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
            "    xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\">\n" +
            "<s:annotation>\n" +
            "  \t<s:documentation xml:lang=\"en\">\n" +
            "\t\tThis is a test schema for WebServiceForSchemaTest!\n" +
            "\t</s:documentation>\n" +
            "</s:annotation>\n" +
            "    <s:element name=\"EchoName\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "\t\t<s:element name=\"inName\">\n" +
            "        \t\t<s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:string\">\n" +
            "    \t\t\t\t\t<s:minLength value=\"15\"/>\n" +
            "    \t\t\t\t\t<s:maxLength value=\"30\"/>\n" +
            "\t\t\t\t\t<s:pattern value=\"([a-z])*\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"passstringtestype\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"FailPatternTest12345\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"failminlenth\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"failmaxlenthaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"/>\n" +
            "  \t\t\t\t</s:restriction>\n" +
            "\t\t\t</s:simpleType>\t\t\t\t\n" +
            "\t\t</s:element>\n" +
            "            </s:sequence>\t    \n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoNameResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"EchoNameResult\" type=\"s:string\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoGenda\">\n" +
            "\t<s:complexType>\n" +
            "            <s:sequence>\n" +
            "\t\t<s:element name=\"inGenda\">\n" +
            "        \t\t<s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:string\">\n" +
            "   \t\t\t\t\t<s:pattern value=\"male|female\"/> \t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t</s:simpleType>\t\t\t\t\n" +
            "\t\t</s:element>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoGendaResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"EchoGendaResult\" type=\"s:string\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoAge\">\n" +
            "\t<s:complexType>\n" +
            "            <s:sequence>\n" +
            "\t\t<s:element name=\"inAge\">\n" +
            "        \t\t<s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:int\">\n" +
            " \t\t\t\t\t<s:minInclusive value=\"29\"/>\n" +
            "    \t\t\t\t\t<s:maxInclusive value=\"47\"/> \n" +
            " \t \t\t\t\t<s:totalDigits value=\"2\"/>  \t\t\t\t\n" +
            "   \t\t\t\t\t<s:pattern value=\"[1-4][7-9]\"/> \t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t</s:simpleType>\t\t\t\t\n" +
            "\t\t</s:element>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoAgeResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                    name=\"EchoAgeResult\" type=\"s:int\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoDateTimeBorn\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element name=\"inDateTimeBorn\">\n" +
            "        \t\t<s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:dateTime\">\n" +
            "\t\t\t\t\t<s:minInclusive value=\"2003-01-01T08:01:59.0000000-08:00\"/>\n" +
            "    \t\t\t\t\t<s:maxInclusive value=\"2007-01-01T08:01:59.0000000-08:00\"/> \t\t\t\n" +
            "\t\t\t\t\t<s:enumeration value=\"2004-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2002-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2009-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2005-09-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:pattern value=\"200[0-9]-0[1-8]-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{7}-\\d{2}:\\d{2}\"/>\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t</s:simpleType>\t\t\t\t\n" +
            "\t\t</s:element>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoDateTimeBornResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                    name=\"EchoDateTimeBornResult\" type=\"s:dateTime\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoMarried\">\n" +
            "\t<s:complexType>\n" +
            "            <s:sequence>\n" +
            "\t\t<s:element name=\"inMarried\">\n" +
            "        \t\t<s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:boolean\">\n" +
            " \t\t\t\t\t<s:pattern value=\"true|false\"/> \t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t</s:simpleType>\t\t\t\t\n" +
            "\t\t</s:element>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "     </s:element>\n" +
            "    <s:element name=\"EchoMarriedResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                    name=\"EchoMarriedResult\" type=\"s:boolean\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoHomeAddress\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"inHomeAddress\" type=\"s:string\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoHomeAddressResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"EchoHomeAddressResult\" type=\"s:string\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoHomePhoneNumber\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                    name=\"inHomePhoneNumber\" type=\"s:long\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoHomePhoneNumberResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                    name=\"EchoHomePhoneNumberResult\" type=\"s:long\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"Echo\">\n" +
            "        <s:complexType>\n" +
            "  \t   <s:sequence>\n" +
            "             <s:element name=\"inPerson\">\n" +
            "\t\t<s:complexType>\n" +
            "\t            <s:sequence>\n" +
            "\t\t        <s:element maxOccurs=\"2\" minOccurs=\"0\" name=\"Name\" type=\"s:string\"/>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Genda\" type=\"s:string\"/>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"Age\">\n" +
            "\t\t\t     <s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:int\">\n" +
            " \t\t\t\t\t<s:minInclusive value=\"29\"/>\n" +
            "    \t\t\t\t\t<s:maxInclusive value=\"47\"/> \n" +
            " \t \t\t\t\t<s:totalDigits value=\"2\"/>  \t\t\t\t\n" +
            "   \t\t\t\t\t<s:pattern value=\"[1-4][7-9]\"/> \t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t     </s:simpleType>\n" +
            "    \t\t\t</s:element>\n" +
            "\t\t\t<s:element name=\"DateTimeBorn\">\n" +
            "        \t\t     <s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:dateTime\">\n" +
            "  \t\t\t \t\t<s:minInclusive value=\"2003-01-01T08:01:59.0000000-08:00\"/>\n" +
            "    \t\t\t\t\t<s:maxInclusive value=\"2007-01-01T08:01:59.0000000-08:00\"/> \t\t\t\n" +
            "\t\t\t\t\t<s:enumeration value=\"2004-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2002-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2009-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2005-09-09T08:01:59.0000000-08:00\"/>\n" +
            "\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t             </s:simpleType>\t\t\t\t\n" +
            "\t\t\t</s:element>\n" +
            "\t\t        <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"Married\">\n" +
            "        \t\t     <s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:boolean\">\n" +
            " \t\t\t\t\t<s:pattern value=\"([TFt][RAr][ULu][ESe])*\"/> \t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t     </s:simpleType>\t\t\t\t\n" +
            "\t\t\t</s:element>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"HomeAddress\" type=\"s:string\"/>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"HomePhoneNumber\" type=\"s:long\"/>\n" +
            "            \t    </s:sequence>\n" +
            "\t\t</s:complexType>\n" +
            "            </s:element>\n" +
            "    \t  </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"EchoResponse\">\n" +
            "        <s:complexType>\n" +
            "           <s:sequence>\n" +
            "\t      <s:element name=\"EchoResult\">\n" +
            "\t\t<s:complexType>\n" +
            "\t            <s:sequence>\n" +
            "\t\t        <s:element maxOccurs=\"2\" minOccurs=\"0\" name=\"Name\" type=\"s:string\"/>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Genda\" type=\"s:string\"/>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"Age\">\n" +
            "\t\t\t     <s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:int\">\n" +
            " \t\t\t\t\t<s:minInclusive value=\"29\"/>\n" +
            "    \t\t\t\t\t<s:maxInclusive value=\"47\"/> \n" +
            " \t \t\t\t\t<s:totalDigits value=\"2\"/>  \t\t\t\t\n" +
            "   \t\t\t\t\t<s:pattern value=\"[1-4][7-9]\"/> \t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t     </s:simpleType>\n" +
            "    \t\t\t</s:element>\n" +
            "\t\t\t<s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"DateTimeBorn\">\n" +
            "        \t\t     <s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:dateTime\">\n" +
            "  \t\t\t \t\t<s:minInclusive value=\"2003-01-01T08:01:59.0000000-08:00\"/>\n" +
            "    \t\t\t\t\t<s:maxInclusive value=\"2007-01-01T08:01:59.0000000-08:00\"/> \t\t\t\n" +
            "\t\t\t\t\t<s:enumeration value=\"2004-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2002-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2009-02-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2005-09-09T08:01:59.0000000-08:00\"/>\n" +
            "\t\t\t\t\t<s:enumeration value=\"2003-11-11T00:00:00.0000000-08:00\"/>\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t             </s:simpleType>\t\t\t\t\n" +
            "\t\t\t</s:element>\n" +
            "\t\t\t<s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"Married\">\n" +
            "        \t\t     <s:simpleType>\n" +
            "\t\t\t\t<s:restriction base=\"s:boolean\">\n" +
            " \t\t\t\t\t\n" +
            "\t\t\t\t</s:restriction>\n" +
            "\t\t\t     </s:simpleType>\t\t\t\t\n" +
            "\t\t\t</s:element>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"HomeAddress\" type=\"s:string\"/>\n" +
            "            \t\t<s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"HomePhoneNumber\" type=\"s:long\"/>\n" +
            "            \t    </s:sequence>\n" +
            "\t\t</s:complexType>\n" +
            "\t     </s:element>\n" +
            "           </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"string\" nillable=\"true\" type=\"s:string\"/>\n" +
            "    <s:element name=\"int\" type=\"s:int\"/>\n" +
            "    <s:element name=\"dateTime\" type=\"s:dateTime\"/>\n" +
            "    <s:element name=\"boolean\" type=\"s:boolean\"/>\n" +
            "    <s:element name=\"long\" type=\"s:long\"/>\n" +
            "</s:schema>";

    private static final String SCHEMA_WOTNS = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  <s:element name=\"Account\">\n" +
            "    <s:complexType>\n" +
            "      <s:sequence>\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"CustomerId\" type=\"s:string\" />\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"Rep\" type=\"s:string\" />\n" +
            "      </s:sequence>\n" +
            "    </s:complexType>\n" +
            "  </s:element>\n" +
            "</s:schema>";

    private static final String NOT_EVEN_A_SCHEMA = "<soap:Envelope xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsse11=\"http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            " <soap:Header>\n" +
            "     <PingHeader xmlns=\"http://xmlsoap.org/Ping\">Layer 7 Technologies - Scenario #8</PingHeader>\n" +
            " </soap:Header>\n" +
            " <soap:Body wsu:Id=\"Body\">\n" +
            "     <Ping xmlns=\"http://xmlsoap.org/Ping\">Layer 7 Technologies - Scenario #8</Ping>\n" +
            " </soap:Body>\n" +
            "</soap:Envelope>";

    private static final String NOT_EVEN_XML = "lsdkjlfgdskjglfskjglsf";
}
