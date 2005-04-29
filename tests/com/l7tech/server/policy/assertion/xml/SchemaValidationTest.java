package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.audit.BootMessages;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.StashManagerFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

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
        InputStream is = getClass().getResourceAsStream(ECHO3_XSD);
        String xsd = new String(HexUtils.slurpStream(is, 10000));
        assertion.setSchema(xsd);
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion, ApplicationContexts.getTestApplicationContext());
        AssertionStatus res = serverAssertion.checkRequest(getResAsContext(ECHO_REQ));
        System.out.println("result is " + res);
    }

    public void testCaseWith2BodyChildren() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        assertion.assignSchemaFromWsdl(getResAsDoc(DOCLIT_WSDL_WITH2BODYCHILDREN));
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
        assertion.assignSchemaFromWsdl(getResAsDoc(WAREHOUSE_WSDL_PATH));
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


    private InputStream getRes(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("\ncannot load resource " + path + ".\ncheck your runtime properties.\n");
        }
        return is;
    }

    private Document getResAsDoc(String path)
            throws IOException, SAXException, IllegalArgumentException
    {
        return XmlUtil.parse(getRes(path));
    }

    private PolicyEnforcementContext getResAsContext(String path) throws IOException, NoSuchPartException {
        return new PolicyEnforcementContext(
                new Message(StashManagerFactory.createStashManager(),
                        ContentTypeHeader.XML_DEFAULT,
                        getRes(path)),
                new Message());
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
	private static final String WAREHOUSE_WSDL_PATH = RESOURCE_PATH + "warehouse.wsdl";
    private static final String LISTREQ_PATH = RESOURCE_PATH + "listProductsRequest.xml";
    private static final String BAD_LISTREQ_PATH = RESOURCE_PATH + "listProductsRequestIncorrect.xml";
    private static final String LISTRES_PATH = RESOURCE_PATH + "listProductResponse.xml";
    private static final String DOCLIT_WSDL_WITH2BODYCHILDREN = RESOURCE_PATH + "axisDocumentLiteralWith2BodyParts.wsdl";
    private static final String DOCLIT_WITH2BODYCHILDREN_REQ = RESOURCE_PATH + "requestWith2BodyChildren.xml";

    private static final String ECHO_XSD = RESOURCE_PATH + "echo.xsd";
    private static final String ECHO2_XSD = RESOURCE_PATH + "echo2.xsd";
    private static final String ECHO3_XSD = RESOURCE_PATH + "echo3.xsd";
    private static final String ECHO_REQ = RESOURCE_PATH + "echoReq.xml";

    private final Logger logger = Logger.getLogger(getClass().getName());

}
