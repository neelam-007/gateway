/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jun 10, 2005<br/>
 */
package com.l7tech.skunkworks.schemavalidation;

import com.l7tech.common.io.XmlUtil;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.xml.sax.*;

/**
 * A simple class that validates a document against a schema.
 * Consider making this a prod class and have ServerSchemaValidation use it.
 *
 * @author flascelles@layer7-tech.com
 */
public class Validator {
    private final Logger logger = Logger.getLogger(Validator.class.getName());

    /**
     * Expect a SAXParseException if the document is not valid, no exception means it is valid.
     */
    public void validate(InputStream schemaStream, InputStream documentStream) throws SAXParseException, SAXException, ParserConfigurationException, IOException {
        validate(schemaStream, documentStream, XmlUtil.getSafeEntityResolver());
    }

    public void validate(InputStream schemaStream, InputStream documentStream, EntityResolver entityResolver) throws SAXParseException, SAXException, ParserConfigurationException, IOException {
        if (schemaStream == null || documentStream == null || entityResolver == null) throw new NullPointerException();
        final ArrayList errors = new ArrayList();
        ErrorHandler errorHandler = new ErrorHandler() {
            public void warning(SAXParseException e) {
                errors.add(e);
                logger.log(Level.WARNING, "SAX Error during validation", e);
            }
            public void error(SAXParseException e) {
                errors.add(e);
                logger.log(Level.WARNING, "SAX Error during validation", e);
            }
            public void fatalError(SAXParseException e) {
                errors.add(e);
                logger.log(Level.WARNING, "SAX Error during validation", e);
            }
        };

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setAttribute(XmlUtil.XERCES_DISALLOW_DOCTYPE, Boolean.TRUE);
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(true);
        dbf.setAttribute(XmlUtil.JAXP_SCHEMA_LANGUAGE, XmlUtil.W3C_XML_SCHEMA);
	    // Specify other factory configuration settings
	    dbf.setAttribute(XmlUtil.JAXP_SCHEMA_SOURCE, schemaStream);
        DocumentBuilder db = null;
        db = dbf.newDocumentBuilder();
        db.setEntityResolver(entityResolver);
        db.setErrorHandler(errorHandler);

        InputSource source = new InputSource(documentStream);
        db.parse(source);
        if (!errors.isEmpty()) {
            SAXParseException firstError = (SAXParseException)errors.get(0);
            throw firstError;
        }
    }

    public static void main(String[] args) throws Exception {
        String docToValidate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "       <Echo xmlns=\"http://qaschematest.layer7.com/\">\n" +
                "            <inPerson>\n" +
                "                <Name>Michael Qiu</Name>\n" +
                "                <Genda>Male</Genda>\n" +
                "                <Age>33</Age>\n" +
                "                <DateTimeBorn>2004-02-09T01:02:03.0000000-08:00</DateTimeBorn>\n" +
                "                <Married>true</Married>\n" +
                "                <HomeAddress>123 Betty St. Vancouver</HomeAddress>\n" +
                "                <HomePhoneNumber>6041234567</HomePhoneNumber>\n" +
                "            </inPerson>\n" +
                "        </Echo>";

        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<s:schema elementFormDefault=\"qualified\"\n" +
                "    targetNamespace=\"http://qaschematest.layer7.com/\"\n" +
                "    xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\"\n" +
                "    xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\"\n" +
                "    xmlns:s=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "    xmlns:s0=\"http://qaschematest.layer7.com/\"\n" +
                "    xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n" +
                "    xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\">\n" +
                "      <s:element name=\"Echo\">\n" +
                "        <s:complexType>\n" +
                "            <s:sequence>\n" +
                "            <s:element name=\"inPerson\">\n" +
                "                        <s:complexType>\n" +
                "                        <s:sequence>\n" +
                "                                <s:element maxOccurs=\"2\" minOccurs=\"0\" name=\"Name\" type=\"s:string\"/>\n" +
                "                                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"Genda\" type=\"s:string\"/>\n" +
                "                                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"Age\" type=\"s:int\"/>\n" +
                "                                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"DateTimeBorn\" type=\"s:dateTime\"/>\n" +
                "                                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"Married\" type=\"s:boolean\"/>\n" +
                "                                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"HomeAddress\" type=\"s:string\"/>\n" +
                "                                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"HomePhoneNumber\" type=\"s:long\"/>\n" +
                "                            </s:sequence>\n" +
                "                        </s:complexType>\n" +
                "                </s:element>\n" +
                "            </s:sequence>\n" +
                "        </s:complexType>\n" +
                "    </s:element>\n" +
                "    \n" +
                "    <s:element name=\"string\" nillable=\"true\" type=\"s:string\"/>\n" +
                "    <s:element name=\"int\" type=\"s:int\"/>\n" +
                "    <s:element name=\"dateTime\" type=\"s:dateTime\"/>\n" +
                "    <s:element name=\"boolean\" type=\"s:boolean\"/>\n" +
                "    <s:element name=\"long\" type=\"s:long\"/>\n" +
                "</s:schema>";

        Validator me = new Validator();
        me.validate(new ByteArrayInputStream(schema.getBytes()), new ByteArrayInputStream(docToValidate.getBytes()));

        schema = "<s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.layer7.com/schemas/blah\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "        <s:element name=\"foo\">\n" +
                "                <s:complexType>\n" +
                "                        <s:sequence>\n" +
                "                                <s:element name=\"foochild1\" type=\"s:int\" maxOccurs=\"1\" minOccurs=\"1\"/>\n" +
                "                                <s:element name=\"foochild2\" maxOccurs=\"1\" minOccurs=\"1\">\n" +
                "                                        <s:complexType>\n" +
                "                                                <s:sequence>\n" +
                "                                                        <s:any maxOccurs=\"unbounded\" minOccurs=\"0\" namespace=\"##other\" processContents=\"lax\"/>\n" +
                "                                                </s:sequence>\n" +
                "                                        </s:complexType>\n" +
                "                                </s:element>\n" +
                "                        </s:sequence>\n" +
                "                </s:complexType>\n" +
                "        </s:element>\n" +
                "</s:schema>";
        docToValidate = "<grr:foo xmlns:grr=\"http://www.layer7.com/schemas/blah\">\n" +
                "  <grr:foochild1>666</grr:foochild1>\n" +
                "  <grr:foochild2/>\n" +
                "</grr:foo>";
        docToValidate = "<grr:foo xmlns:grr=\"http://www.layer7.com/schemas/blah\">\n" +
                "  <grr:foochild1>666</grr:foochild1>\n" +
                "  <grr:foochild2>\n" +
                "<beep:blah xmlns:beep=\"http://john.com\"/>" +
                "  </grr:foochild2>\n" +
                "</grr:foo>";

        me.validate(new ByteArrayInputStream(schema.getBytes()), new ByteArrayInputStream(docToValidate.getBytes()));
    }
}
