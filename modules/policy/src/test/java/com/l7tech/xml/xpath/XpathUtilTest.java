package com.l7tech.xml.xpath;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import org.jaxen.JaxenException;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.*;

import static com.l7tech.xml.xpath.XpathVersion.XPATH_1_0;
import static com.l7tech.xml.xpath.XpathVersion.XPATH_2_0;
import static org.junit.Assert.*;

/**
 *
 */
public class XpathUtilTest {
    private Map<String, String> namespaces = new HashMap<String, String>();
    {
        namespaces.put("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("m", "Some-URI");
        namespaces.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("ware", "http://warehouse.acme.com/ws");
        namespaces.put("sesyn", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("foo", "http://schemas.foo.org/");
        namespaces.put("bar", "http://schemas.bar.org/");
        namespaces.put("baz", "http://schemas.baz.org/");
    }

    private XpathVersion xpathVersion = XpathVersion.UNSPECIFIED;

    @Before
    public void setUp() {
        xpathVersion = XpathVersion.UNSPECIFIED;
    }

    private List<Node> select(Document doc, @Nullable Map<String, String> namespaces, String xpath, XpathVersion xpathVersion) throws InvalidXpathException {
        XpathResult res = XpathUtil.testXpathExpression(doc, xpath, xpathVersion, namespaces, null);        
        assertEquals(XpathResult.TYPE_NODESET, res.getType());
        List<Node> ret = new ArrayList<Node>();
        XpathResultNodeSet ns = res.getNodeSet();
        XpathResultIterator it = ns.getIterator();
        while (it.hasNext()) {
            ElementCursor c = it.nextElementAsCursor();
            ret.add(c.asDomElement());
        }
        return ret;
    }

    private List<Node> select(Document doc, String xpath, XpathVersion xpathVersion) throws InvalidXpathException {
        return select(doc, namespaces, xpath, xpathVersion);
    }

    @Test
    public void testOtherFunctions() throws Exception {
        doTestOtherFunctions(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testOtherFunctionsXP10() throws Exception {
        doTestOtherFunctions(XPATH_1_0);
    }

    @Test
    public void testOtherFunctionsXP20() throws Exception {
        doTestOtherFunctions(XpathVersion.XPATH_2_0);
    }

    private void doTestOtherFunctions(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println( XmlUtil.nodeToFormattedString(doc));
        XpathResult result = evaluate(doc, "string(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol)", xpathVersion);
        assertEquals(XpathResult.TYPE_STRING, result.getType());
        assertEquals("DIS", result.getString());

        result = evaluate(doc, "namespace-uri(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice)", xpathVersion);
        assertEquals(XpathResult.TYPE_STRING, result.getType());
        assertEquals("Some-URI", result.getString());

        result = evaluate(doc, "string-length(string(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol))", xpathVersion);
        assertEquals(XpathResult.TYPE_NUMBER, result.getType());
        assertEquals(3d, result.getNumber(), 0.01d);
    }

    @Test
    public void testContains() throws Exception {
        doTestContains(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testContainsXP10() throws Exception {
        doTestContains(XPATH_1_0);
    }

    @Test
    public void testContainsXP20() throws Exception {
        doTestContains(XpathVersion.XPATH_2_0);
    }

    private void doTestContains(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        XpathResult result = evaluate(doc, "contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'DI')", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());

        result = evaluate(doc, "not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'BZZT'))", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());

        result = evaluate(doc, "not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/elnotpresent,'BZZT'))", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());

        result = evaluate(doc, "not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'DI'))", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertFalse(result.getBoolean());

        result = evaluate(doc, "not(contains(translate(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol, 'dis', 'DIS'),'DI'))", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertFalse(result.getBoolean());
    }

    @Test
    public void testBasicXmlMessage() throws Exception {
        doTestBasicXmlMessage(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testBasicXmlMessageXP10() throws Exception {
        doTestBasicXmlMessage(XPATH_1_0);
    }

    @Test
    public void testBasicXmlMessageXP20() throws Exception {
        doTestBasicXmlMessage(XpathVersion.XPATH_2_0);
    }

    private void doTestBasicXmlMessage(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        List nodes = select(doc, null, "//*", xpathVersion);
        assertTrue("Size should have been >0", nodes.size() > 0);
    }

    private XpathResult evaluate(Document doc, Map<String, String> namespaces, String xpath, XpathVersion xpathVersion) throws JaxenException, InvalidXpathException {
        return XpathUtil.testXpathExpression(doc, xpath, xpathVersion, namespaces, null);
    }

    private XpathResult evaluate(Document doc, String xpath, XpathVersion xpathVersion) throws JaxenException, InvalidXpathException {
        return evaluate(doc, namespaces, xpath, xpathVersion);
    }

    @Test
    public void testValidateXPUnk() throws Exception {
        doTestValidate(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testValidateXP10() throws Exception {
        doTestValidate(XPATH_1_0);
    }

    @Test
    public void testValidateXP20() throws Exception {
        doTestValidate(XPATH_2_0);
    }

    private void doTestValidate(XpathVersion xpathVersion) throws Exception {
        // OK, no namespaced used or provided
        XpathUtil.validate( "/a/b/c/d/e", xpathVersion, null );

        // OK, namespaced used and provided (includes a duplicate unused namespace which is fine)
        XpathUtil.validate( "/a/prefix1:b/c/prefix2:d/e", xpathVersion, new HashMap<String,String>(){{put("prefix1","urn:p1"); put("prefix2", "urn:p2"); put("prefix3", "urn:p2");}} );

        // Fail, missing namespaces
        try {
            XpathUtil.validate( "/prefix1:b/c/d/prefix2:f", xpathVersion, null );
            fail("Expected validation exception");
        } catch ( InvalidXpathException e ) {
            assertTrue( "Message identifies prefix1", e.getMessage().contains( "prefix1" ));
            if (!XPATH_2_0.equals(xpathVersion)) {
                // Jaxon apparently includes all missing prefixes in the exception message, but Saxon does not
                assertTrue( "Message identifies prefix2", e.getMessage().contains( "prefix2" ));
            }
        }

        // Fail, missing a namespace
        try {
            XpathUtil.validate( "/prefix1:b/c/d/prefix2:f", xpathVersion, new HashMap<String,String>(){{put("prefix1","urn:p1"); put("prefix3", "urn:p3");}} );
            fail("Expected validation exception");
        } catch ( InvalidXpathException e ) {
            assertTrue( "Message identifies prefix2", e.getMessage().contains( "prefix2" ));
        }
    }

    @Test
    public void testSoapMessage() throws Exception {
        doTestSoapMessage(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testSoapMessageXP10() throws Exception {
        doTestSoapMessage(XPATH_1_0);
    }

    @Test
    public void testSoapMessageXP20() throws Exception {
        doTestSoapMessage(XpathVersion.XPATH_2_0);
    }

    private void doTestSoapMessage(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        List nodes = select(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice", xpathVersion);
        assertTrue("Size should have been >0", nodes.size() > 0);
        XpathResult result = evaluate(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol='DIS'", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue("Should have returned true (element exists)", result.getBoolean());
    }

    @Test
    public void testNamespaceSoapMessage() throws Exception {
        doTestNamespaceSoapMessage(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testNamespaceSoapMessageXP10() throws Exception {
        doTestNamespaceSoapMessage(XPATH_1_0);
    }

    @Test
    public void testNamespaceSoapMessageXP20() throws Exception {
        doTestNamespaceSoapMessage(XpathVersion.XPATH_2_0);
    }

    private void doTestNamespaceSoapMessage(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        select(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*", xpathVersion);
        evaluate(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol='DIS'", xpathVersion);
    }

    @Test
    public void testNamespaceNonExistSoapMessage() throws Exception {
        doTestNamespaceNonExistSoapMessage(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testNamespaceNonExistSoapMessageXP10() throws Exception {
        doTestNamespaceNonExistSoapMessage(XPATH_1_0);
    }

    @Test
    public void testNamespaceNonExistSoapMessageXP20() throws Exception {
        doTestNamespaceNonExistSoapMessage(XpathVersion.XPATH_2_0);
    }

    private void doTestNamespaceNonExistSoapMessage(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        try {
            select(doc, "//SOAP-ENVX:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*", xpathVersion);
            fail("the UnresolvableException should have been thrown");
        } catch (InvalidXpathException e) {
            // ok
        }
    }

    @Test
    public void testGetNamespaces() throws Exception {
        String soapDoc = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'><soap:Body><noprefix xmlns='urn:noprefix'><a:A xmlns:a='urn:A'/><a:B xmlns:a='urn:B'/><a:C xmlns:a='urn:C'/><noprefix2 xmlns='urn:noprefix2'/></noprefix></soap:Body></soap:Envelope>";
        MessageFactory factory = MessageFactory.newInstance( SOAPConstants.SOAP_1_2_PROTOCOL );
        MimeHeaders headers = new MimeHeaders();
        headers.addHeader( "Content-Type", "application/soap+xml" );
        SOAPMessage message = factory.createMessage( headers, new ByteArrayInputStream( soapDoc.getBytes() ) );
        Map<String,String> namespaces = XpathUtil.getNamespaces( message );
        System.out.println(namespaces);
        assertTrue( "Found namespace soap env", namespaces.containsValue( "http://www.w3.org/2003/05/soap-envelope" ));
        assertTrue( "Found namespace noprefix", namespaces.containsValue( "urn:noprefix" ));
        assertTrue( "Found namespace noprefix2", namespaces.containsValue( "urn:noprefix2" ));
        assertTrue( "Found namespace A", namespaces.containsValue( "urn:A" ));
        assertTrue( "Found namespace B", namespaces.containsValue( "urn:B" ));
        assertTrue( "Found namespace C", namespaces.containsValue( "urn:C" ));
    }

    @Test
    public void testMethodElementEnvelopeAncestor() throws Exception {
        doTestMethodElementEnvelopeAncestor(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testMethodElementEnvelopeAncestorXP10() throws Exception {
        doTestMethodElementEnvelopeAncestor(XPATH_1_0);
    }

    @Test
    public void testMethodElementEnvelopeAncestorXP20() throws Exception {
        doTestMethodElementEnvelopeAncestor(XpathVersion.XPATH_2_0);
    }

    private void doTestMethodElementEnvelopeAncestor(XpathVersion xpathVersion) throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        List list = select(doc, "/SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/ancestor::SOAP-ENV:Envelope", xpathVersion);
        assertTrue("Size should have been == 1, returned "+list.size(), list.size() == 1);

        list = select(doc, "/SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastBid/ancestor::SOAP-ENV:Envelope", xpathVersion);
        assertTrue("Size should have been == 0, returned "+list.size(), list.size() == 0);
    }

    @Test
    public void testNonSoap() throws Exception {
        doTestNonSoap(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testNonSoapXP10() throws Exception {
        doTestNonSoap(XPATH_1_0);
    }

    @Test
    public void testNonSoapXP20() throws Exception {
        doTestNonSoap(XpathVersion.XPATH_2_0);
    }

    public void doTestNonSoap(XpathVersion xpathVersion) throws Exception {            
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\"/>");
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");
        List nodes = select(doc, nsmap, "//*[local-name()='blah']", xpathVersion);
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object obj : nodes) {
            System.out.println(obj);
        }
    }

    @Test
    public void testXPathStringNumber() throws Exception {
        doTestXPathStringNumber(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testXPathStringNumberXP10() throws Exception {
        doTestXPathStringNumber(XPATH_1_0);
    }

    @Test
    public void testXPathStringNumberXP20() throws Exception {
        doTestXPathStringNumber(XpathVersion.XPATH_2_0);
    }

    private void doTestXPathStringNumber(XpathVersion xpathVersion) throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">123</s0:blah>");
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");

        XpathResult result = evaluate(doc, nsmap, "/s0:blah=\"123\"", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());

        result = evaluate(doc, nsmap, "/s0:blah=123", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());
    }

    @Test
    public void testLongTextNodes() throws Exception {
        doTestLongTextNodes(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testLongTextNodesXP10() throws Exception {
        doTestLongTextNodes(XPATH_1_0);
    }

    @Test
    public void testLongTextNodesXP20() throws Exception {
        doTestLongTextNodes(XpathVersion.XPATH_2_0);
    }

    private void doTestLongTextNodes(XpathVersion xpathVersion) throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">123</s0:blah>");
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");

        XpathResult result = evaluate(doc, nsmap, "(//*/text())[string-length() > 20]", xpathVersion);
        assertEquals(XpathResult.TYPE_NODESET, result.getType());
        assertTrue(result.getNodeSet().isEmpty());

        doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">blahblahblahblahblahblahblahblahblahblahblahblahblah</s0:blah>");
        result = evaluate(doc, nsmap, "(//*/text())[string-length() > 20]", xpathVersion);
        assertEquals(XpathResult.TYPE_NODESET, result.getType());
        assertFalse(result.getNodeSet().isEmpty());
        final XpathResultIterator it = result.getNodeSet().getIterator();
        assertTrue(it.hasNext());
        it.next(new XpathResultNode());
        assertFalse(it.hasNext());
    }

    @Test
    public void testLongAttributesValues() throws Exception {
        doTestLongAttributesValues(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testLongAttributesValuesXP10() throws Exception {
        doTestLongAttributesValues(XPATH_1_0);
    }

    @Test
    public void testLongAttributesValuesXP20() throws Exception {
        doTestLongAttributesValues(XpathVersion.XPATH_2_0);
    }

    private void doTestLongAttributesValues(XpathVersion xpathVersion) throws Exception {            
        Document doc = XmlUtil.stringToDocument("<blah foo=\"blah\">123</blah>");
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");
        XpathResult result = evaluate(doc, nsmap, "count(//*/@*[string-length() > 20]) > 0", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertFalse(result.getBoolean());

        doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\" foo=\"blahblahblahblahblahblahblahblahblahblahblahblahblah\">123</s0:blah>");
        result = evaluate(doc, nsmap, "count(//*/@*[string-length() > 20]) > 0", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());

        doc = XmlUtil.stringToDocument("<blah blahblahblahblahblahblahblahblahblahblahblahblahblah=\"foo\">123</blah>");
        result = evaluate(doc, nsmap, "count(//*/@*[string-length() > 20]) > 0", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertFalse(result.getBoolean());
    }

    @Test
    public void testWsiBspXpathsThatDontWorkWithTarariDirectXpath() throws InvalidXpathException {
        Map<String, String> m = new HashMap<String, String>();
        m.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        m.put("xsd", "http://www.w3.org/2001/XMLSchema");
        m.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        m.put("xenc", "http://www.w3.org/2001/04/xmlenc#");
        m.put("c14n", "http://www.w3.org/2001/10/xml-exc-c14n#");
        m.put("wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        m.put("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

        String R5426 = "0=count(//xenc:EncryptedKey//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])";
        new XpathExpression(R5426, m).compile();

        String R5427 = "0=count(//xenc:EncryptedData//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])";
        new XpathExpression(R5427, m).compile();

        String R3065 = "0=count(//ds:Signature/ds:SignedInfo/ds:Reference/ds:Transforms/ds:Transform[@Algorithm=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform\" and count(wsse:TransformationParameters/ds:CanonicalizationMethod)=0])";
        new XpathExpression(R3065, m).compile();
    }

    @Test
    public void testDeepNested() throws Exception {
        doTestDeepNested(XpathVersion.UNSPECIFIED);
    }

    @Test
    public void testDeepNestedXP10() throws Exception {
        doTestDeepNested(XPATH_1_0);
    }

    @Test
    public void testDeepNestedXP20() throws Exception {
        doTestDeepNested(XpathVersion.XPATH_2_0);
    }

    private void doTestDeepNested(XpathVersion xpathVersion) throws Exception {
        Document doc = XmlUtil.stringToDocument("<blah foo=\"blah\"><foo><bar/></foo></blah>");
        final Map<String, String> nsmap = new HashMap<String, String>();
        XpathResult result = evaluate(doc, nsmap, "count(/*/*/*) > 0", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertTrue(result.getBoolean());

        doc = XmlUtil.stringToDocument("<blah foo=\"blah\"><foo/></blah>");
        result = evaluate(doc, nsmap, "count(/*/*/*) > 0", xpathVersion);
        assertEquals(XpathResult.TYPE_BOOLEAN, result.getType());
        assertFalse(result.getBoolean());
    }

    @Test
    public void testGetUnprefixedVars() throws Exception {
        List<String> got = XpathUtil.getUnprefixedVariablesUsedInXpath("$foo = $bar + $pfx:blat", XPATH_1_0);
        assertEquals(2, got.size());
        assertEquals("foo", got.get(0));
        assertEquals("bar", got.get(1));
    }

    @Test
    public void testGetUnprefixedVarsXP20() throws Exception {
        List<String> got = XpathUtil.getUnprefixedVariablesUsedInXpath("$foo = $bar + $pfx:blat", XpathVersion.XPATH_2_0);
        assertEquals(2, got.size());
        assertTrue(got.contains("foo"));
        assertTrue(got.contains("bar"));

        List<String> got2 = XpathUtil.getUnprefixedVariablesUsedInXpath("//employee[startDateTime > xs:dateTime($dateTime)]", XpathVersion.XPATH_2_0);
        assertEquals(1, got2.size());
        assertEquals("dateTime", got2.get(0));
    }

    @Test
    public void testUsesTargetDocument() throws Exception {
        assertTrue(XpathUtil.usesTargetDocument("//foo", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("/foo/bar/baz", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("*[namespace-uri() = $foo]", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("foo/bar", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("../$blah", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("count(//foo)", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("4 = sum(*)", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("id(foo)", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("id(\"A\")", XPATH_1_0));
    }

    @Test
    public void testUsesTargetDocument_inconclusive() throws Exception {
        // These are inconclusive and so report that it might indeed use the target document
        assertTrue(XpathUtil.usesTargetDocument("$blah/bar/baz", XPATH_1_0));
        assertTrue(XpathUtil.usesTargetDocument("5 + 4 / 4 + namespace-uri()", XPATH_1_0));
    }

    @Test
    public void testUsesTargetDocument_neg() {
        assertFalse(XpathUtil.usesTargetDocument("0=0", XPATH_1_0));
        assertFalse(XpathUtil.usesTargetDocument("1=0", XPATH_1_0));
        assertFalse(XpathUtil.usesTargetDocument("$blah", XPATH_1_0));
        assertFalse(XpathUtil.usesTargetDocument("$blah > 4", XPATH_1_0));
        assertFalse(XpathUtil.usesTargetDocument("count($blah)", XPATH_1_0));
    }
    
    @Test
    public void testUsesTargetDocumentXP20() {
        // For XPath 2.0, we currently don't even try to determin this, and just always assume the expression might use the target document.
        assertTrue(XpathUtil.usesTargetDocument("//foo", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("/foo/bar/baz", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("*[namespace-uri() = $foo]", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("foo/bar", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("../$blah", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("count(//foo)", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("4 = sum(*)", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("id(foo)", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("id(\"A\")", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("$blah/bar/baz", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("5 + 4 / 4 + namespace-uri()", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("0=0", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("1=0", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("$blah", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("$blah > 4", XPATH_2_0));
        assertTrue(XpathUtil.usesTargetDocument("count($blah)", XPATH_2_0));
    }

    @Test
    public void testNamespaceRemoval() {
        Map<String,String> namespaces = new HashMap<String,String>(){{
            put( "soap", "http://soapnamespacehere" );
            put( "remove1", "http://namespace1here" );
            put( "remove2", "http://namespace2here" );
            put( "keep1", "http://namespace3here" );
            put( "keep2", "http://namespace4here" );
        }};
        XpathUtil.removeNamespaces( "/soap:Envelope/soap:Body/remove1:used", namespaces, new HashSet<String>( Arrays.asList("remove1","remove2") ) );
        assertTrue( "soap present", namespaces.containsKey("soap") );
        assertTrue( "remove1 present", namespaces.containsKey("remove1") ); // present since used
        assertFalse( "remove2 present", namespaces.containsKey("remove2") );
        assertTrue( "remove1 present", namespaces.containsKey("keep1") ); // present since remove not requested
        assertTrue( "remove1 present", namespaces.containsKey("keep2") );

        XpathUtil.removeNamespaces( "'literal'", namespaces, new HashSet<String>( Arrays.asList("remove1","remove2") ) );
        assertTrue( "soap present", namespaces.containsKey("soap") );
        assertFalse( "remove1 present", namespaces.containsKey("remove1") );
        assertFalse( "remove2 present", namespaces.containsKey("remove2") );
        assertTrue( "remove1 present", namespaces.containsKey("keep1") ); // present since remove not requested
        assertTrue( "remove1 present", namespaces.containsKey("keep2") );
    }

    @Test
    public void testNamespaceRemovalInvalidXPath() {
        XpathUtil.removeNamespaces( "/soap:Envelope", Collections.<String,String>emptyMap(), Collections.<String>emptySet() );
        XpathUtil.removeNamespaces( " # 2#$@%  @@", Collections.<String,String>emptyMap(), Collections.<String>emptySet() );
        XpathUtil.removeNamespaces( "//://///!!$@%  @@", Collections.<String,String>emptyMap(), Collections.<String>emptySet() );
    }

    @Test
    public void testXpathLiteral() throws Exception {
        literalTest("blah");
        literalTest("'blah");
        literalTest("\"blah");
        literalTest("'blah'");
        literalTest("\"blah\"");
        literalTest("\"blah's\"");
        literalTest("\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'");
        literalTest("");
        literalTest("'");
        literalTest("\"");
        literalTest("''''''");
        literalTest("\"\"\"\"\"\"\"");
        literalTest("'''\"\"\"e'r'r\"w'r'\"\"\"\"'''");
        literalTest("aaaaaa'aaaa");
        literalTest("aaaaaa\"aaaa");
        literalTest("aaa'aaa\"aa'aa");
        literalTest("aaaaaa\"aaaa'");
    }

    @Test
    public void testXpathLiteralXP10() throws Exception {
        xpathVersion = XPATH_1_0;
        testXpathLiteral();
    }

    @Test
    public void testXpathLiteralXP20() throws Exception {
        xpathVersion = XPATH_2_0;
        testXpathLiteral();
    }

    private void literalTest( final String value ) throws Exception {
        XpathResult result = evaluate( null, XpathUtil.literalExpression(value), xpathVersion );
        assertEquals(XpathResult.TYPE_STRING, result.getType());
        assertEquals( value, result.getString() );
    }

    @Test
    public void testGetNamespacesUsedByXpath() throws Exception {
        Set<String> got = XpathUtil.getNamespacePrefixesUsedByXpath("/s:foo/s:bar[@blah=\'x:bleef\']", xpathVersion, false);
        assertEquals(1, got.size());
        assertTrue(got.contains("s"));
    }

    @Test
    public void testGetNamespacesUsedByXpath_withQnameLiterals() throws Exception {
        Set<String> got = XpathUtil.getNamespacePrefixesUsedByXpath("/s:foo/s:bar[@blah=\'x:bleef\']", xpathVersion, true);
        assertEquals(2, got.size());
        assertTrue(got.contains("s"));
        assertTrue(got.contains("x"));
    }

    @Test
    public void testGetNamespacesUsedByXpath_withQnameLiterals_XP20() throws Exception {
        xpathVersion = XPATH_2_0;
        // Currently the lookForQnameLiterals flag is ignored for XPath 2.0
        Set<String> got = XpathUtil.getNamespacePrefixesUsedByXpath("/s:foo/s:bar[@blah=\'x:bleef\']", xpathVersion, true);
        assertEquals(1, got.size());
        assertTrue(got.contains("s"));
    }

    @Test
    public void testGetNamespacesUsedByXpath_complex() throws Exception {
        Set<String> got = XpathUtil.getNamespacePrefixesUsedByXpath("/s:foo/s:bar[@blah=\'x:bleef\' and ack:myfunc()][@bloof = $vp:myvar]", xpathVersion, true);
        assertEquals(4, got.size());
        assertTrue(got.contains("s"));
        assertTrue(got.contains("x"));
        assertTrue(got.contains("ack"));
        assertTrue(got.contains("vp"));
    }

    @Test
    public void testGetNamespacesUsedByXpath_complex_XP20() throws Exception {
        xpathVersion = XPATH_2_0;
        // Omit "ack:myfunc()" since Saxon/XPath 2.0 won't compile references to undefined functions
        // Omit "x" from expected result set since for Xpath 2.0 we currently ingore the "lookForQnameLiterals" flag
        Set<String> got = XpathUtil.getNamespacePrefixesUsedByXpath("/s:foo/s:bar[@blah='x:bleef'][@bloof = $vp:myvar]", xpathVersion, false);
        assertEquals(2, got.size());
        assertTrue(got.contains("s"));
        assertTrue(got.contains("vp"));
    }

    @Test(expected = ParseException.class)
    public void testGetNamespacesUsedByXpath_invalidXpath() throws Exception {
        XpathUtil.getNamespacePrefixesUsedByXpath("/s:foo/s:bar[@blah=\'x:bleef\'", xpathVersion, false);
    }

    @Test(expected = ParseException.class)
    public void testGetNamespacesUsedByXpath_invalidXpath_XP20() throws Exception {
        xpathVersion = XPATH_2_0;
        testGetNamespacesUsedByXpath_invalidXpath();
    }

}
