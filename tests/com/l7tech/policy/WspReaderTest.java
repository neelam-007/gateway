package com.l7tech.policy;

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

    public void testParseWsp() throws IOException {
        InputStream wspStream = cl.getResourceAsStream(SIMPLE_POLICY);
        Assertion policy = WspReader.parse(wspStream);
        log.info("Got back policy: " + policy);
        assertTrue(policy != null);
        assertTrue(policy instanceof ExactlyOneAssertion);
        ExactlyOneAssertion eoa = (ExactlyOneAssertion)policy;
        assertTrue(eoa.getChildren().size() == 4);
        assertTrue(eoa.getChildren().get(0) instanceof AllAssertion);

        // Do a round trip policyA -> xmlA -> policyB -> xmlB and verify that both XMLs match
        String xmlA = WspWriter.getPolicyXml(policy);
        Assertion policyB = WspReader.parse(xmlA);
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
                Assertion policy = WspReader.parse("asdfhaodh/asdfu2h$9ha98h");
            }
        });
    }

    public void testParseStrangeXml() {
        mustThrow(IOException.class,  new throwingRunnable() {
            public void run() throws IOException {
                Assertion policy = WspReader.parse("<foo><bar blee=\"1\"/></foo>");
            }
        });
    }

    public void testParseSwAPolicy() throws Exception {
        Assertion policy = WspWriterTest.createSoapWithAttachmentsPolicy();
        String serialized = WspWriter.getPolicyXml(policy);
        Assertion parsedPolicy = WspReader.parse(serialized);
        assertTrue(parsedPolicy instanceof AllAssertion);
        AllAssertion all = (AllAssertion)parsedPolicy;
        Assertion kid = (Assertion)all.getChildren().get(0);
        assertTrue(kid instanceof RequestSwAAssertion);
        RequestSwAAssertion swa = (RequestSwAAssertion)kid;
        assertNotNull(swa.getBindingInfo());
        BindingInfo bindingInfo = swa.getBindingInfo();
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

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
