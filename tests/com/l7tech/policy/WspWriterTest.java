package com.l7tech.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.skunkworks.schemavalidation.Validator;
import com.l7tech.skunkworks.wsp.pre32.Pre32WspReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Test serializing policy tree to XML.
 */
public class WspWriterTest extends TestCase {
    private static Logger log = Logger.getLogger(WspWriterTest.class.getName());
    private static final ClassLoader cl = WspReaderTest.class.getClassLoader();
    public static final String RESOURCE_PATH = "com/l7tech/policy/resources";
    public static final String SIMPLE_POLICY = RESOURCE_PATH + "/simple_policy.xml";
    public static final String SCHEMA_PATH = RESOURCE_PATH + "/ws_policy_2002.xsd";
    public static final String UTILITY_SCHEMA = RESOURCE_PATH + "/ws_utility_2002.xsd";

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

    public void testWritePolicy() throws Exception {
        Assertion policy = makeTestPolicy();

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

        //log.info("Expected XML: " + knownStr);
        fos = new FileOutputStream("WspWriterTest_knownStr.xml");
        fos.write(knownStr.getBytes());
        fos.close();

        assertEquals(gotXml, knownStr);
        log.info("Output matched expected XML.");

        // Test validation with WS-Policy schema
        log.info("Validating output against WS-Policy 2002 schema...");
        InputStream schemaStream = cl.getResourceAsStream(SCHEMA_PATH);
        assertNotNull(schemaStream);
        Validator validator = new Validator();
        validator.validate(schemaStream, new ByteArrayInputStream(gotXml.getBytes()), new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId.endsWith("2002/07/utility"))
                    return new InputSource(getStream(UTILITY_SCHEMA));
                throw new RuntimeException("Unknown external resource:" + systemId);
            }
        });

        //Assertion tree = WspReader.parse(gotXml);
        //log.info("After parsing: " + tree);
    }

    private Assertion makeTestPolicy() {
        RequestXpathAssertion rxa = new RequestXpathAssertion();
        Map foo = new HashMap();
        foo.put("abc", "http://namespaces.somewhere.com/abc#bletch");
        foo.put("blee", "http://namespaces.nowhere.com/asdf/fdsa/qwer#blortch.1.2");
        rxa.setXpathExpression(new XpathExpression("//blee:blaz", foo));

        final CustomAssertionHolder custom = new CustomAssertionHolder();
        custom.setCategory(Category.ACCESS_CONTROL);
        custom.setCustomAssertion(new TestCustomAssertion(22, "foo bar baz", new HashMap()));
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
                createSoapWithAttachmentsPolicy(),
            })),
            new TrueAssertion(),
            new FalseAssertion(),
            custom
        }));
        return policy;
    }

    private InputStream getStream(String path) {
        final InputStream st = cl.getResourceAsStream(path);
        if (st == null) throw new RuntimeException("Missing resource: " + path);
        return st;
    }

    public static void testNestedMaps() throws Exception {
        Assertion policy = createSoapWithAttachmentsPolicy();
        log.info("Serialized complex policy: " + WspWriter.getPolicyXml(policy));
    }

    public void testUnknownAssertionPreservesOriginalElement() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "<FooAssertion/>");
        AllAssertion aa = new AllAssertion(Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            uk,
            new FalseAssertion(),
        }));
        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNodeName().equals("FooAssertion"));
        log.info("Serialized: " + got);
    }

    public void testUnknownAssertionPreservesOriginalElementIgnoringSurroundingText() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "asdf\n\nasdfq  <FooBarBazAssertion/>qgrqegr");
        AllAssertion aa = new AllAssertion(Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            uk,
            new FalseAssertion(),
        }));
        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNodeName().equals("FooBarBazAssertion"));
        log.info("Serialized: " + got);
    }

    public void testUnknownAssertionFallbackOnBadForm() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "foo>blah<>not<well<formed/>");
        AllAssertion aa = new AllAssertion(Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            uk,
            new FalseAssertion(),
        }));

        String got = WspWriter.getPolicyXml(aa);
        log.info("Got result: " + got);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getLocalName().equals("UnknownAssertion"));
        log.info("Serialized: " + got);
    }

    public void testUnknownAssertionFallbackOnMultipleElements() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "<foo/>blahblah<bar/>");
        AllAssertion aa = new AllAssertion(Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            uk,
            new FalseAssertion(),
        }));

        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getLocalName().equals("UnknownAssertion"));
        log.info("Serialized: " + got);
    }

    public void testUnknownAssertionFallbackOnNoElement() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "asdf asdhfkajsl laskfh");
        AllAssertion aa = new AllAssertion(Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            uk,
            new FalseAssertion(),
        }));

        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getLocalName().equals("UnknownAssertion"));
        log.info("Serialized: " + got);
    }

    public void testSslAssertionOptionChange() throws Exception {
        SslAssertion sa = new SslAssertion(SslAssertion.OPTIONAL);
        String got = WspWriter.getPolicyXml(sa);
        assertNotNull(got);
        assertTrue(got.contains("SslAssertion"));
        assertTrue(got.contains("Optional"));
    }

    public static Assertion createSoapWithAttachmentsPolicy() {
        Map getQuoteAttachments = new HashMap();
        getQuoteAttachments.put("portfolioData", new MimePartInfo("portfolioData", "application/x-zip-compressed"));
        //getQuoteAttachments.put("expectedQuoteFormat", new MimePartInfo("expectedQuoteFormat", "text/xml"));
        //getQuoteAttachments.put("quoteData", new MimePartInfo("quoteData", "application/x-zip-compressed"));

        Map buyStockAttachments = new HashMap();
        buyStockAttachments.put("portfolioData", new MimePartInfo("portfolioData", "application/x-zip-compressed"));
        //buyStockAttachments.put("paymentInformation", new MimePartInfo("paymentInformation", "application/x-payment-info"));

        Map bindingOperations = new HashMap();
        bindingOperations.put("getQuote", new BindingOperationInfo("getQuote", getQuoteAttachments));
        //bindingOperations.put("buyStock", new BindingOperationInfo("buyStock", buyStockAttachments));

        BindingInfo bindingInfo = new BindingInfo("serviceBinding1", bindingOperations);

        Map bindings = new HashMap();
        bindings.put(bindingInfo.getBindingName(), bindingInfo);
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[] {
            new RequestSwAAssertion(bindings),
        }));
        return policy;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Map checkTestCustomAssertion(CustomAssertion ca) throws Exception {
        if (ca == null)
            throw new NullPointerException("CustomAssertion is null");
        if (!(ca instanceof TestCustomAssertion))
            throw new ClassCastException("CustomAssertion isn't an instance of TestCustomAssertion; actual class is " + ca.getClass().getName());
        TestCustomAssertion tca = (TestCustomAssertion)ca;
        if (tca.getInt1() != 22)
            throw new IllegalArgumentException("TestCustomAssertion has invalid int1");
        if (!"foo bar baz".equals(tca.getString1()))
            throw new IllegalArgumentException("TestCustomAssertion has invalid string1");
        return tca.getMap1();
    }

    /** Verify that WspWriter can, when so directed, produce a policy comprehensible to a 3.1 WspReader. */
    public void testWspWriterCompatibilityMode() throws IOException {
        // Create our usual complex test policy
        Assertion policy = makeTestPolicy();

        // Serialize in compatibility mode
        WspWriter cww = new WspWriter();
        cww.setPre32Compat(true);
        cww.setPolicy(policy);
        String written = cww.getPolicyXmlAsString();

        log.info("Produced compatibility policy: " + written);

        // Feed it to the old parser
        Assertion out = Pre32WspReader.parsePermissively(written);
        log.info("Old policy reader returned the following: " + out);
        assertNotNull(out);
        assertEquals(policy.getClass(), out.getClass());
    }

    public void testDisappearingXslt() throws Exception {
        SchemaValidation sv = new SchemaValidation();
        sv.setResourceInfo(new StaticResourceInfo("<schema/>"));
        XslTransformation xsl1 = new XslTransformation();
        xsl1.setResourceInfo(new MessageUrlResourceInfo(new String[] { ".*" }));
        XslTransformation xsl2 = new XslTransformation();
        xsl2.setResourceInfo(new StaticResourceInfo("<static xsl/>"));
        HttpRoutingAssertion http = new HttpRoutingAssertion();
        AllAssertion all = new AllAssertion(Arrays.asList(new Assertion[] {
            sv,
            xsl2,
            xsl1,
            http,
        }));
        System.out.println(WspWriter.getPolicyXml(all));
    }

    public void testResourceInfo() throws Exception {
        tryIt(new XslTransformation(), makeStaticInfo());
        tryIt(new XslTransformation(), makeSingleInfo());
        tryIt(new XslTransformation(), makeMessageInfo());
        tryIt(new XslTransformation(), null);
    }

    private void tryIt(XslTransformation xsl, AssertionResourceInfo rinfo) {
        xsl.setResourceInfo(rinfo);
        String got = WspWriter.getPolicyXml(xsl);
        System.out.println(got);
        assertNotNull(got);
    }

    static SingleUrlResourceInfo makeSingleInfo() {
        SingleUrlResourceInfo suri = new SingleUrlResourceInfo();
        suri.setUrl("http://example.org/");
        return suri;
    }

    static MessageUrlResourceInfo makeMessageInfo() {
        MessageUrlResourceInfo muri = new MessageUrlResourceInfo();
        muri.setAllowMessagesWithoutUrl(true);
        muri.setUrlRegexes(new String[] { ".*", "^$"});
        return muri;
    }

    static StaticResourceInfo makeStaticInfo() {
        StaticResourceInfo sri = new StaticResourceInfo();
        sri.setDocument("<foo/>");
        sri.setOriginalUrl("http://example.org/foo");
        return sri;
    }
}

/** This class is for testing custom assertion serialization compatibility with 2.1.  Do not change this class! */
class TestCustomAssertion implements CustomAssertion {
    private int int1;
    private String String1;
    private Map map1;

    private static final long serialVersionUID = -6253600978668874984L;

    public TestCustomAssertion(int int1, String string1, Map map1) {
        this.int1 = int1;
        String1 = string1;
        this.map1 = map1;
    }

    public String getName() {
        return "Test Assertion";
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    public String getString1() {
        return String1;
    }

    public void setString1(String string1) {
        String1 = string1;
    }

    public Map getMap1() {
        return map1;
    }

    public void setMap1(Map map1) {
        this.map1 = map1;
    }
}
