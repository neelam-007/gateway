package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ExactlyOneAssertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.wsp.WspReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.InputStream;
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
        WspReader wspReader = new WspReader();
        InputStream wspStream = cl.getResourceAsStream(SIMPLE_POLICY);
        Assertion policy = wspReader.parse(wspStream);
        assertTrue(policy != null);
        assertTrue(policy instanceof ExactlyOneAssertion);
        ExactlyOneAssertion eoa = (ExactlyOneAssertion)policy;
        assertTrue(eoa.getChildren().size() == 2);
        assertTrue(eoa.getChildren().get(0) instanceof TrueAssertion);
        assertTrue(eoa.getChildren().get(1) instanceof FalseAssertion);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
