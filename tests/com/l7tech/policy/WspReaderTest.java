package com.l7tech.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Test policy deserializer.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:33:36 PM
 */
public class WspReaderTest extends TestCase {
    private static Logger log = Logger.getLogger(WspReaderTest.class.getName());
    private static final ClassLoader cl = WspReaderTest.class.getClassLoader();
    private static String RESOURCE_PATH = "com/l7tech/policy/resources";
    private static String SIMPLE_POLICY = RESOURCE_PATH + "/simple_policy.xml";

    public WspReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspReaderTest.class);
    }

    public void testParseWsp() throws Exception {
        InputStream wspStream = cl.getResourceAsStream(SIMPLE_POLICY);
        Assertion policy = WspReader.parsePermissively(XmlUtil.parse(wspStream).getDocumentElement());
        log.info("Got back policy: " + policy);
        assertTrue(policy != null);
        assertTrue(policy instanceof ExactlyOneAssertion);
        ExactlyOneAssertion eoa = (ExactlyOneAssertion)policy;
        assertTrue(eoa.getChildren().size() == 5);
        assertTrue(eoa.getChildren().get(0) instanceof AllAssertion);

        // Do a round trip policyA -> xmlA -> policyB -> xmlB and verify that both XMLs match
        String xmlA = WspWriter.getPolicyXml(policy);
        Assertion policyB = WspReader.parseStrictly(xmlA);
        String xmlB = WspWriter.getPolicyXml(policyB);
        assertEquals(xmlA, xmlB);
    }

    private interface throwingRunnable {
        void run() throws Throwable;
    }

    private void mustThrow(Class mustThrowThis, throwingRunnable tr) {
        boolean caught = false;
        try {
            System.err.println(">>>>>> The following operation should throw the exception " + mustThrowThis);
            tr.run();
        } catch (Throwable t) {
            caught = true;
            if (!mustThrowThis.isAssignableFrom(t.getClass()))
                t.printStackTrace();
            assertTrue(mustThrowThis.isAssignableFrom(t.getClass()));
        }
        assertTrue(caught);
        System.err.println(">>>>>> The correct exception was thrown.");
    }

    public void testParseNonXml() {
        mustThrow(IOException.class, new throwingRunnable() {
            public void run() throws IOException {
                WspReader.parseStrictly("asdfhaodh/asdfu2h$9ha98h");
            }
        });
    }

    public void testParseStrangeXml() {
        mustThrow(IOException.class,  new throwingRunnable() {
            public void run() throws IOException {
                WspReader.parseStrictly("<foo><bar blee=\"1\"/></foo>");
            }
        });
    }

    public void testParseSwAPolicy() throws Exception {
        Assertion policy = WspWriterTest.createSoapWithAttachmentsPolicy();
        String serialized = WspWriter.getPolicyXml(policy);
        Assertion parsedPolicy = WspReader.parseStrictly(serialized);
        assertTrue(parsedPolicy instanceof AllAssertion);
        AllAssertion all = (AllAssertion)parsedPolicy;
        Assertion kid = (Assertion)all.getChildren().get(0);
        assertTrue(kid instanceof RequestSwAAssertion);
        RequestSwAAssertion swa = (RequestSwAAssertion)kid;

        assertTrue(swa.getBindings().size() == 1);
        String bindingInfoName = (String) swa.getBindings().keySet().iterator().next();
        BindingInfo bindingInfo = (BindingInfo) swa.getBindings().get(bindingInfoName);
        assertNotNull(bindingInfo);

        assertNotNull(bindingInfo.getBindingName());
        assertTrue(bindingInfo.getBindingName().length() > 0);
        assertEquals(bindingInfo.getBindingName(), "serviceBinding1");

        Map bops = bindingInfo.getBindingOperations();
        assertFalse(bops.isEmpty());
        BindingOperationInfo[] bois = (BindingOperationInfo[])bops.values().toArray(new BindingOperationInfo[0]);
        assertTrue(bois.length == 1);

        String reserialized = WspWriter.getPolicyXml(parsedPolicy);
        assertEquals(reserialized.length(), serialized.length());

    }

    private static final Object[][] VERSIONS = {
        new Object[] { "simple_policy_21.xml", "2.1" },
        new Object[] { "simple_policy_30.xml", "3.0" },
        new Object[] { "simple_policy_31.xml", "3.1" },
    };

    private void trySeamlessPolicyUpgrade(String policyFile) throws Exception {
        InputStream policyStream = null;
        try {
            log.info("Trying to parse policy document; " + policyFile);
            policyStream = cl.getResourceAsStream(RESOURCE_PATH + "/" + policyFile);
            Document policy = XmlUtil.parse(policyStream);
            Assertion root = WspReader.parsePermissively(policy.getDocumentElement());
            assertTrue(root != null);
            assertTrue(root instanceof ExactlyOneAssertion);
        } finally {
            if (policyStream != null) policyStream.close();
        }
    }

    public void testSeamlessPolicyUpgrades() throws Exception {
        for (int i = 0; i < VERSIONS.length; i++) {
            Object[] version = VERSIONS[i];
            String policyFile = (String)version[0];
            trySeamlessPolicyUpgrade(policyFile);
        }
    }

    public void testSeamlessUpgradeFrom21() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_21.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 2.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom30() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_30.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.0 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom31() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_31.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testUnknownElementGetsPreserved() throws Exception {


        String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/200" +
                "2/12/policy\">\n" +
                "    <All>\n" +
                "        <HttpRoutingAssertion>\n" +
                "            <ProtectedServiceUrl stringValue=\"http://hugh/ACMEWarehouseWS/Service1.asmx\"/>\n" +
                "        </HttpRoutingAssertion>\n" +
                "        <EmailAlert>\n" +
                "            <Message stringValue=\"Woot!  Hola!\n" +
                "\n" +
                "Blah blah blah!\n" +
                "\"/>\n" +
                "            <TargetEmailAddress stringValue=\"mlyons@layer7-tech.com\"/>\n" +
                "            <SourceEmailAddress stringValue=\"mlyons-4@layer7-tech.com\"/>\n" +
                "            <Subject stringValue=\"ALERT ALERT from EmailAlertAssertion asdfhasdhf\"/>\n" +
                "        </EmailAlert>\n" +
                "    </All>\n" +
                "</wsp:Policy>\n";

        Assertion p = WspReader.parsePermissively(policyXml);
        String parsed1 = p.toString();
        log.info("Parsed data including unknown element: " + parsed1);

        String out = WspWriter.getPolicyXml(p);
        Assertion p2 = WspReader.parsePermissively(out);
        String parsed2 = p2.toString();
        log.info("After reparsing: " + parsed2);

        assertEquals(parsed1, parsed2);

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
