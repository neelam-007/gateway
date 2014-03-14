package com.l7tech.policy;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.util.LSInputImpl;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.BindingInfo;
import com.l7tech.wsdl.BindingOperationInfo;
import com.l7tech.wsdl.MimePartInfo;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.AfterClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test serializing policy tree to XML.
 */
public class WspWriterTest {
    private static Logger log = Logger.getLogger(WspWriterTest.class.getName());
    private static final ClassLoader cl = WspReaderTest.class.getClassLoader();
    public static final String RESOURCE_PATH = "com/l7tech/policy/resources";
    public static final String SIMPLE_POLICY = RESOURCE_PATH + "/simple_policy.xml";
    public static final String SCHEMA_PATH = RESOURCE_PATH + "/ws_policy_2002.xsd";
    public static final String UTILITY_SCHEMA = RESOURCE_PATH + "/ws_utility_2002.xsd";
    public static final String ALL_ENABLED_ASSERTIONS_POLICY = RESOURCE_PATH + "/all_enabled_assertions_policy.xml";
    public static final String ALL_DISABLED_ASSERTIONS_POLICY = RESOURCE_PATH + "/all_disabled_assertions_policy.xml";
    
    static {
        SyspropUtil.setProperty( "com.l7tech.policy.wsp.checkAccessors", "true" );
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.policy.wsp.checkAccessors"
        );
    }

    private String fixLines(String input) {
        return input.replaceAll("\\r\\n", "\n").replaceAll("\\n\\r", "\n").replaceAll("\\r", "\n");
    }

    @Test
    public void testWriteNullPolicy() {
        Assertion policy = null;
        String xml = WspWriter.getPolicyXml(policy);
        log.info("Null policy serialized to this XML:\n---<snip>---\n" + xml + "---<snip>---\n");
    }

    /**
     * @return  An one or more assertion that contains all assertions (all are enabled)
     */
    private Assertion makeAllEnabledAssertions() {
        return new OneOrMoreAssertion(Arrays.asList(AllAssertions.SERIALIZABLE_EVERYTHING));
    }

    /**
     * @return  A 'one or more' assertion that contains all assertions disabled
     */
    private Assertion makeAllDisabledAssertions() {
        List<Assertion> allAssertions = new ArrayList<Assertion>();
        for (Assertion assertion : AllAssertions.SERIALIZABLE_EVERYTHING) {
            final Assertion copy = (Assertion) assertion.clone();
            copy.setEnabled(false);
            allAssertions.add(copy);
        }

        return new OneOrMoreAssertion(allAssertions);
    }

    /**
     * Helper method to write the policy based on the assertion
     */
    private String writePolicy(Assertion assertion) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        WspWriter.writePolicy(assertion, outStream);
        return fixLines(outStream.toString());
    }

    /**
     * Helper method to read the XML file and output as a string
     */
    private String readPolicyFile(String file) throws IOException {
        InputStream inStream = cl.getResourceAsStream(file);
        byte[] bytes = new byte[65536];
        int len = inStream.read(bytes);
        return fixLines(new String(bytes, 0, len));
    }

    /**
     * Tests that all disabled assertions are written to XML correctly.
     *
     * Note: if this test fails due to the CustomAssertion assertion, it is most likely a change happened to either the class
     * CustomAssertionHolder or it's superclass Assertion. Any change in these classes which affects serilization will
     * cause a new base64 to be computed. The expected value comes from a static resource file which needs to be
     * updated manually if any change happens to the mentioned classes.
     *
     * @throws Exception
     */
    @Test
    public void testWritePolicyForAllDisabledAssertions() throws Exception {
        Assertion policy = makeAllDisabledAssertions();
        log.fine("The created policy with all disabled assertions:\n" + policy);

        String policyXml = writePolicy(policy);
        log.fine("Policy XML output:\n" + policyXml);

        assertEquals("Policy with disabled assertions", readPolicyFile(ALL_DISABLED_ASSERTIONS_POLICY), policyXml);
    }

    /**
     * Tests that all enabled assertions are written to XML correctly.
     *
     * Note: if this test fails due to the CustomAssertion assertion, it is most likely a change happened to either the class
     * CustomAssertionHolder or it's superclass Assertion. Any change in these classes which affects serilization will
     * cause a new base64 to be computed. The expected value comes from a static resource file which needs to be
     * updated manually if any change happens to the mentioned classes.
     *
     * @throws Exception
     */
    @Test
    public void testWritePolicyForAllEnabledAssertions() throws Exception {
        Assertion policy = makeAllEnabledAssertions();
        log.fine("The created policy with all enabled assertions:\n" + policy);

        String policyXml = writePolicy(policy);
        log.fine("Policy XML output:\n" + policyXml);
        
        assertEquals("Policy with enabled assertions.", readPolicyFile(ALL_ENABLED_ASSERTIONS_POLICY), policyXml);
    }

    /**
     * Note: if this test fails due to the CustomAssertion assertion, it is most likely a change happened to either the class
     * CustomAssertionHolder or it's superclass Assertion. Any change in these classes which affects serilization will
     * cause a new base64 to be computed. The expected value comes from a static resource file which needs to be
     * updated manually if any change happens to the mentioned classes.
     *
     * @throws Exception
     */
    @Test
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

        //These are no longer equal as the jdk does not consistently serialize hashmap anymore
        //assertEquals("Policy with all assertions",knownStr, gotXml);
        log.info("Output matched expected XML.");

        // Test validation with WS-Policy schema
        log.info("Validating output against WS-Policy 2002 schema...");
        URL schemaUrl = cl.getResource(SCHEMA_PATH);
        assertNotNull(schemaUrl);

        SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        factory.setResourceResolver( new LSResourceResolver(){
            public LSInput resolveResource( String type, String namespaceURI, String publicId, String systemId, String baseURI ) {
                if (systemId.endsWith("2002/07/utility")) {
                    LSInputImpl input = new LSInputImpl();
                    input.setBaseURI( systemId );
                    input.setPublicId( publicId );
                    input.setSystemId( systemId );
                    input.setByteStream( getStream(UTILITY_SCHEMA) );
                    return input;
                }
                throw new RuntimeException("Unknown external resource:" + systemId);
            }
        } );
        Schema schema = factory.newSchema( schemaUrl );
        schema.newValidator().validate( new DOMSource(XmlUtil.stringAsDocument( gotXml )) );

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
        custom.setCategories(Category.ACCESS_CONTROL);
        custom.setCustomAssertion(new TestCustomAssertion(22, "foo bar baz", new HashMap()));
        Assertion policy = new ExactlyOneAssertion(Arrays.asList(
            new AllAssertion(Arrays.asList(
                new TrueAssertion(),
                new OneOrMoreAssertion(Arrays.asList(AllAssertions.SERIALIZABLE_EVERYTHING))
            )),
            new ExactlyOneAssertion(Arrays.asList(
                new TrueAssertion(),
                new FalseAssertion(),
                new HttpRoutingAssertion("http://floomp.boomp.foomp/", "bob&joe", "james;bloo=foo&goo\"poo\"\\sss\\", "", -5),
                rxa,
                createSoapWithAttachmentsPolicy()
            )),
            new TrueAssertion(),
            new FalseAssertion(),
            custom
        ));
        return policy;
    }

    private InputStream getStream(String path) {
        final InputStream st = cl.getResourceAsStream(path);
        if (st == null) throw new RuntimeException("Missing resource: " + path);
        return st;
    }

    @Test
    public void testNestedMaps() throws Exception {
        Assertion policy = createSoapWithAttachmentsPolicy();
        log.info("Serialized complex policy: " + WspWriter.getPolicyXml(policy));
    }

    @Test
    public void testUnknownAssertionPreservesOriginalElement() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "<FooAssertion/>");
        AllAssertion aa = new AllAssertion(Arrays.asList(
            new TrueAssertion(),
            uk,
            new FalseAssertion()
        ));
        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNodeName().equals("FooAssertion"));
        log.info("Serialized: " + got);
    }

    @Test
    public void testUnknownAssertionPreservesOriginalElementIgnoringSurroundingText() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "asdf\n\nasdfq  <FooBarBazAssertion/>qgrqegr");
        AllAssertion aa = new AllAssertion(Arrays.asList(
            new TrueAssertion(),
            uk,
            new FalseAssertion()
        ));
        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNodeName().equals("FooBarBazAssertion"));
        log.info("Serialized: " + got);
    }

    @Test
    public void testUnknownAssertionFallbackOnBadForm() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "foo>blah<>not<well<formed/>");
        AllAssertion aa = new AllAssertion(Arrays.asList(
            new TrueAssertion(),
            uk,
            new FalseAssertion()
        ));

        String got = WspWriter.getPolicyXml(aa);
        log.info("Got result: " + got);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getLocalName().equals("UnknownAssertion"));
        log.info("Serialized: " + got);
    }

    @Test
    public void testUnknownAssertionFallbackOnMultipleElements() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "<foo/>blahblah<bar/>");
        AllAssertion aa = new AllAssertion(Arrays.asList(
            new TrueAssertion(),
            uk,
            new FalseAssertion()
        ));

        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getLocalName().equals("UnknownAssertion"));
        log.info("Serialized: " + got);
    }

    @Test
    public void testUnknownAssertionFallbackOnNoElement() throws Exception {
        UnknownAssertion uk = new UnknownAssertion(null, "asdf asdhfkajsl laskfh");
        AllAssertion aa = new AllAssertion(Arrays.asList(
            new TrueAssertion(),
            uk,
            new FalseAssertion()
        ));

        String got = WspWriter.getPolicyXml(aa);
        Document doc = XmlUtil.stringToDocument(got);
        assertTrue(doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().getNextSibling().getNextSibling().getNextSibling().getLocalName().equals("UnknownAssertion"));
        log.info("Serialized: " + got);
    }
    
    @Test
    public void testInheritedProperties() throws Exception {
        final WssBasic wssBasic = new WssBasic();
        wssBasic.setRecipientContext(new XmlSecurityRecipientContext("myfunkyactor", HexUtils.encodeBase64(new TestCertificateGenerator().generate().getEncoded())));
        String got = WspWriter.getPolicyXml(wssBasic);
        assertTrue(got.contains("myfunkyactor"));
    }

    @Test
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
        Assertion policy = new AllAssertion(Arrays.<Assertion>asList(
            new RequestSwAAssertion(bindings)
        ));
        return policy;
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

    @Test
    public void testBraServerCertOid() throws Exception {
        BridgeRoutingAssertion bra = new BridgeRoutingAssertion();
        bra.setServerCertificateGoid(new Goid(0, 232L));

        String xml = WspWriter.getPolicyXml(bra);
        log.info("Bra with server cert oid: " + xml);

        Assertion got = WspReader.getDefault().parsePermissively(xml, WspReader.INCLUDE_DISABLED);
        assertTrue(got instanceof BridgeRoutingAssertion);
        assertEquals(((BridgeRoutingAssertion)got).getServerCertificateGoid(), new Goid(0, 232L));
    }

    @Test
    @BugNumber(4752)
    public void testBug4752SamlVersion() throws Exception {
        RequireWssSaml ass = new RequireWssSaml();
        ass.setVersion(1);
        assertTrue(WspWriter.getPolicyXml(ass).contains("TokenType>urn:oasis:names:tc:SAML:1.0:assertion#Assertion<"));
        assertFalse(WspWriter.getPolicyXml(ass).contains(":SAML:2.0:"));

        RequireWssSaml2 ass2 = new RequireWssSaml2();
        ass2.setVersion(2);
        assertFalse(WspWriter.getPolicyXml(ass2).contains("SAML:1.0:"));
        assertTrue(WspWriter.getPolicyXml(ass2).contains("TokenType>urn:oasis:names:tc:SAML:2.0:assertion#Assertion<"));
    }

    @Test
    public void testDisappearingXslt() throws Exception {
        SchemaValidation sv = new SchemaValidation();
        sv.setResourceInfo(new StaticResourceInfo("<schema/>"));
        XslTransformation xsl1 = new XslTransformation();
        xsl1.setResourceInfo(new MessageUrlResourceInfo(new String[] { ".*" }));
        XslTransformation xsl2 = new XslTransformation();
        xsl2.setResourceInfo(new StaticResourceInfo("<static xsl/>"));
        HttpRoutingAssertion http = new HttpRoutingAssertion();
        AllAssertion all = new AllAssertion(Arrays.<Assertion>asList(
            sv,
            xsl2,
            xsl1,
            http
        ));
        System.out.println(WspWriter.getPolicyXml(all));
    }

    @Test
    public void testResourceInfo() throws Exception {
        tryIt(new XslTransformation(), makeStaticInfo());
        tryIt(new XslTransformation(), makeSingleInfo());
        tryIt(new XslTransformation(), makeMessageInfo());
        tryIt(new XslTransformation(), null);
    }

    @Test
    public void testWrite50Policy() throws Exception {
        AllAssertion all = new AllAssertion(Arrays.<Assertion>asList(
            new RequireWssSignedElement(),
            new RequireWssEncryptedElement(),
            new WssSignElement(),
            new WssEncryptElement(),
            new WssReplayProtection(),
            new AddWssTimestamp(),
            new RequireWssTimestamp(),
            new AddWssSecurityToken()
        ));

        WspWriter writer = new WspWriter();
        writer.setTargetVersion("5.0");
        writer.setPolicy(all);
        String value = writer.getPolicyXmlAsString();
        System.out.println("Got policy xml (for 5.0)\n" + value);
        System.out.println("Got policy xml\n" + WspWriter.getPolicyXml(all));

        assertFalse( "5.0 format request signature", value.contains(":RequireWssSignedElement") );
        assertFalse( "5.0 format request encryption", value.contains(":RequireWssEncryptedElement") );
        assertFalse( "5.0 format response signature", value.contains(":WssSignElement") );
        assertFalse( "5.0 format response encryption", value.contains(":WssEncryptElement") );
        assertFalse( "5.0 format request wss replay protection", value.contains(":WssReplayProtection") );
        assertFalse( "5.0 format response wss timestamp", value.contains(":AddWssTimestamp") );
        assertFalse( "5.0 format request wss timestamp", value.contains(":RequireWssTimestamp") );
        assertFalse( "5.0 format wss security token", value.contains(":RequireWssSecurityToken") );
    }

    @Test
    @BugNumber(8933)
    public void testWritePolicyWithInvalidXMLCharacters() throws Exception {
        final CommentAssertion commentAssertion = new CommentAssertion();
        commentAssertion.setComment( "Blah" );
        final Assertion.Comment comment = new Assertion.Comment();
        comment.setComment( "This is not valid for XML -> \u0006", Assertion.Comment.RIGHT_COMMENT );
        commentAssertion.setAssertionComment( comment );
        final AllAssertion all = new AllAssertion(Arrays.<Assertion>asList(
            commentAssertion
        ));

        final WspWriter writer = new WspWriter();
        writer.setPolicy(all);
        final String value = writer.getPolicyXmlAsString();
        System.out.println("Got policy xml\n" + value);

        assertTrue( "Base64 encoded string value: ", value.contains( "stringValueBase64" ));

        comment.setComment( "This is valid for XML", Assertion.Comment.RIGHT_COMMENT );
        writer.setPolicy(all);
        final String value2 = writer.getPolicyXmlAsString();
        System.out.println("Got policy xml\n" + value2);

        assertFalse( "Base64 encoded string value: ", value2.contains( "stringValueBase64" ));
    }

    @Test
    public  void testWritePolicyAsDocument() throws Exception {
        final CommentAssertion commentAssertion = new CommentAssertion();
        commentAssertion.setComment( "<doc><[CDATA[comment text\n]]></doc>" );
        final AllAssertion all = new AllAssertion(Arrays.<Assertion>asList(
            commentAssertion
        ));

        final Document document = WspWriter.getPolicyDocument( all );
        final String value = XmlUtil.nodeToFormattedString( document );
        System.out.println(value);
        assertTrue( "contains comment text", value.contains( "comment text" ) );
        XmlUtil.parse( value ); // ensure parsable with nested CDATA
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

