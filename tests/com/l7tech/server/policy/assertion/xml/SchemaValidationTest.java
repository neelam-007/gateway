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

/**
 * Tests for schema validation code.
 *
 * Todo: turn this class into a TestCase
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 3, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationTest {
    public static void main(String[] args) throws Exception {
        SchemaValidationTest me = new SchemaValidationTest();
        SchemaValidation assertion = new SchemaValidation();
        assertion.assignSchemaFromWsdl(me.getResAsDoc(WAREHOUSE_WSDL_PATH));
        ServerSchemaValidation serverAssertion = new ServerSchemaValidation(assertion);
        AssertionStatus res = serverAssertion.checkRequest(me.getResAsDoc(LISTREQ_PATH));
        System.out.println("validation of " + LISTREQ_PATH +
                            (res == AssertionStatus.NONE ? " succeeded" : " failed"));
        res = serverAssertion.checkRequest(me.getResAsDoc(BAD_LISTREQ_PATH));
        System.out.println("validation of " + BAD_LISTREQ_PATH +
                            (res == AssertionStatus.NONE ? " succeeded" : " failed"));
        res = serverAssertion.checkRequest(me.getResAsDoc(LISTRES_PATH));
        System.out.println("validation of " + LISTRES_PATH +
                            (res == AssertionStatus.NONE ? " succeeded" : " failed"));
    }


    private InputSource getRes(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IOException("cannot load resource " + path + ". file not there or path not set");
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
}
