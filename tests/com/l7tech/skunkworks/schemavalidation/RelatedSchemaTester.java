/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 2, 2005<br/>
 */
package com.l7tech.skunkworks.schemavalidation;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.logging.Logger;

import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.LSInputImpl;

/**
 * This tests using EntityResolver to do software schema validation through multiple schemas that relate to each other
 *
 * @author flascelles@layer7-tech.com
 */
public class RelatedSchemaTester {
    //private static final String POXSD = "com/l7tech/service/resources/relatedschemas/purchaseOrder.xsd";
    //private static final String MSG = "com/l7tech/service/resources/relatedschemas/validSample.xml";
    //private static final String ACCNTXSD = "com/l7tech/service/resources/relatedschemas/account.xsd";

    private final Logger logger = Logger.getLogger(RelatedSchemaTester.class.getName());
    private final Validator validator = new Validator();

    public static void main(String[] args) throws Exception {
        RelatedSchemaTester tester = new RelatedSchemaTester();
        tester.dothetest();
    }

    public void dothetest() throws Exception {
        // get root schema
        InputStream schema = new ByteArrayInputStream(SOURCE_SCHEMA.getBytes());
        // get sample doc
        InputStream doc = new ByteArrayInputStream(SOURCE_MESSAGE.getBytes());
        // validate the doc
        validator.validate(schema, doc, getRealEntityResolver(), getRealLSResourceResolver());
    }

    protected EntityResolver getRealEntityResolver() {
        return new EntityResolver () {
            private final String HOMEDIR = System.getProperty("user.dir");
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                // by default, the parser constructs a systemId in the form of a url "file:///user.dir/filename"
                String schemaId = systemId;
                if (systemId != null && HOMEDIR != null) {
                    int pos = systemId.indexOf(HOMEDIR);
                    if (pos > -1) {
                        schemaId = systemId.substring(pos+HOMEDIR.length()+1);
                    }
                }
                if(systemId !=null && systemId.startsWith("http://fake/fakeuout/faker/")) {
                    schemaId = systemId.substring("http://fake/fakeuout/faker/".length());
                }
                logger.info("asking for resource with systemId " + systemId + ", schemaId " + schemaId);
                // todo, get schema based on the schemaId instead of the hardcoded example below

                if(schemaId.endsWith("account.xsd")) {
                    if(!"account.xsd".equals(schemaId)) {
                        logger.warning("RESOLUTION WOULD FAIL with systemId '" + systemId + "', schemaId '" + schemaId + "'.");
                    }
                    return new InputSource(new ByteArrayInputStream(RELATED_SCHEMA_1.getBytes()));
                }
                if(schemaId.endsWith("delivery.xsd")) {
                    if(!"delivery.xsd".equals(schemaId)) {
                        logger.warning("RESOLUTION WOULD FAIL with systemId '" + systemId + "', schemaId '" + schemaId + "'.");
                    }
                    return new InputSource(new ByteArrayInputStream(RELATED_SCHEMA_2.getBytes()));
                }
                return null;
            }
        };
    }

    protected LSResourceResolver getRealLSResourceResolver() {
        return new LSResourceResolver () {
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, final String baseURI) {
                LSInput lsInput = new LSInputImpl();
                if(XmlUtil.W3C_XML_SCHEMA.equals(type)) { // check we are resolving schema
                    String schemaId = systemId;
                    if(systemId !=null && systemId.startsWith("http://fake/fakeuout/faker/")) {
                        schemaId = systemId.substring("http://fake/fakeuout/faker/".length());
                    }
                    logger.info("asking for resource with systemId " + systemId + ", schemaId " + schemaId + ", baseURI " + baseURI);

                    if("account.xsd".equals(schemaId)) {
                        lsInput.setCharacterStream(new StringReader(RELATED_SCHEMA_1));
                    }
                    else if("delivery.xsd".equals(schemaId)) {
                        lsInput.setCharacterStream(new StringReader(RELATED_SCHEMA_2));
                    }
                }
                else {
                    logger.info("Not resolving resource of non-schema type '"+type+"', systemId is '"+systemId+"'.");
                }
                return lsInput; // if we return null the schema would be resolved over the network.
            }
        };
    }



    protected InputStream getRes(String resname) {
        return getClass().getClassLoader().getResourceAsStream(resname);
    }


    private static final String SOURCE_MESSAGE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<po:PurchaseOrder xmlns:po=\"http://www.acme.com/schemas/purchaseOrder\">\n" +
            "  <po:Quantity>10</po:Quantity>\n" +
            "  <po:ItemId>sdfdslkhs6</po:ItemId>\n" +
            "  <id:Account xmlns:id=\"http://www.acme.com/schemas/account\">\n" +
            "    <id:CustomerId>sdkfjsd89fsj</id:CustomerId>\n" +
            "    <id:Rep>John Doe</id:Rep>\n" +
            "  </id:Account>\n" +
            "  <dd:Delivery xmlns:dd=\"http://www.acme.com/schemas/delivery\">\n" +
            "    <dd:Month>July</dd:Month>\n" +
            "    <dd:Year>2005</dd:Year>\n" +
            "  </dd:Delivery>\n" +
            "</po:PurchaseOrder>";
    private static final String SOURCE_SCHEMA = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\"\n" +
            "          targetNamespace=\"http://www.acme.com/schemas/purchaseOrder\"\n" +
            "          xmlns:accnt=\"http://www.acme.com/schemas/account\"\n" +
            "\t  xmlns:dd=\"http://www.acme.com/schemas/delivery\"\n" +
            "          xmlns:s=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  \n" +
            "  <s:import namespace=\"http://www.acme.com/schemas/account\" schemaLocation=\"account.xsd\" />\n" +
            "  <s:import namespace=\"http://www.acme.com/schemas/delivery\" schemaLocation=\"delivery.xsd\" />\n" +
            "  \n" +
            "  <s:element name=\"PurchaseOrder\">\n" +
            "    <s:complexType>\n" +
            "      <s:sequence>\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"Quantity\" type=\"s:int\" />\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"ItemId\" type=\"s:string\" />\n" +
            "        <s:element ref=\"accnt:Account\" />\n" +
            "\t<s:element ref=\"dd:Delivery\" />\n" +
            "      </s:sequence>\n" +
            "    </s:complexType>\n" +
            "  </s:element>\n" +
            "</s:schema>";
    private static final String RELATED_SCHEMA_1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
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
    private static final String RELATED_SCHEMA_2 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.acme.com/schemas/delivery\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  <s:element name=\"Delivery\">\n" +
            "    <s:complexType>\n" +
            "      <s:sequence>\n" +
            "        <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"Month\" type=\"s:string\" />\n" +
            "        <s:element minOccurs=\"1\" maxOccurs=\"1\" name=\"Year\" type=\"s:int\" />\n" +
            "      </s:sequence>\n" +
            "    </s:complexType>\n" +
            "  </s:element>\n" +
            "</s:schema>";
}
