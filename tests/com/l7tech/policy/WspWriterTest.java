package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ExactlyOneAssertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.wsp.WspWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Test serializing policy tree to XML.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:33:11 PM
 */
public class WspWriterTest extends TestCase {
    private static Logger log = Logger.getLogger(WspWriterTest.class.getName());
    private static final ClassLoader cl = WspReaderTest.class.getClassLoader();
    private static String RESOURCE_PATH = "com/l7tech/policy/resources";
    private static String SIMPLE_POLICY = RESOURCE_PATH + "/simple_policy.xml";

    public WspWriterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspWriterTest.class);
    }

    public void testWritePolicy() throws IOException {
        List kids = new ArrayList(Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            new FalseAssertion()
        }));
        Assertion policy = new ExactlyOneAssertion(kids);

        log.info("Created policy tree: " + policy);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WspWriter.writePolicy(policy, out);

        log.info("Encoded to XML: " + out.toString());

        // See what it should look like
        InputStream knownStream = cl.getResourceAsStream(SIMPLE_POLICY);
        byte[] known = new byte[16384];
        int len = knownStream.read(known);
        String knownStr = new String(known, 0, len);

        log.info("Expected XML: " + knownStr);

        assertTrue(out.toString().equals(knownStr));

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
