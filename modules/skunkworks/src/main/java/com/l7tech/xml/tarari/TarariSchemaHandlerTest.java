/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.xml.tarari;

import com.l7tech.message.Message;
import com.l7tech.message.TarariKnob;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariSchemaHandler;
import com.l7tech.xml.tarari.TarariMessageContext;
import com.l7tech.common.io.XmlUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author alex
 */
public class TarariSchemaHandlerTest extends TestCase {

    public TarariSchemaHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TarariSchemaHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        System.setProperty("com.l7tech.common.xml.tarari.enable", "true");
        TarariLoader.compile();
    }

    // disabled because there is now no way to populate TarariSchemaHandler without going through the SchemaManager
    public void DISABLED_testStuff() throws Exception {
        TarariSchemaHandler tsh = TarariLoader.getSchemaHandler();
        assertNotNull(tsh);

//        tsh.loadHardware("/tmp/foo", SOAPENV_SCHEMA);
//        tsh.loadHardware("/tmp/bar", WAREHOUSE_SCHEMA);

        Message msg = new Message( XmlUtil.stringToDocument(MESSAGE));
        assertTrue(msg.isSoap());
        TarariKnob tk = (TarariKnob) msg.getKnob(TarariKnob.class);
        assertNotNull(tk);

        TarariMessageContext tmc = tk.getContext();
        assertNotNull(tmc);
        boolean valid = tsh.validate(tmc);
        assertTrue(valid);
    }

    public static final String MESSAGE = "<SOAP-ENV:Envelope xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><namesp1:listProducts xmlns:namesp1=\"http://warehouse.acme.com/ws\"></namesp1:listProducts></SOAP-ENV:Body></SOAP-ENV:Envelope>";

    public static final String WAREHOUSE_SCHEMA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\"\n" +
            "    targetNamespace=\"http://warehouse.acme.com/ws\"\n" +
            "    xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\"\n" +
            "    xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\"\n" +
            "    xmlns:s=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
            "    xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\"\n" +
            "    xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "    xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\"\n" +
            "    xmlns:tns=\"http://warehouse.acme.com/ws\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "    <s:element name=\"listProducts\"/>\n" +
            "    <s:element name=\"listProductsResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"listProductsResult\" type=\"tns:ArrayOfProductListHeader\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:complexType name=\"ArrayOfProductListHeader\">\n" +
            "        <s:sequence>\n" +
            "            <s:element maxOccurs=\"unbounded\" minOccurs=\"0\"\n" +
            "                name=\"ProductListHeader\" nillable=\"true\" type=\"tns:ProductListHeader\"/>\n" +
            "        </s:sequence>\n" +
            "    </s:complexType>\n" +
            "    <s:complexType name=\"ProductListHeader\">\n" +
            "        <s:sequence>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"productName\" type=\"s:string\"/>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"productId\" type=\"s:long\"/>\n" +
            "        </s:sequence>\n" +
            "    </s:complexType>\n" +
            "    <s:element name=\"getProductDetails\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"productid\" type=\"s:long\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"getProductDetailsResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"getProductDetailsResult\" type=\"tns:ProductDetails\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:complexType name=\"ProductDetails\">\n" +
            "        <s:complexContent mixed=\"false\">\n" +
            "            <s:extension base=\"tns:ProductListHeader\">\n" +
            "                <s:sequence>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                        name=\"description\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"price\" type=\"s:float\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                        name=\"inStock\" type=\"s:boolean\"/>\n" +
            "                </s:sequence>\n" +
            "            </s:extension>\n" +
            "        </s:complexContent>\n" +
            "    </s:complexType>\n" +
            "    <s:element name=\"placeOrder\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"productid\" type=\"s:long\"/>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"amount\" type=\"s:long\"/>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"price\" type=\"s:float\"/>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"accountid\" type=\"s:long\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"placeOrderResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\"\n" +
            "                    name=\"placeOrderResult\" type=\"s:long\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"currentOrders\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"accountid\" type=\"s:long\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:element name=\"currentOrdersResponse\">\n" +
            "        <s:complexType>\n" +
            "            <s:sequence>\n" +
            "                <s:element maxOccurs=\"1\" minOccurs=\"0\"\n" +
            "                    name=\"currentOrdersResult\" type=\"tns:ArrayOfOrder\"/>\n" +
            "            </s:sequence>\n" +
            "        </s:complexType>\n" +
            "    </s:element>\n" +
            "    <s:complexType name=\"ArrayOfOrder\">\n" +
            "        <s:sequence>\n" +
            "            <s:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"Order\"\n" +
            "                nillable=\"true\" type=\"tns:Order\"/>\n" +
            "        </s:sequence>\n" +
            "    </s:complexType>\n" +
            "    <s:complexType name=\"Order\">\n" +
            "        <s:sequence>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"date\" type=\"s:dateTime\"/>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"accountId\" type=\"s:long\"/>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"productId\" type=\"s:long\"/>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"amount\" type=\"s:long\"/>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"price\" type=\"s:float\"/>\n" +
            "            <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"confirmationId\" type=\"s:long\"/>\n" +
            "        </s:sequence>\n" +
            "    </s:complexType>\n" +
            "</s:schema>";

    public static final String SOAPENV_SCHEMA = "<?xml version='1.0' encoding='UTF-8' ?>\n" +
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "           xmlns:tns=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           targetNamespace=\"http://schemas.xmlsoap.org/soap/envelope/\" >\n" +
            "  <!-- Envelope, header and body -->\n" +
            "  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n" +
            "  \n" +
            "  <xs:complexType name=\"Envelope\" >\n" +
            "    <xs:sequence>\n" +
            "      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n" +
            "      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n" +
            "      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
            "  </xs:complexType>\n" +
            "  <xs:element name=\"Header\" type=\"tns:Header\" />\n" +
            "  <xs:complexType name=\"Header\" >\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n" +
            "  </xs:complexType>\n" +
            "  <xs:element name=\"Body\" type=\"tns:Body\" />\n" +
            "  <xs:complexType name=\"Body\" >\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" >\n" +
            "          <xs:annotation>\n" +
            "            <xs:documentation>\n" +
            "                  Prose in the spec does not specify that attributes are allowed on the Body element\n" +
            "                </xs:documentation>\n" +
            "          </xs:annotation>\n" +
            "        </xs:anyAttribute>\n" +
            "  </xs:complexType>\n" +
            "  <!-- Global Attributes.  The following attributes are intended to be usable via qualified attribute names on any complex type referencing them.  -->\n" +
            "  <xs:attribute name=\"mustUnderstand\" >\n" +
            "     <xs:simpleType>\n" +
            "     <xs:restriction base='xs:boolean'>\n" +
            "           <xs:pattern value='0|1' />\n" +
            "         </xs:restriction>\n" +
            "   </xs:simpleType>\n" +
            "  </xs:attribute>\n" +
            "  <xs:attribute name=\"actor\" type=\"xs:anyURI\" />\n" +
            "  <xs:simpleType name=\"encodingStyle\" >\n" +
            "    <xs:annotation>\n" +
            "          <xs:documentation>\n" +
            "            'encodingStyle' indicates any canonicalization conventions followed in the contents of the containing element.  For example, the value 'http://schemas.xmlsoap.org/soap/encoding/' indicates the pattern described in SOAP specification\n" +
            "          </xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "    <xs:list itemType=\"xs:anyURI\" />\n" +
            "  </xs:simpleType>\n" +
            "  <xs:attribute name=\"encodingStyle\" type=\"tns:encodingStyle\" />\n" +
            "  <xs:attributeGroup name=\"encodingStyle\" >\n" +
            "    <xs:attribute ref=\"tns:encodingStyle\" />\n" +
            "  </xs:attributeGroup>  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n" +
            "  <xs:complexType name=\"Fault\" final=\"extension\" >\n" +
            "    <xs:annotation>\n" +
            "          <xs:documentation>\n" +
            "            Fault reporting structure\n" +
            "          </xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "    <xs:sequence>\n" +
            "      <xs:element name=\"faultcode\" type=\"xs:QName\" />\n" +
            "      <xs:element name=\"faultstring\" type=\"xs:string\" />\n" +
            "      <xs:element name=\"faultactor\" type=\"xs:anyURI\" minOccurs=\"0\" />\n" +
            "      <xs:element name=\"detail\" type=\"tns:detail\" minOccurs=\"0\" />\n" +
            "    </xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "  <xs:complexType name=\"detail\">\n" +
            "    <xs:sequence>\n" +
            "      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n" +
            "    </xs:sequence>\n" +
            "    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" />\n" +
            "  </xs:complexType>\n" +
            "</xs:schema>";

}
