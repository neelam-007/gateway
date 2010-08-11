package com.l7tech.xml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.DomCompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultNodeSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author alex
 * @version $Revision$
 */
public class XmlUtilTest {
    private static final Logger logger = Logger.getLogger(XmlUtilTest.class.getName());
    private static final String PI_XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<?xml-stylesheet href=\"foo\" type=\"text/xsl\"?>\n<foo/>";

    private static Document getTestDocument() throws Exception {
        return TestDocuments.getTestDocument( TestDocuments.PLACEORDER_WITH_MAJESTY );

    }

    @Test
    public void testFindAllNamespaces() throws Exception {
        Element el = getTestDocument().getDocumentElement();
        Map foo = DomUtils.findAllNamespaces(el);
        logger.info("Found namespaces: " + foo);
    }

        @Test
    public void testFindAllNamespaces2() throws Exception {
        String soapDoc = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'><soap:Body><noprefix xmlns='urn:noprefix'><a:A xmlns:a='urn:A'/><a:B xmlns:a='urn:B'/><a:C xmlns:a='urn:C'/><noprefix2 xmlns='urn:noprefix2'/></noprefix></soap:Body></soap:Envelope>";
        Map<String,String> namespaces = DomUtils.findAllNamespaces( XmlUtil.parse( soapDoc ).getDocumentElement() );
        System.out.println(namespaces);
        assertTrue( "Found namespace soap env", namespaces.containsValue( "http://www.w3.org/2003/05/soap-envelope" ));
        assertTrue( "Found namespace noprefix", namespaces.containsValue( "urn:noprefix" ));
        assertTrue( "Found namespace noprefix2", namespaces.containsValue( "urn:noprefix2" ));
        assertTrue( "Found namespace A", namespaces.containsValue( "urn:A" ));
        assertTrue( "Found namespace B", namespaces.containsValue( "urn:B" ));
        assertTrue( "Found namespace C", namespaces.containsValue( "urn:C" ));
    }

    private void assertElementEquals( Element element, String nsuri, String name ) {
        assertTrue(element != null);
        assertTrue(nsuri.equals(element.getNamespaceURI()));
        assertTrue(name.equals(element.getLocalName()));
    }

    @Test
    public void testDomAttrChildren() throws Exception {
        Document d = XmlUtil.stringAsDocument("<foo attr1=\"blah\" attr2=\"blah2\"/>");
        assertTrue(d.getDocumentElement().getChildNodes().getLength() == 0);
        assertFalse(d.getDocumentElement().hasChildNodes());
    }

    @Test
    public void testFindFirstChildElement() throws Exception {
        Document d = getTestDocument();
        Element header = DomUtils.findFirstChildElement( d.getDocumentElement() );
        assertElementEquals( header, SOAPConstants.URI_NS_SOAP_ENVELOPE, SoapUtil.HEADER_EL_NAME );
        Element security = DomUtils.findFirstChildElement( header );
        Element sctoken = DomUtils.findFirstChildElement( security );
        Element ident = DomUtils.findFirstChildElement( sctoken );
        Element none = DomUtils.findFirstChildElement( ident );
        assertNull( none );
    }

    @Test
    public void testFindOnlyOne() throws Exception {
        Document d = getTestDocument();
        Element header = DomUtils.findOnlyOneChildElementByName( d.getDocumentElement(),
                                                              SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                              SoapUtil.HEADER_EL_NAME );
        assertNotNull(header);

        Element security = DomUtils.findOnlyOneChildElementByName( header, SoapUtil.SECURITY_URIS_ARRAY, SoapUtil.SECURITY_EL_NAME );
        assertNotNull(security);

        try {
            Element firstSignature = DomUtils.findOnlyOneChildElementByName( security,
                                                                          SoapUtil.DIGSIG_URI,
                                                                          SoapUtil.SIGNATURE_EL_NAME );
            fail("Expected exception not thrown");
        } catch ( TooManyChildElementsException e ) {
            // Expected
        }

        Element nil = DomUtils.findOnlyOneChildElementByName( security, "Foo", "Bar" );
        assertNull(nil);

        Element sec2 = DomUtils.findOnlyOneChildElement(header);
        assertEquals(security, sec2);
    }


    @Test
    public void testFindFirstChildElementByName() throws Exception {
        Document d = getTestDocument();
        Element header = DomUtils.findFirstChildElementByName( d.getDocumentElement(),
                                                              SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                              SoapUtil.HEADER_EL_NAME );
        assertElementEquals( header, SOAPConstants.URI_NS_SOAP_ENVELOPE, SoapUtil.HEADER_EL_NAME );
        Element security = DomUtils.findFirstChildElement( header );
        Element firstSignature = DomUtils.findFirstChildElementByName( security,
                                                                      SoapUtil.DIGSIG_URI,
                                                                      SoapUtil.SIGNATURE_EL_NAME );
        // Make sure it's really the first one
        assertTrue( XmlUtil.nodeToString( firstSignature ).indexOf( "#signref1" ) >= 0 );
        assertElementEquals( firstSignature, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME );

        Element sctoken = DomUtils.findFirstChildElement( security );
        // Make sure it doesn't find spurious children
        Element none = DomUtils.findFirstChildElementByName( sctoken, SoapUtil.XMLENC_NS, "Foo" );
        assertNull(none);
    }

    @Test
    public void testFindChildElementsByName() throws Exception {
        Document d = getTestDocument();
        List children = DomUtils.findChildElementsByName( d.getDocumentElement(),
                                                              SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                              SoapUtil.HEADER_EL_NAME );
        assertTrue( children.size() > 0 );
        Element header = (Element)children.get(0);
        assertElementEquals( header, SOAPConstants.URI_NS_SOAP_ENVELOPE, SoapUtil.HEADER_EL_NAME );
        Element security = DomUtils.findFirstChildElement( header );
        List signatures = DomUtils.findChildElementsByName( security, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME);

        assertTrue( signatures.size() == 3 );
        for ( int i = 0; i < signatures.size(); i++ ) {
            Element child  = (Element)signatures.get(i);
            assertElementEquals( child, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME );
            assertTrue( XmlUtil.nodeToString( child ).indexOf( "#signref" + (i+1) ) >= 0 );
        }

        Element sctoken = DomUtils.findFirstChildElement( security );
        // Make sure it doesn't find spurious children
        List none = DomUtils.findChildElementsByName( sctoken, SoapUtil.XMLENC_NS, "Foo" );
        assertTrue(none.isEmpty());
    }

    @Test
    public void testFindChildElementsByNameWithNSArray() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        Element env = d.getDocumentElement();
        Element header = DomUtils.findFirstChildElement(env);
        List children = DomUtils.findChildElementsByName(header, SoapUtil.SECURITY_URIS_ARRAY, SoapUtil.SECURITY_EL_NAME);
        assertTrue(children.size() == 3);
        Element sec1 = (Element)children.get(0);
        Element sec2 = (Element)children.get(1);
        Element sec3 = (Element)children.get(2);
        assertTrue(SoapUtil.SECURITY_NAMESPACE4.equals(sec1.getNamespaceURI()));
        assertTrue(SoapUtil.SECURITY_NAMESPACE2.equals(sec2.getNamespaceURI()));
        assertTrue(SoapUtil.SECURITY_NAMESPACE.equals(sec3.getNamespaceURI()));
    }

    @Test
    public void testGetPayloadNamespaceUri() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        assertNull(SoapUtil.getPayloadNames(d));

        d = TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST);
        assertEquals("http://warehouse.acme.com/ws", SoapUtil.getPayloadNames(d)[0].getNamespaceURI());
    }

    @Test
    public void testFindActivePrefixForNamespace() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);

        Element env = d.getDocumentElement();
        Element header = DomUtils.findFirstChildElement(env);
        Element sec1 = DomUtils.findFirstChildElement(header);

        String prefix = DomUtils.findActivePrefixForNamespace(sec1, SoapUtil.SECURITY_NAMESPACE4);
        assertEquals(prefix, "sec1");

        prefix = DomUtils.findActivePrefixForNamespace(sec1, "http://schemas.xmlsoap.org/soap/envelope/");
        assertEquals(prefix, "s");

        prefix = DomUtils.findActivePrefixForNamespace(sec1, "http://blah.bletch");
        assertNull(prefix);

        prefix = DomUtils.findActivePrefixForNamespace(header, SoapUtil.SECURITY_NAMESPACE4);
        assertNull(prefix);

    }

    @Test
    public void testParseEmptyString() {
        try {
            Document d = XmlUtil.stringToDocument("");
            fail("Expected exception not thrown.  Returned d=" + d);
        } catch (Exception e) {
            // Ok
            logger.log(Level.INFO, "The expected exception was thrown: " + e.getMessage());
        }
    }

    @Test
    public void testParseNull() {
        try {
            Document d = XmlUtil.stringToDocument(null);
            fail("Expected exception not thrown.  Returned d=" + d);
        } catch (Exception e) {
            // Ok
            logger.log(Level.INFO, "The expected exception was thrown: " + e.getMessage());
        }
    }

    @Test
    public void testGetNamespaceMap() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        Element header = DomUtils.findFirstChildElement(d.getDocumentElement());
        assertNotNull(header);
        Element sec2 = DomUtils.findFirstChildElementByName(header, "http://schemas.xmlsoap.org/ws/2002/12/secext", "Security");
        assertNotNull(sec2);
        Element t = DomUtils.findFirstChildElement(sec2);
        assertNotNull(t);
        Map nsmap = DomUtils.getNamespaceMap(t);
        assertEquals(nsmap.size(), 3);
        assertTrue(nsmap.containsKey("s"));
        assertTrue(nsmap.containsValue("urn:testblah"));
        assertEquals(nsmap.get("sec2"), "http://schemas.xmlsoap.org/ws/2002/12/secext");
    }

    /**
     * Test that redeclared namespace prefixes are handled correctly 
     */
    @Test
    public void testFindNamespacesInScope() throws Exception {
        Document test = XmlUtil.parse( "<a xmlns='http://a'><b xmlns:b='http://b'><c xmlns:b='http://c'><d><e xmlns=''></e></d></c></b></a>" );
        Element eElement = (Element) test.getDocumentElement().getElementsByTagNameNS( "", "e" ).item( 0 );
        Map<String,String> nsMap = DomUtils.getNamespaceMap( eElement );
        Assert.assertEquals( "b namespace", "http://c", nsMap.get("b"));
        Assert.assertEquals( "Default namespace", "", nsMap.get(""));
    }

    @Test
    public void testStripWhitespace() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        DomUtils.stripWhitespace(d.getDocumentElement());
        String stripped = XmlUtil.nodeToString(d);
        String wantstripped = XmlUtil.nodeToString(XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS_STRIPPED));
        assertEquals(wantstripped, stripped);
    }

    @Test
    public void testProcessingInstructionDom() throws Exception {
        Document doc = XmlUtil.parse(new StringReader(PI_XML), false);
        DomElementCursor cursor = new DomElementCursor(doc);
        cursor.moveToRoot();
        XpathResult res = cursor.getXpathResult(new DomCompiledXpath("processing-instruction('xml-stylesheet')", null) { });
        XpathResultNodeSet nodes = res.getNodeSet();
        assertTrue(nodes.size() > 0);
        assertEquals(nodes.getNodeName(0), "xml-stylesheet");
        String nv = nodes.getNodeValue(0);
        assertEquals(nv, "href=\"foo\" type=\"text/xsl\"");
        Pattern spaceSplitter = Pattern.compile("\\s+");
        long before = System.currentTimeMillis();
        int n = 0;
        for (int i = 0; i < 1000000; i++) {
            n += spaceSplitter.split(nv).length;
        }
        long now = System.currentTimeMillis();
        System.err.println("Found " + n + " strings in " + (now - before) + "ms");
    }

    @Test
    public void testProcessingInstructionMaybeTarari() throws Exception {
        TarariLoader.compile();
        Message msg = new Message(new ByteArrayStashManager(),
            ContentTypeHeader.XML_DEFAULT,
            new ByteArrayInputStream(PI_XML.getBytes("UTF-8"))
        );
        ElementCursor cursor = msg.getXmlKnob().getElementCursor();
        cursor.moveToRoot();
        XpathResult res = cursor.getXpathResult(new XpathExpression("processing-instruction('xml-stylesheet')", null).compile());
        XpathResultNodeSet nodes = res.getNodeSet();
        assertTrue(nodes.size() > 0);
        assertEquals(nodes.getNodeName(0), "xml-stylesheet");
        assertEquals(nodes.getNodeValue(0), "href=\"foo\" type=\"text/xsl\"");
    }

    @Test
    public void testVisitNodes() throws Exception {
        final Document document = XmlUtil.parse( "<a><b><c></c></b><d></d><e><f><g></g></f></e></a>" );
        final List<String> nodeNames = new ArrayList<String>();
        XmlUtil.visitNodes( document, new Functions.UnaryVoid<Node>(){
            @Override
            public void call( final Node node ) {
                nodeNames.add( node.getLocalName() );
            }
        } );
        assertEquals("Node local names", Arrays.asList( null, "a", "b", "c", "d", "e", "f", "g" ), nodeNames );
    }

    @Test
    public void testWhitespaceInProlog() throws Exception {
        XmlUtil.stringToDocument(" \t \r  \n   " + XmlUtil.nodeToString(XmlUtil.stringToDocument("<foo/>")));
    }

    public void testNonWhitespaceInProlog() throws Exception {
        try {
            XmlUtil.stringToDocument("  x  " + XmlUtil.nodeToString(XmlUtil.stringToDocument("<foo/>")));
            fail("Content in prolog did not cause parse failure");
        } catch (SAXException e) {
            // Ok
        }
    }

    private final String REUTERS_SCHEMA_URL = "http://locutus/reuters/schemas1/ReutersResearchAPI.xsd";

    @Ignore("Test disabled because it depends on a resource on Alex's workstation")
    @Test
    public void testTranslateReutersSchemaNamespaces() throws Exception {
        InputStream is = new URL(REUTERS_SCHEMA_URL).openStream();
        String schemaXml = new String( IOUtils.slurpStream(is));

        Document doc = XmlUtil.stringToDocument(schemaXml);

        Element newDocEl = DomUtils.normalizeNamespaces(doc.getDocumentElement());
        String normalizedXml = XmlUtil.nodeToString(newDocEl);
        Document normalizedDoc = XmlUtil.stringToDocument(normalizedXml);
        System.out.println("Normalized:\n" + XmlUtil.nodeToFormattedString(normalizedDoc));
    }

    @Test
    public void testLeadingWhitespace() {
        InputStream pis = new ByteArrayInputStream(XML_WITH_LEADING_WHITESPACE.getBytes());
        try {
            XmlUtil.parse(pis);
            fail("XML parser failed to reject stream with leading whitespace");
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            fail("Unexpected exception was thrown");
        } catch (SAXException e) {
            // Ok
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCreateEmptyDocumentNullName() {
        XmlUtil.createEmptyDocument(null, "s", "urn:blah");
    }

    @Test
    public void testCreateEmptyDocumentNoPrefix() {
        Document doc = XmlUtil.createEmptyDocument("foo", null, "urn:blah");
        checkDocument(doc, "foo", null, "foo", "urn:blah");
    }
    
    @Test
    public void testCreateEmptyDocumentNoNsUri() {
        Document doc = XmlUtil.createEmptyDocument("foo", "s", null);
        checkDocument(doc, "foo", null, null, null);
    }

    @Test
    public void testCreateEmptyDocumentNoNsUriOrPrefix() {
        Document doc = XmlUtil.createEmptyDocument("foo", null, null);
        checkDocument(doc, "foo", null, null, null);
    }

    @Test
    public void testCreateEmptyDocument() {
        Document doc = XmlUtil.createEmptyDocument("foo", "s", "urn:blah");
        checkDocument(doc, "s:foo", "s", "foo", "urn:blah");
    }

    private static void checkDocument(Document doc, String wantNodeName, String wantPrefix, String wantLocalName, String wantNsUri) {
        try {
            assertNotNull(doc);
            Element root = doc.getDocumentElement();
            checkElement(root, wantNodeName, wantPrefix, wantLocalName, wantNsUri);
            final String string = XmlUtil.nodeToString(doc);
            System.out.println("Got xml:\n" + string);
            Document reparsed = XmlUtil.stringToDocument(string);
            checkElement(reparsed.getDocumentElement(), wantNodeName, wantPrefix, wantLocalName, wantNsUri);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkElement(Element element, String wantNodeName, String wantPrefix, String wantLocalName, String wantNsUri) {
        assertNotNull(element);
        assertEquals(element.getNodeName(), wantNodeName);
        assertEquals(element.getPrefix(), wantPrefix);
        assertEquals(element.getNamespaceURI(), wantNsUri);
        if (wantNsUri != null)
            assertEquals(element.getLocalName(), wantLocalName);
    }

    @Test
    @BugNumber(6851)
    public void testSerializerTransparencyOfXmlElements_with_Default() throws Exception {
        doTestSerializerTransparencyOfXmlElements(null);
    }

    @Test
    @BugNumber(6851)
    @Ignore("Disabled because this serialization is not transparent when using XSS4J's W3CCanonicalizer2WC")
    public void testSerializerTransparencyOfXmlElements_with_XSS4J() throws Exception {
        doTestSerializerTransparencyOfXmlElements(true);
    }

    @Test
    @BugNumber(6851)
    @SuppressWarnings({"deprecation"})
    public void testSerializerTransparencyOfXmlElements_with_XMLSerializer() throws Exception {
        doTestSerializerTransparencyOfXmlElements(false);
    }

    @SuppressWarnings({"deprecation"})
    private void doTestSerializerTransparencyOfXmlElements(Boolean useXss4jSer) throws SAXException, IOException {
        try {
            XmlUtil.setSerializeWithXss4j(useXss4jSer);
            assertTransparency("<a xml:a=\"a\"><a xml:a=\"a\"/></a>");
        } finally {
            XmlUtil.setSerializeWithXss4j(null);
        }
    }

    @Test
    @Ignore("Currently our serializer always omits the XML declaration")
    public void testSerializerTransparencyOfXmlDecl() throws Exception {
        assertTransparency("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a xml:a=\"a\"><a xml:a=\"a\"/></a>");
    }

    private void assertTransparency(String in) throws SAXException, IOException {
        Document doc1 = XmlUtil.stringToDocument(in);
        String str1 = XmlUtil.nodeToString(doc1);
        assertEquals(in, str1);
        Document doc2 = XmlUtil.stringToDocument(str1);
        String str2 = XmlUtil.nodeToString(doc2);
        assertEquals(str1, str2);
        Document doc3 = XmlUtil.stringToDocument(str2);
        String str3 = XmlUtil.nodeToString(doc3);
        assertEquals(str2, str3);
    }

    // Normal parser should refuse DOCTYPE
    @Test(expected = SAXParseException.class)
    @BugNumber(7685)
    public void testDoctypeRefused() throws Exception {
        String xml = TestDocuments.getTestDocumentAsXml(TestDocuments.BUG_7685_SPLOIT_XML);
        XmlUtil.stringToDocument(xml);
    }

    // Xerces2 2.9.1 parses the illegal DOCTYPE correctly, then our safe entity resolver refuses the external entity
    @Test(expected = IOException.class, timeout = 5000)
    @BugNumber(7685)
    public void testDoctypeInfiniteLoop() throws Exception {
        String xml = TestDocuments.getTestDocumentAsXml(TestDocuments.BUG_7685_SPLOIT_XML);
        XmlUtil.parse(new StringReader(xml), true);
    }

    @Test(expected = SAXException.class, timeout = 5000)
    @BugNumber(7685)
    public void testDoctypeParserInfiniteLoop_defaultParser() throws IOException, SAXException {
        XmlUtil.parse(new ByteArrayInputStream(HexUtils.decodeBase64(BUG_7685_DOCTYPE_INFINITE_LOOP_SPLOIT_BASE64)));
    }

    @Test(expected = SAXParseException.class, timeout = 5000)
    @BugNumber(7685)
    public void testDoctypeParserInfiniteLoop_doctypeAllowingParser() throws IOException, SAXException {
        XmlUtil.parse(new ByteArrayInputStream(HexUtils.decodeBase64(BUG_7685_DOCTYPE_INFINITE_LOOP_SPLOIT_BASE64)), true);
    }

    @Test
    public void testElementImplDoesNotOverrideEqualsOrHashCode() throws Exception {
        final Class<? extends Element> elementImplClass = XmlUtil.stringAsDocument("<foo/>").getDocumentElement().getClass();
        assertFalse(MethodUtil.isEqualsOrHashCodeOverridden(elementImplClass));
    }

    public static final String XML_WITH_LEADING_WHITESPACE =
            "\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:ThroughputQuota>\n" +
            "        <L7p:CounterName stringValue=\"asdfasdf\"/>\n" +
            "    </L7p:ThroughputQuota>\n" +
            "</wsp:Policy>";

    public static final String DOC_WITH_SEC_HEADERS = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                                  "    <s:Header>\n" +
                                                  "        <sec1:Security xmlns:sec1=\"http://schemas.xmlsoap.org/ws/2002/xx/secext\"/>\n" +
                                                  "        <sec2:Security xmlns:sec2=\"http://schemas.xmlsoap.org/ws/2002/12/secext\"><t:testEl xmlns:t=\"urn:testblah\"/></sec2:Security>\n" +
                                                  "        <sec3:Security xmlns:sec3=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"/>\n" +
                                                  "        <sec4:Security xmlns:sec4=\"http://docs.oasis-open.org/asdfhalsfhasldkhf\"/>\n" +
                                                  "    </s:Header>\n" +
                                                  "    <s:Body/>\n" +
                                                  "</s:Envelope>";

    public static final String DOC_WITH_SEC_HEADERS_STRIPPED = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                                  "<s:Header>" +
                                                  "<sec1:Security xmlns:sec1=\"http://schemas.xmlsoap.org/ws/2002/xx/secext\"/>" +
                                                  "<sec2:Security xmlns:sec2=\"http://schemas.xmlsoap.org/ws/2002/12/secext\"><t:testEl xmlns:t=\"urn:testblah\"/></sec2:Security>" +
                                                  "<sec3:Security xmlns:sec3=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"/>" +
                                                  "<sec4:Security xmlns:sec4=\"http://docs.oasis-open.org/asdfhalsfhasldkhf\"/>" +
                                                  "</s:Header>" +
                                                  "<s:Body/>" +
                                                  "</s:Envelope>";

    public static final String BUG_7685_DOCTYPE_INFINITE_LOOP_SPLOIT_BASE64 =
            "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPCFET0NUWVBFIEVudmVsb3Bl\n" +
            "IFNZU1RFTSAid2FhaO+/vi5kdGQiPiAgICAgICAgCjxzb2FwOkVudmVsb3BlCXhtbG5zOnNvYXA9\n" +
            "Imh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3NvYXAvZW52ZWxvcGUvIiB4bWxuczp4c2k9Imh0\n" +
            "dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIiB4bWxuczp4c2Q9Imh0dHA6\n" +
            "Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiB4bWxuczp3c2E9Imh0dHA6Ly9zY2hlbWFzLnht\n" +
            "bHNvYXAub3JnL3dzLzIwMDQvMDMvYWRkcmVzc2luZyIgeG1sbnM6d3NzZT0iaHR0cDovL2RvY3Mu\n" +
            "b2FzaXMtb3Blbi5vcmcvd3NzLzIwMDQvMDEvb2FzaXMtMjAwNDAxLXdzcy13c3NlY3VyaXR5LXNl\n" +
            "Y2V4dC0xLjAueHNkIiB4bWxuczp3c3U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8y\n" +
            "MDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS11dGlsaXR5LTEuMC54c2QiPgoJPHNv\n" +
            "YXA6SGVhZGVyPgoJCTx3c2E6QWN0aW9uPmh0dHA6Ly93YXJlaG91c2UuYWNtZS5jb20vd3MvbGlz\n" +
            "dFByb2R1Y3RzPC93c2E6QWN0aW9uPgoJCTx3c2E6TWVzc2FnZUlEPnV1aWQ6NDBhMWRiMjYtMjY5\n" +
            "Ny00Nzk4LWEyYjctODAzYzczYjA3YjJmPC93c2E6TWVzc2FnZUlEPgoJCTx3c2E6UmVwbHlUbz4K\n" +
            "CQkJPHdzYTpBZGRyZXNzPmh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDQvMDMvYWRk\n" +
            "cmVzc2luZy9yb2xlL2Fub255bW91czwvd3NhOkFkZHJlc3M+CgkJPC93c2E6UmVwbHlUbz4KCQk8\n" +
            "d3NhOlRvPmh0dHA6Ly9yaWtlcjo4ODg4L0FDTUVXYXJlaG91c2VXUy9TZXJ2aWNlMS5hc214PC93\n" +
            "c2E6VG8+CgkJPHdzc2U6U2VjdXJpdHkgc29hcDptdXN0VW5kZXJzdGFuZD0iMSI+CgkJCTx3c3U6\n" +
            "VGltZXN0YW1wIHdzdTpJZD0iVGltZXN0YW1wLWRmMDliYjNkLWQ2MDUtNDg3MC1iN2E1LWNlYjFm\n" +
            "YmNkNDdiYiI+CgkJCQk8d3N1OkNyZWF0ZWQ+MjAwNC0wNi0xNFQxODo0OTowNVo8L3dzdTpDcmVh\n" +
            "dGVkPgoJCQkJPHdzdTpFeHBpcmVzPjIwMDQtMDYtMTRUMTg6NTQ6MDVaPC93c3U6RXhwaXJlcz4K\n" +
            "CQkJPC93c3U6VGltZXN0YW1wPgoJCQk8d3NzZTpVc2VybmFtZVRva2VuIHhtbG5zOndzdT0iaHR0\n" +
            "cDovL2RvY3Mub2FzaXMtb3Blbi5vcmcvd3NzLzIwMDQvMDEvb2FzaXMtMjAwNDAxLXdzcy13c3Nl\n" +
            "Y3VyaXR5LXV0aWxpdHktMS4wLnhzZCIgd3N1OklkPSJTZWN1cml0eVRva2VuLTAzYWRjN2YzLTE2\n" +
            "NWYtNGFmYy04YzI5LWRhM2M2YTNmODViZiI+CgkJCQk8d3NzZTpVc2VybmFtZT51c2VybmFtZTwv\n" +
            "d3NzZTpVc2VybmFtZT4KCQkJCTx3c3NlOlBhc3N3b3JkIFR5cGU9Imh0dHA6Ly9kb2NzLm9hc2lz\n" +
            "LW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3MtdXNlcm5hbWUtdG9rZW4tcHJv\n" +
            "ZmlsZS0xLjAjUGFzc3dvcmRUZXh0Ij5wYXNzd29yZDwvd3NzZTpQYXNzd29yZD4KCQkJCTx3c3Nl\n" +
            "Ok5vbmNlPlJIaWtRcmFlTjd3QzJzYzk4aDZVY1E9PTwvd3NzZTpOb25jZT4KCQkJCTx3c3U6Q3Jl\n" +
            "YXRlZD4yMDA0LTA2LTE0VDE4OjQ5OjA1Wjwvd3N1OkNyZWF0ZWQ+CgkJCTwvd3NzZTpVc2VybmFt\n" +
            "ZVRva2VuPgoJCTwvd3NzZTpTZWN1cml0eT4KCTwvc29hcDpIZWFkZXI+Cgk8c29hcDpCb2R5IHdz\n" +
            "dTpJZD0iSWQtM2E4NTM5NGItZTZkYy00NjlkLWFhNWEtZGJhNGQ3ODdjMDM1Ij4KCQk8bGlzdFBy\n" +
            "b2R1Y3RzIHhtbG5zPSJodHRwOi8vd2FyZWhvdXNlLmFjbWUuY29tL3dzIiAvPgoJPC9zb2FwOkJv\n" +
            "ZHk+Cjwvc29hcDpFbnZlbG9wZT4=";
}