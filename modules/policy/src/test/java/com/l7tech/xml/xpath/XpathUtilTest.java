package com.l7tech.xml.xpath;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.InvalidXpathException;
import org.jaxen.JaxenException;
import org.jaxen.UnresolvableException;
import static org.junit.Assert.*;

import org.jaxen.saxpath.SAXPathException;
import org.junit.*;
import org.w3c.dom.Document;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

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

    private List select(Document doc, Map<String, String> namespaces, String xpath) throws JaxenException {
        return XpathUtil.compileAndSelect(doc, xpath, namespaces, null);
    }

    private List select(Document doc, String xpath) throws JaxenException {
        return select(doc, namespaces, xpath);
    }

    @Test
    public void testOtherFunctions() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println( XmlUtil.nodeToFormattedString(doc));
        List nodes = select(doc, "string(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol)");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            String obj = (String) node;
            assertTrue("Value must be DIS", obj.equals("DIS"));
        }

        nodes = select(doc, "namespace-uri(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice)");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            String obj = (String) node;
            assertTrue("Value must be Some-URI", obj.equals("Some-URI"));
        }

        nodes = select(doc, "string-length(string(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            Double obj = (Double) node;
            assertTrue("Value must be 3", obj.intValue() == 3);
        }
    }

    @Test
    public void testContains() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        List nodes = select(doc, "contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'DI')");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue("Should return true", bool);
        }

        nodes = select(doc, "not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'BZZT'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue("Should return true", bool);
        }

        nodes = select(doc, "not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/elnotpresent,'BZZT'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue("Should return true", bool);
        }

        nodes = select(doc, "not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'DI'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node1 : nodes) {
            Boolean bool = (Boolean) node1;
            assertTrue("Should return false", !bool);
        }

        nodes = select(doc, "not(contains(translate(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol, 'dis', 'DIS'),'DI'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue("Should return false", !bool);
        }
    }

    @Test
    public void testBasicXmlMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        List nodes = select(doc, null, "//*");
        assertTrue("Size should have been >0", nodes.size() > 0);
    }

    private Object evaluate(Document doc, Map<String, String> namespaces, String xpath) throws JaxenException {
        return XpathUtil.compileAndEvaluate(doc, xpath, namespaces, null);
    }

    private Object evaluate(Document doc, String xpath) throws JaxenException {
        return evaluate(doc, namespaces, xpath);
    }

    @Test
    public void testValidate() throws Exception {
        // OK, no namespaced used or provided
        XpathUtil.validate( "/a/b/c/d/e", null );

        // OK, namespaced used and provided (includes a duplicate unused namespace which is fine)
        XpathUtil.validate( "/a/prefix1:b/c/prefix2:d/e", new HashMap<String,String>(){{put("prefix1","urn:p1"); put("prefix2", "urn:p2"); put("prefix3", "urn:p2");}} );

        // Fail, missing namespaces
        try {
            XpathUtil.validate( "/prefix1:b/c/d/prefix2:f", null );
            fail("Expected validation exception");
        } catch ( SAXPathException e ) {
            assertTrue( "Message identifies prefix1", e.getMessage().contains( "prefix1" ));
            assertTrue( "Message identifies prefix2", e.getMessage().contains( "prefix2" ));
        }

        // Fail, missing a namespace
        try {
            XpathUtil.validate( "/prefix1:b/c/d/prefix2:f", new HashMap<String,String>(){{put("prefix1","urn:p1"); put("prefix3", "urn:p3");}} );
            fail("Expected validation exception");
        } catch ( SAXPathException e ) {
            assertTrue( "Message identifies prefix2", e.getMessage().contains( "prefix2" ));
        }
    }

    @Test
    public void testSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        List nodes = select(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice");
        assertTrue("Size should have been >0", nodes.size() > 0);
        Object ret = evaluate(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol='DIS'");
        Boolean bool = (Boolean)ret;
        assertTrue("Should have returned true (element exists)", bool);
    }

    @Test
    public void testNamespaceSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        select(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*");
        evaluate(doc, "//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol='DIS'");
    }

    @Test
    public void testNamespaceNonExistSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        try {
            select(doc, "//SOAP-ENVX:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*");
            fail("the UnresolvableException should have been thrown");
        } catch (UnresolvableException e) {
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
    public void testMethodElementEnvelopeAcestor() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        List list = select(doc, "/SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/ancestor::SOAP-ENV:Envelope");
        assertTrue("Size should have been == 1, returned "+list.size(), list.size() == 1);

        list = select(doc, "/SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastBid/ancestor::SOAP-ENV:Envelope");
        assertTrue("Size should have been == 0, returned "+list.size(), list.size() == 0);
    }

    @Test
    public void testNonSoap() throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\"/>");
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");
        List nodes = select(doc, nsmap, "//*[local-name()='blah']");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Object obj : nodes) {
            System.out.println(obj);
        }
    }

    @Test
    public void testXPathStringNumber() throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">123</s0:blah>");
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");
        List nodes = select(doc, nsmap, "/s0:blah=\"123\"");
        assertTrue("Size should have been > 0", nodes.size() > 0);
        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue(bool);
        }
        nodes = select(doc, nsmap, "/s0:blah=123");
        assertTrue("Size should have been > 0", nodes.size() > 0);
        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue(bool);
        }
    }

    @Test
    public void testLongTextNodes() throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">123</s0:blah>");
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");
        List nodes = select(doc, nsmap, "(//*/text())[string-length() > 20]");
        assertTrue(nodes.size() == 0);
        doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">blahblahblahblahblahblahblahblahblahblahblahblahblah</s0:blah>");
        nodes = select(doc, nsmap, "(//*/text())[string-length() > 20]");
        assertTrue(nodes.size() == 1);

    }

    @Test
    public void testLongAttributesValues() throws Exception {
        Document doc = XmlUtil.stringToDocument("<blah foo=\"blah\">123</blah>");
        Map<String, String> nsmap = new HashMap<String, String>();
        nsmap.put("s0", "http://grrr.com/nsblah");
        List nodes = select(doc, nsmap, "count(//*/@*[string-length() > 20]) > 0");
        System.out.println(nodes.size());

        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertFalse(bool);
        }
        doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\" foo=\"blahblahblahblahblahblahblahblahblahblahblahblahblah\">123</s0:blah>");
        nodes = select(doc, nsmap, "count(//*/@*[string-length() > 20]) > 0");

        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertTrue(bool);
        }

        doc = XmlUtil.stringToDocument("<blah blahblahblahblahblahblahblahblahblahblahblahblahblah=\"foo\">123</blah>");
        nodes = select(doc, nsmap, "count(//*/@*[string-length() > 20]) > 0");

        for (Object node : nodes) {
            Boolean bool = (Boolean) node;
            assertFalse(bool);
        }
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
        Document doc = XmlUtil.stringToDocument("<blah foo=\"blah\"><foo><bar/></foo></blah>");
        final Map<String, String> nsmap = new HashMap<String, String>();
        List res = select(doc, nsmap, "count(/*/*/*) > 0");
        Boolean bool = (Boolean)res.iterator().next();
        assertTrue(bool);
        doc = XmlUtil.stringToDocument("<blah foo=\"blah\"><foo/></blah>");
        res = select(doc, nsmap, "count(/*/*/*) > 0");
        bool = (Boolean)res.iterator().next();
        assertFalse(bool);
    }

    @Test
    public void testGetUnprefixedVars() throws Exception {
        List<String> got = XpathUtil.getUnprefixedVariablesUsedInXpath("$foo = $bar + $pfx:blat");
        assertEquals(2, got.size());
        assertEquals("foo", got.get(0));
        assertEquals("bar", got.get(1));
    }

    @Test
    public void testUsesTargetDocument() throws Exception {
        assertTrue(XpathUtil.usesTargetDocument("//foo"));
        assertTrue(XpathUtil.usesTargetDocument("/foo/bar/baz"));
        assertTrue(XpathUtil.usesTargetDocument("*[namespace-uri() = $foo]"));
        assertTrue(XpathUtil.usesTargetDocument("foo/bar"));
        assertTrue(XpathUtil.usesTargetDocument("../$blah"));
        assertTrue(XpathUtil.usesTargetDocument("count(//foo)"));
        assertTrue(XpathUtil.usesTargetDocument("4 = sum(*)"));
        assertTrue(XpathUtil.usesTargetDocument("id(foo)"));
        assertTrue(XpathUtil.usesTargetDocument("id(\"A\")"));
    }

    @Test
    public void testUsesTargetDocument_inconclusive() throws Exception {
        // These are inconclusive and so report that it might indeed use the target document
        assertTrue(XpathUtil.usesTargetDocument("$blah/bar/baz"));
        assertTrue(XpathUtil.usesTargetDocument("5 + 4 / 4 + namespace-uri()"));
    }

    @Test
    public void testUsesTargetDocument_neg() {
        assertFalse(XpathUtil.usesTargetDocument("0=0"));
        assertFalse(XpathUtil.usesTargetDocument("1=0"));
        assertFalse(XpathUtil.usesTargetDocument("$blah"));
        assertFalse(XpathUtil.usesTargetDocument("$blah > 4"));
        assertFalse(XpathUtil.usesTargetDocument("count($blah)"));
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

    private void literalTest( final String value ) throws Exception {
        String result = (String) evaluate( null, XpathUtil.literalExpression(value) );
        assertEquals( value, result );
    }
}
