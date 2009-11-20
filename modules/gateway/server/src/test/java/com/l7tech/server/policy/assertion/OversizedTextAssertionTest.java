/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.Assertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * Test for OversizedTextAssertion bean.
 */
public class OversizedTextAssertionTest extends TestCase {
    private static Logger log = Logger.getLogger(OversizedTextAssertionTest.class.getName());
    private ServerPolicyFactory serverPolicyFactory;

    public OversizedTextAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(OversizedTextAssertionTest.class);
    }

    protected void setUp() throws Exception {
        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testNestingLimitXpath() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setLimitNestingDepth(true);
        ota.setMaxNestingDepth(3);
        assertEquals("/*/*/*/*", ota.makeNestingXpath());
    }

    // Creates a PEC holding the specified request document.
    private PolicyEnforcementContext makeContext(String doc) throws Exception {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message( XmlUtil.stringToDocument(doc)), new Message());
    }

    // Passes the specified request to the specified asseriton and returns the result.
    private AssertionStatus checkRequest( Assertion assertion, String document) throws Exception {
        ServerAssertion ass = getServerPolicyFactory().compilePolicy(assertion, false);
        return ass.checkRequest(makeContext(document));
    }

    private ServerPolicyFactory getServerPolicyFactory() {
        if (serverPolicyFactory == null) {
            MockServletApi servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/server/resources/testApplicationContext.xml");
            serverPolicyFactory = (ServerPolicyFactory)servletApi.getApplicationContext().getBean("policyFactory");
        }
        return serverPolicyFactory;
    }

    public void testOtaDefaults() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC));
    }

    public void testOtaLongTextNode() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setLimitTextChars(true);
        ota.setMaxTextChars(15);
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC));
        ota.setMaxTextChars(8);
        assertEquals(AssertionStatus.BAD_REQUEST, checkRequest(ota.getCopy(), TEST_DOC));
    }

    public void testOtaLongAttrNode() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setLimitAttrChars(true);
        ota.setMaxAttrChars(45);
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC));
        ota.setMaxAttrChars(41);
        assertEquals(AssertionStatus.BAD_REQUEST, checkRequest(ota.getCopy(), TEST_DOC));
    }

    public void testOtaLongAttrNameNode() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setLimitAttrNameChars(true);
        ota.setMaxAttrNameChars(40);
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC_LONG_ATTR_NAME));
        ota.setMaxAttrNameChars(20);
        assertEquals(AssertionStatus.BAD_REQUEST, checkRequest(ota.getCopy(), TEST_DOC_LONG_ATTR_NAME));
    }

    public void testOtaNesting() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setLimitNestingDepth(true);
        ota.setMaxNestingDepth(5);
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC));
        ota.setMaxNestingDepth(3);
        assertEquals(AssertionStatus.BAD_REQUEST, checkRequest(ota.getCopy(), TEST_DOC));
    }

    public void testOtaMaxPayloads() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setMaxPayloadElements(3);
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC_THREE_PAYLOADS));
        ota.setMaxNestingDepth(2);
        assertEquals(AssertionStatus.BAD_REQUEST, checkRequest(ota.getCopy(), TEST_DOC_THREE_PAYLOADS));
    }

    public void testOtaSoapEnv() throws Exception {
        OversizedTextAssertion ota = new OversizedTextAssertion();
        ota.setRequireValidSoapEnvelope(false);
        assertEquals(AssertionStatus.NONE, checkRequest(ota.getCopy(), TEST_DOC_EXTRA_BODY));
        ota.setRequireValidSoapEnvelope(true);
        assertEquals(AssertionStatus.BAD_REQUEST, checkRequest(ota.getCopy(), TEST_DOC_EXTRA_BODY));
    }

    private static final String TEST_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instancex\">\n" +
            "    <soapenv:Body>\n" +
            "        <getOffensiveStats xmlns=\"http://playerstatsws.test.l7tech.com\">\n" +
            "            <playerNumber>19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String TEST_DOC_LONG_ATTR_NAME =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instancex\">\n" +
            "    <soapenv:Body>\n" +
            "        <getOffensiveStats xmlns=\"http://playerstatsws.test.l7tech.com\">\n" +
            "            <playerNumber myveryextremelylongattributename=\"blah\">19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String TEST_DOC_EXTRA_BODY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instancex\">\n" +
            "    <soapenv:Body>\n" +
            "        <getOffensiveStats2 xmlns=\"http://playerstatsws2.test.l7tech.com\">\n" +
            "            <playerNumber>19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats2>\n" +
            "    </soapenv:Body>\n" +
            "    <soapenv:Body>\n" +
            "        <getOffensiveStats xmlns=\"http://playerstatsws.test.l7tech.com\">\n" +
            "            <playerNumber>19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String TEST_DOC_THREE_PAYLOADS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instancex\">\n" +
            "    <soapenv:Body>\n" +
            "        <getOffensiveStats xmlns=\"http://playerstatsws.test.l7tech.com\">\n" +
            "            <playerNumber>19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats>\n" +
            "        <getOffensiveStats xmlns=\"http://playerstatsws.test.l7tech.com\">\n" +
            "            <playerNumber>19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats>\n" +
            "        <getOffensiveStats xmlns=\"http://playerstatsws.test.l7tech.com\">\n" +
            "            <playerNumber>19</playerNumber>\n" +
            "            <delay>0</delay>\n" +
            "        </getOffensiveStats>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";
}
