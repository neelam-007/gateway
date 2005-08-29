/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 29, 2005<br/>
 */
package com.l7tech.skunkworks.schemavalidation;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;

/**
 * A class that takes an xml document and decides whether it's a good schema or not.
 * Uses WSDP jars.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaCritic {

    // todo, move this to a proper junit test
    public static void main(String[] args) throws Exception {
        if (isValidSchema(GOOD_SCHEMA)) {
            System.out.println("OK");
        } else {
            System.out.println("unexpected result with GOOD_SCHEMA");
        }

        if (!isValidSchema(BAD_SCHEMA1)) {
            System.out.println("OK");
        } else {
            System.out.println("unexpected result with BAD_SCHEMA1");
        }

        if (!isValidSchema(NOT_EVEN_A_SCHEMA)) {
            System.out.println("OK");
        } else {
            System.out.println("unexpected result with NOT_EVEN_A_SCHEMA");
        }

        if (!isValidSchema(NOT_EVEN_XML)) {
            System.out.println("OK");
        } else {
            System.out.println("unexpected result with NOT_EVEN_XML");
        }

        if (!isValidSchema(SCHEMA_WOTNS)) {
            System.out.println("OK");
        } else {
            System.out.println("unexpected result with SCHEMA_WOTNS");
        }
    }

    public static boolean isValidSchema(String maybeSchemaXml) {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.newSchema(new StreamSource(new ByteArrayInputStream(maybeSchemaXml.getBytes())));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("NOT VALID");
            return false;
        }
        return true;
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
