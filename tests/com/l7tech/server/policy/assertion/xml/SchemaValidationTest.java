package com.l7tech.server.policy.assertion.xml;

import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.AssertionStatus;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

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

    public void testCaseWith2BodyChildren() throws Exception {
        // create assertion based on the wsdl
        SchemaValidation assertion = new SchemaValidation();
        assertion.assignSchemaFromWsdl(getResAsDoc(DOCLIT_WSDL_WITH2BODYCHILDREN));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion);

        // try to validate a number of different soap messages
        String[] resources = {DOCLIT_WITH2BODYCHILDREN_REQ};
        boolean[] expectedResults = {true};
        for (int i = 0; i < resources.length; i++) {
            AssertionStatus res = serverAssertion.checkRequest(getResAsDoc(resources[i]));
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
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion);

        // try to validate a number of different soap messages
        String[] resources = {LISTREQ_PATH, BAD_LISTREQ_PATH, LISTRES_PATH};
        boolean[] expectedResults = {true, false, true};
        for (int i = 0; i < resources.length; i++) {
            AssertionStatus res = serverAssertion.checkRequest(getResAsDoc(resources[i]));
            //System.out.println("DOCUMENT " + resources[i] +
            //                    (res == AssertionStatus.NONE ? " VALIDATES OK" : " DOES NOT VALIDATE"));
            if (expectedResults[i]) {
                assertTrue(res == AssertionStatus.NONE);
            } else {
                assertFalse(res == AssertionStatus.NONE);
            }
        }
    }


    private InputSource getRes(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("\ncannot load resource " + path + ".\ncheck your runtime properties.\n");
        }
        return new InputSource(is);
    }

    private Document getResAsDoc(String path) throws IOException, ParserConfigurationException,
                                                          SAXException, IllegalArgumentException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(getRes(path));
    }

    private static final String RESOURCE_PATH = "/com/l7tech/server/policy/assertion/xml/";
	private static final String WAREHOUSE_WSDL_PATH = RESOURCE_PATH + "warehouse.wsdl";
    private static final String LISTREQ_PATH = RESOURCE_PATH + "listProductsRequest.xml";
    private static final String BAD_LISTREQ_PATH = RESOURCE_PATH + "listProductsRequestIncorrect.xml";
    private static final String LISTRES_PATH = RESOURCE_PATH + "listProductResponse.xml";
    private static final String DOCLIT_WSDL_WITH2BODYCHILDREN = RESOURCE_PATH + "axisDocumentLiteralWith2BodyParts.wsdl";
    private static final String DOCLIT_WITH2BODYCHILDREN_REQ = RESOURCE_PATH + "requestWith2BodyChildren.xml";


}
