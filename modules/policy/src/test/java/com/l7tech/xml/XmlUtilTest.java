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
import org.junit.*;
import static org.junit.Assert.*;
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
            "IFNZU1RFTSAid2FhaPSAgJMuZHRkIj4gICAgICAgIAo8c29hcDpFbnZlbG9wZQl4bWxuczpzb2Fw\n" +
            "PSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyIgeG1sbnM6eHNpPSJo\n" +
            "dHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSIgeG1sbnM6eHNkPSJodHRw\n" +
            "Oi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54\n" +
            "bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2Nz\n" +
            "Lm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1z\n" +
            "ZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3Mv\n" +
            "MjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIj4KCTxz\n" +
            "b2FwOkhlYWRlcj4KCQk8d3NhOkFjdGlvbj5odHRwOi8vd2FyZWhvdXNlLmFjbWUuY29tL3dzL2xp\n" +
            "c3RQcm9kdWN0czwvd3NhOkFjdGlvbj4KCQk8d3NhOk1lc3NhZ2VJRD51dWlkOjQwYTFkYjI2LTI2\n" +
            "OTctNDc5OC1hMmI3LTgwM2M3M2IwN2IyZjwvd3NhOk1lc3NhZ2VJRD4KCQk8d3NhOlJlcGx5VG8+\n" +
            "CgkJCTx3c2E6QWRkcmVzcz5odHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2Fk\n" +
            "ZHJlc3Npbmcvcm9sZS9hbm9ueW1vdXM8L3dzYTpBZGRyZXNzPgoJCTwvd3NhOlJlcGx5VG8+CgkJ\n" +
            "PHdzYTpUbz5odHRwOi8vcmlrZXI6ODg4OC9BQ01FV2FyZWhvdXNlV1MvU2VydmljZTEuYXNteDwv\n" +
            "d3NhOlRvPgoJCTx3c3NlOlNlY3VyaXR5IHNvYXA6bXVzdFVuZGVyc3RhbmQ9IjEiPgoJCQk8d3N1\n" +
            "OlRpbWVzdGFtcCB3c3U6SWQ9IlRpbWVzdGFtcC1kZjA5YmIzZC1kNjA1LTQ4NzAtYjdhNS1jZWIx\n" +
            "ZmJjZDQ3YmIiPgoJCQkJPHdzdTpDcmVhdGVkPjIwMDQtMDYtMTRUMTg6NDk6MDVaPC93c3U6Q3Jl\n" +
            "YXRlZD4KCQkJCTx3c3U6RXhwaXJlcz4yMDA0LTA2LTE0VDE4OjU0OjA1Wjwvd3N1OkV4cGlyZXM+\n" +
            "CgkJCTwvd3N1OlRpbWVzdGFtcD4KCQkJPHdzc2U6VXNlcm5hbWVUb2tlbiB4bWxuczp3c3U9Imh0\n" +
            "dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3Nz\n" +
            "ZWN1cml0eS11dGlsaXR5LTEuMC54c2QiIHdzdTpJZD0iU2VjdXJpdHlUb2tlbi0wM2FkYzdmMy0x\n" +
            "NjVmLTRhZmMtOGMyOS1kYTNjNmEzZjg1YmYiPgoJCQkJPHdzc2U6VXNlcm5hbWU+dXNlcm5hbWU8\n" +
            "L3dzc2U6VXNlcm5hbWU+CgkJCQk8d3NzZTpQYXNzd29yZCBUeXBlPSJodHRwOi8vZG9jcy5vYXNp\n" +
            "cy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXVzZXJuYW1lLXRva2VuLXBy\n" +
            "b2ZpbGUtMS4wI1Bhc3N3b3JkVGV4dCI+cGFzc3dvcmQ8L3dzc2U6UGFzc3dvcmQ+CgkJCQk8d3Nz\n" +
            "ZTpOb25jZT5SSGlrUXJhZU43d0Myc2M5OGg2VWNRPT08L3dzc2U6Tm9uY2U+CgkJCQk8d3N1OkNy\n" +
            "ZWF0ZWQ+MjAwNC0wNi0xNFQxODo0OTowNVo8L3dzdTpDcmVhdGVkPgoJCQk8L3dzc2U6VXNlcm5h\n" +
            "bWVUb2tlbj4KCQk8L3dzc2U6U2VjdXJpdHk+Cgk8L3NvYXA6SGVhZGVyPgoJPHNvYXA6Qm9keSB3\n" +
            "c3U6SWQ9IklkLTNhODUzOTRiLWU2ZGMtNDY5ZC1hYTVhLWRiYTRkNzg3YzAzNSI+CgkJPGxpc3RQ\n" +
            "cm9kdWN0cyB4bWxucz0iaHR0cDovL3dhcmVob3VzZS5hY21lLmNvbS93cyIgLz4KCTwvc29hcDpC\n" +
            "b2R5Pgo8L3NvYXA6RW52ZWxvcGU+";
}