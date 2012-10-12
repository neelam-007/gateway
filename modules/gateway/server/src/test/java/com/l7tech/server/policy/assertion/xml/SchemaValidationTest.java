package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.Message;
import com.l7tech.message.ValidationTarget;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.communityschemas.SchemaHandle;
import com.l7tech.server.communityschemas.SchemaManager;
import com.l7tech.server.communityschemas.SchemaValidationErrorHandler;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.WsdlSchemaAnalizer;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for schema validation code.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 3, 2004<br/>
 *
 */
public class SchemaValidationTest {
    private static final Logger logger = Logger.getLogger(SchemaValidationTest.class.getName());
    private final String REUTERS_SCHEMA_URL = "http://locutus/reuters/schemas1/ReutersResearchAPI.xsd";
    private final String REUTERS_REQUEST_URL = "http://locutus/reuters/request1.xml";
    private ApplicationContext testApplicationContext;

    @Before
    public void setUp() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.common.xml.tarari.enable", "true" );
        GlobalTarariContextImpl context = (GlobalTarariContextImpl) TarariLoader.getGlobalContext();
        if (context != null) {
            context.compileAllXpaths();
        }
        testApplicationContext = ApplicationContexts.getTestApplicationContext();
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.common.xml.tarari.enable"
        );
    }

    @Test
    public void testEcho() throws Exception {
        SchemaValidation assertion = new SchemaValidation();
        InputStream is = TestDocuments.getInputStream(ECHO3_XSD);
        String xsd = new String( IOUtils.slurpStream(is, 10000));
        assertion.setResourceInfo(new StaticResourceInfo(xsd));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, testApplicationContext);
        AssertionStatus res = serverAssertion.checkRequest(getResAsContext(ECHO_REQ));
        Assert.assertTrue(res == AssertionStatus.NONE);
    }

    @Test
    public void testCaseWith2BodyChildren() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        WsdlSchemaAnalizer wsn = new WsdlSchemaAnalizer(TestDocuments.getTestDocument(DOCLIT_WSDL_WITH2BODYCHILDREN), null);
        Element[] schemas = wsn.getFullSchemas();
        Assert.assertTrue(schemas.length == 1); // no multiple schema support
        assertion.setResourceInfo(new StaticResourceInfo( XmlUtil.elementToXml(schemas[0])));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, testApplicationContext);

        // try to validate a number of different soap messages
        String[] resources = {DOCLIT_WITH2BODYCHILDREN_REQ};
        boolean[] expectedResults = {true};
        for (int i = 0; i < resources.length; i++) {
            AssertionStatus res = serverAssertion.checkRequest(getResAsContext(resources[i]));
            //System.out.println("DOCUMENT " + resources[i] +
            //                    (res == AssertionStatus.NONE ? " VALIDATES OK" : " DOES NOT VALIDATE"));
            if (expectedResults[i]) {
                Assert.assertTrue(res == AssertionStatus.NONE);
            } else {
                Assert.assertFalse(res == AssertionStatus.NONE);
            }
        }
    }

    @Test
    public void testWarehouseValidations() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        WsdlSchemaAnalizer wsn = new WsdlSchemaAnalizer(TestDocuments.getTestDocument(WAREHOUSE_WSDL_PATH), null);
        Element[] schemas = wsn.getFullSchemas();
        assertion.setResourceInfo(new StaticResourceInfo(XmlUtil.elementToXml(schemas[0])));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, testApplicationContext);

        // try to validate a number of different soap messages
        String[] resources = {BAD_LISTREQ_PATH, LISTREQ_PATH, LISTRES_PATH};
        boolean[] expectedResults = {false, true, true};
        for (int i = 0; i < resources.length; i++) {
            AssertionStatus res = serverAssertion.checkRequest(getResAsContext(resources[i]));
            System.out.println("DOCUMENT " + resources[i] +
                                (res == AssertionStatus.NONE ? " VALIDATES OK" : " DOES NOT VALIDATE"));
            if (expectedResults[i]) {
                Assert.assertTrue(res == AssertionStatus.NONE);
            } else {
                Assert.assertFalse(res == AssertionStatus.NONE);
            }
        }
    }

    @Test
    @Ignore("Tarari only test")
    public void testTarariSchemaImportsWithInlineNamespaceDecl() throws Exception {
        SchemaResolver resolver = new SchemaResolver() {
            @Override
            public byte[] resolveSchema(String string, String string1, String string2) {
                throw new RuntimeException("Screw you!");
            }
        };

        SchemaLoader.setSchemaResolver(resolver);
        SchemaLoader.unloadAllSchemas();

        InputStream is = new URL(REUTERS_SCHEMA_URL).openStream();
        String schemaDoc = new String(IOUtils.slurpStream(is));
        SchemaLoader.loadSchema(schemaDoc);
    }

    @Test
    @Ignore("Developer only test")
    public void testReutersUrlSchemaHttpObjectCache() throws Exception {
        SchemaManager sm = (SchemaManager)testApplicationContext.getBean("schemaManager");
        SchemaHandle handle = sm.getSchemaByUri(new LoggingAudit(logger), REUTERS_SCHEMA_URL);
        Document requestDoc = XmlUtil.parse(new URL(REUTERS_REQUEST_URL).openStream());
        Message request = new Message(requestDoc);

        handle.validateMessage(request, ValidationTarget.BODY, new SchemaValidationErrorHandler());
    }

    @Test
    @Ignore("Developer only test")
    public void testReutersUrlSchemaJaxp() throws Exception {
        SchemaFactory sfac = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        sfac.setResourceResolver(new LSResourceResolver() {
            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                logger.info(type + ", " + namespaceURI + ", " + publicId + ", " + systemId + ", " + baseURI);
                return null;
            }
        });

        Schema s = sfac.newSchema(getSchemaSource());
        Validator val = s.newValidator();
        val.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                logger.log(Level.INFO, ExceptionUtils.getMessage(exception), exception);
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(exception), exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                logger.log(Level.SEVERE, ExceptionUtils.getMessage(exception), exception);
            }
        });

        URL request = new URL(REUTERS_REQUEST_URL);
        val.validate(getMessageSource(request));
    }

    private StreamSource getMessageSource(URL request) throws IOException {
        return new StreamSource(request.openStream(), REUTERS_REQUEST_URL);
    }

    private StreamSource getSchemaSource() throws IOException {
        return new StreamSource(new URL(REUTERS_SCHEMA_URL).openStream(), REUTERS_SCHEMA_URL);
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
        assertion.setResourceInfo(new StaticResourceInfo(schema));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, testApplicationContext);
        Message requestMsg = new Message(XmlUtil.stringToDocument(request));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, new Message());
        AssertionStatus res = serverAssertion.validateMessage(requestMsg, context);
        Assert.assertTrue(res == AssertionStatus.NONE);
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
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                new Message((TestStashManagerFactory.getInstance().createStashManager()),
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
    private static final String WAREHOUSE_WSDL_PATH = "com/l7tech/policy/resources/warehouse.wsdl";
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
