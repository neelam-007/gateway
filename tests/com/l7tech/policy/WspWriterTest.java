package com.l7tech.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.common.xml.XpathExpression;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    private String fixLines(String input) {
        return input.replaceAll("\\r\\n", "\n").replaceAll("\\n\\r", "\n").replaceAll("\\r", "\n");
    }

    public void testWriteNullPolicy() {
        Assertion policy = null;
        String xml = WspWriter.getPolicyXml(policy);
        log.info("Null policy serialized to this XML:\n---<snip>---\n" + xml + "---<snip>---\n");
    }

    public void testWritePolicy() throws IOException {
        RequestXpathAssertion rxa = new RequestXpathAssertion();
        Map foo = new HashMap();
        foo.put("abc", "http://namespaces.somewhere.com/abc#bletch");
        foo.put("blee", "http://namespaces.nowhere.com/asdf/fdsa/qwer#blortch.1.2");
        rxa.setXpathExpression(new XpathExpression("//blee:blaz", foo));

        Assertion policy = new ExactlyOneAssertion(Arrays.asList(new Assertion[]{
            new AllAssertion(Arrays.asList(new Assertion[]{
                new TrueAssertion(),
                new OneOrMoreAssertion(Arrays.asList(AllAssertions.SERIALIZABLE_EVERYTHING)),
            })),
            new ExactlyOneAssertion(Arrays.asList(new Assertion[]{
                new TrueAssertion(),
                new FalseAssertion(),
                new HttpRoutingAssertion("http://floomp.boomp.foomp/", "bob&joe", "james;bloo=foo&goo\"poo\"\\sss\\", "", -5),
                rxa,
            })),
            new TrueAssertion(),
            new FalseAssertion()
        }));

        log.info("Created policy tree: " + policy);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WspWriter.writePolicy(policy, out);
        String gotXml = fixLines(out.toString());

        log.info("Encoded to XML: " + gotXml);

        //uncomment to save the XML to a file
        FileOutputStream fos = new FileOutputStream("WspWriterTest_gotXml.xml");
        fos.write(gotXml.getBytes());
        fos.close();

        // See what it should look like
        InputStream knownStream = cl.getResourceAsStream(SIMPLE_POLICY);
        byte[] known = new byte[65536];
        int len = knownStream.read(known);
        String knownStr = fixLines(new String(known, 0, len));

        log.info("Expected XML: " + knownStr);
        fos = new FileOutputStream("WspWriterTest_knownStr.xml");
        fos.write(knownStr.getBytes());
        fos.close();

        assertEquals(gotXml, knownStr);
        log.info("Output matched expected XML.");

        //Assertion tree = WspReader.parse(gotXml);
        //log.info("After parsing: " + tree);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
