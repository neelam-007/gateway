package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.WsdlSchemaAnalizer;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for schema validation code.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 3, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationTest extends TestCase {

    public SchemaValidationTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(SchemaValidationTest.class);
        return suite;
    }

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
        System.out.println("Test complete: " + SchemaValidationTest.class);
    }

    protected void setUp() throws Exception {
        GlobalTarariContext context = TarariLoader.getGlobalContext();
        if (context != null) {
            context.compile();
        }
    }

    public void testEcho() throws Exception {
        SchemaValidation assertion = new SchemaValidation();
        InputStream is = TestDocuments.getInputStream(ECHO3_XSD);
        String xsd = new String(HexUtils.slurpStream(is, 10000));
        assertion.setSchema(xsd);
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, ApplicationContexts.getTestApplicationContext());
        AssertionStatus res = serverAssertion.checkRequest(getResAsContext(ECHO_REQ));
        System.out.println("result is " + res);
    }

    public void testCaseWith2BodyChildren() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        WsdlSchemaAnalizer wsn = new WsdlSchemaAnalizer(TestDocuments.getTestDocument(DOCLIT_WSDL_WITH2BODYCHILDREN));
        Element[] schemas = wsn.getFullSchemas();
        assertTrue(schemas.length == 1); // no multiple schema support
        assertion.setSchema(XmlUtil.elementToXml(schemas[0]));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, ApplicationContexts.getTestApplicationContext());

        // try to validate a number of different soap messages
        String[] resources = {DOCLIT_WITH2BODYCHILDREN_REQ};
        boolean[] expectedResults = {true};
        for (int i = 0; i < resources.length; i++) {
            AssertionStatus res = serverAssertion.checkRequest(getResAsContext(resources[i]));
            //System.out.println("DOCUMENT " + resources[i] +
            //                    (res == AssertionStatus.NONE ? " VALIDATES OK" : " DOES NOT VALIDATE"));
            if (expectedResults[i]) {
                assertTrue(res == AssertionStatus.NONE);
            } else {
                assertFalse(res == AssertionStatus.NONE);
            }
        }
    }

    public void testWarehouseValidations() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        WsdlSchemaAnalizer wsn = new WsdlSchemaAnalizer(TestDocuments.getTestDocument(WAREHOUSE_WSDL_PATH));
        Element[] schemas = wsn.getFullSchemas();
        assertion.setSchema(XmlUtil.elementToXml(schemas[0]));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, ApplicationContexts.getTestApplicationContext());

        // try to validate a number of different soap messages
        String[] resources = {LISTREQ_PATH, BAD_LISTREQ_PATH, LISTRES_PATH};
        boolean[] expectedResults = {true, false, true};
        for (int i = 0; i < resources.length; i++) {
            AssertionStatus res = serverAssertion.checkRequest(getResAsContext(resources[i]));
            //System.out.println("DOCUMENT " + resources[i] +
            //                    (res == AssertionStatus.NONE ? " VALIDATES OK" : " DOES NOT VALIDATE"));
            if (expectedResults[i]) {
                assertTrue(res == AssertionStatus.NONE);
            } else {
                assertFalse(res == AssertionStatus.NONE);
            }
        }
    }

    public void testRpcLiteralValidations() throws Exception {
        String schema = "<schema targetNamespace=\"http://acpkg\"\n" +
                "    xmlns=\"http://www.w3.org/2001/XMLSchema\" xmlns:impl=\"http://acpkg\"\n" +
                "    xmlns:intf=\"http://acpkg\"\n" +
                "    xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"\n" +
                "    xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "     \n" +
                "    <element name=\"getTheTimeReturn\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    <element name=\"format\" type=\"xsd:int\"/>\n" +
                "    <element name=\"getTheFormattedDateReturn\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "</schema>";
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "<soapenv:Body>\n" +
                "        <blah:getTheFormattedDate xmlns:blah=\"http://acpkg\">\n" +
                "                <blah:format>1</blah:format>\n" +
                "        </blah:getTheFormattedDate>\n" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
        SchemaValidation assertion = new SchemaValidation();
        assertion.setApplyToArguments(true);
        assertion.setSchema(schema);
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, ApplicationContexts.getTestApplicationContext());
        AssertionStatus res = serverAssertion.validateDocument(XmlUtil.stringToDocument(request));
        assertTrue(res == AssertionStatus.NONE);
    }

    /* emil, i commented this as it seems to generate messages that are not relevent. see other impl instead
    public void testRpcLiteralValidations() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        WsdlSchemaAnalizer wsn = new WsdlSchemaAnalizer(TestDocuments.getTestDocument(TestDocuments.WSDL_RPC_LITERAL));
        assertion.setApplyToArguments(true);
        Element[] schemas = wsn.getFullSchemas();
        System.out.println("SCHEMA:\n" + XmlUtil.elementToXml(schemas[0]));
        assertion.setSchema(XmlUtil.elementToXml(schemas[0]));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, ApplicationContexts.getTestApplicationContext());

        SoapMessageGenerator sgm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                return "aaa";
            }
        });
        SoapMessageGenerator.Message[] requests = sgm.generateRequests(TestDocuments.WSDL_RPC_LITERAL);

        // try to validate a number of different soap messages
        boolean[] expectedResults = {true, false, true};
        for (int i = 0; i < requests.length; i++) {
            final SOAPMessage soapMessage = requests[i].getSOAPMessage();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // test
            soapMessage.writeTo(bos);
            System.out.println("REQUEST:\n" + bos.toString());
            bos = new ByteArrayOutputStream();
            // test
            soapMessage.writeTo(bos);
            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            AssertionStatus res = serverAssertion.checkRequest(getResAsContext(bin));
            if (expectedResults[i]) {
                assertTrue(res == AssertionStatus.NONE);
            } else {
                assertFalse(res == AssertionStatus.NONE);
            }
        }
    }*/



    private PolicyEnforcementContext getResAsContext(String path) throws IOException, NoSuchPartException {
        return new PolicyEnforcementContext(
                new Message(StashManagerFactory.createStashManager(),
                            ContentTypeHeader.XML_DEFAULT,
                            TestDocuments.getInputStream(path)),
                new Message());
    }

    /*private PolicyEnforcementContext getResAsContext(InputStream msgInputStream) throws IOException, NoSuchPartException {
        return new PolicyEnforcementContext(
                new Message(StashManagerFactory.createStashManager(),
                            ContentTypeHeader.XML_DEFAULT,
                            msgInputStream),
                new Message());
    }*/
    private static final String RESOURCE_PATH = "com/l7tech/server/policy/assertion/xml/";
    private static final String WAREHOUSE_WSDL_PATH = RESOURCE_PATH + "warehouse.wsdl";
    private static final String LISTREQ_PATH = RESOURCE_PATH + "listProductsRequest.xml";
    private static final String BAD_LISTREQ_PATH = RESOURCE_PATH + "listProductsRequestIncorrect.xml";
    private static final String LISTRES_PATH = RESOURCE_PATH + "listProductResponse.xml";
    private static final String DOCLIT_WSDL_WITH2BODYCHILDREN = RESOURCE_PATH + "axisDocumentLiteralWith2BodyParts.wsdl";
    private static final String DOCLIT_WITH2BODYCHILDREN_REQ = RESOURCE_PATH + "requestWith2BodyChildren.xml";

    //private static final String ECHO_XSD = RESOURCE_PATH + "echo.xsd";
    //private static final String ECHO2_XSD = RESOURCE_PATH + "echo2.xsd";
    private static final String ECHO3_XSD = RESOURCE_PATH + "echo3.xsd";
    private static final String ECHO_REQ = RESOURCE_PATH + "echoReq.xml";

}
