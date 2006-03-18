package com.l7tech.common.util;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.xml.*;
import com.l7tech.common.xml.xpath.DomCompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.XpathResultNodeSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author alex
 * @version $Revision$
 */
public class XmlUtilTest extends TestCase {
    private static final Logger logger = Logger.getLogger(XmlUtilTest.class.getName());
    private static final String PI_XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<?xml-stylesheet href=\"foo\" type=\"text/xsl\"?>\n<foo/>";

    /**
     * test <code>XmlUtilTest</code> constructor
     */
    public XmlUtilTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the XmlUtilTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( XmlUtilTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>XmlUtilTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }

    private static Document getTestDocument() throws Exception {
        return TestDocuments.getTestDocument( TestDocuments.PLACEORDER_WITH_MAJESTY );

    }

    public void testFindAllNamespaces() throws Exception {
        Element el = getTestDocument().getDocumentElement();
        Map foo = XmlUtil.findAllNamespaces(el);
        logger.info("Found namespaces: " + foo);
    }

    private void assertElementEquals( Element element, String nsuri, String name ) {
        assertTrue(element != null);
        assertTrue(nsuri.equals(element.getNamespaceURI()));
        assertTrue(name.equals(element.getLocalName()));
    }

    public void testFindFirstChildElement() throws Exception {
        Document d = getTestDocument();
        Element header = XmlUtil.findFirstChildElement( d.getDocumentElement() );
        assertElementEquals( header, SOAPConstants.URI_NS_SOAP_ENVELOPE, SoapUtil.HEADER_EL_NAME );
        Element security = XmlUtil.findFirstChildElement( header );
        Element sctoken = XmlUtil.findFirstChildElement( security );
        Element ident = XmlUtil.findFirstChildElement( sctoken );
        Element none = XmlUtil.findFirstChildElement( ident );
        assertNull( none );
    }

    public void testFindOnlyOne() throws Exception {
        Document d = getTestDocument();
        Element header = XmlUtil.findOnlyOneChildElementByName( d.getDocumentElement(),
                                                              SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                              SoapUtil.HEADER_EL_NAME );
        assertNotNull(header);

        Element security = XmlUtil.findOnlyOneChildElementByName( header, SoapUtil.SECURITY_URIS_ARRAY, SoapUtil.SECURITY_EL_NAME );
        assertNotNull(security);

        try {
            Element firstSignature = XmlUtil.findOnlyOneChildElementByName( security,
                                                                          SoapUtil.DIGSIG_URI,
                                                                          SoapUtil.SIGNATURE_EL_NAME );
            fail("Expected exception not thrown");
        } catch ( TooManyChildElementsException e ) {
            // Expected
        }

        Element nil = XmlUtil.findOnlyOneChildElementByName( security, "Foo", "Bar" );
        assertNull(nil);

        Element sec2 = XmlUtil.findOnlyOneChildElement(header);
        assertEquals(security, sec2);
    }


    public void testFindFirstChildElementByName() throws Exception {
        Document d = getTestDocument();
        Element header = XmlUtil.findFirstChildElementByName( d.getDocumentElement(),
                                                              SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                              SoapUtil.HEADER_EL_NAME );
        assertElementEquals( header, SOAPConstants.URI_NS_SOAP_ENVELOPE, SoapUtil.HEADER_EL_NAME );
        Element security = XmlUtil.findFirstChildElement( header );
        Element firstSignature = XmlUtil.findFirstChildElementByName( security,
                                                                      SoapUtil.DIGSIG_URI,
                                                                      SoapUtil.SIGNATURE_EL_NAME );
        // Make sure it's really the first one
        assertTrue( XmlUtil.nodeToString( firstSignature ).indexOf( "#signref1" ) >= 0 );
        assertElementEquals( firstSignature, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME );

        Element sctoken = XmlUtil.findFirstChildElement( security );
        // Make sure it doesn't find spurious children
        Element none = XmlUtil.findFirstChildElementByName( sctoken, SoapUtil.XMLENC_NS, "Foo" );
        assertNull(none);
    }

    public void testFindChildElementsByName() throws Exception {
        Document d = getTestDocument();
        List children = XmlUtil.findChildElementsByName( d.getDocumentElement(),
                                                              SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                              SoapUtil.HEADER_EL_NAME );
        assertTrue( children.size() > 0 );
        Element header = (Element)children.get(0);
        assertElementEquals( header, SOAPConstants.URI_NS_SOAP_ENVELOPE, SoapUtil.HEADER_EL_NAME );
        Element security = XmlUtil.findFirstChildElement( header );
        List signatures = XmlUtil.findChildElementsByName( security, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME);

        assertTrue( signatures.size() == 3 );
        for ( int i = 0; i < signatures.size(); i++ ) {
            Element child  = (Element)signatures.get(i);
            assertElementEquals( child, SoapUtil.DIGSIG_URI, SoapUtil.SIGNATURE_EL_NAME );
            assertTrue( XmlUtil.nodeToString( child ).indexOf( "#signref" + (i+1) ) >= 0 );
        }

        Element sctoken = XmlUtil.findFirstChildElement( security );
        // Make sure it doesn't find spurious children
        List none = XmlUtil.findChildElementsByName( sctoken, SoapUtil.XMLENC_NS, "Foo" );
        assertTrue(none.isEmpty());
    }

    public void testFindChildElementsByNameWithNSArray() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        Element env = d.getDocumentElement();
        Element header = XmlUtil.findFirstChildElement(env);
        List children = XmlUtil.findChildElementsByName(header, SoapUtil.SECURITY_URIS_ARRAY, SoapUtil.SECURITY_EL_NAME);
        assertTrue(children.size() == 3);
        Element sec1 = (Element)children.get(0);
        Element sec2 = (Element)children.get(1);
        Element sec3 = (Element)children.get(2);
        assertTrue(SoapUtil.SECURITY_NAMESPACE4.equals(sec1.getNamespaceURI()));
        assertTrue(SoapUtil.SECURITY_NAMESPACE2.equals(sec2.getNamespaceURI()));
        assertTrue(SoapUtil.SECURITY_NAMESPACE.equals(sec3.getNamespaceURI()));
    }

    public void testGetPayloadNamespaceUri() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        assertNull(SoapUtil.getPayloadNamespaceUris(d));

        d = TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST);
        assertEquals("http://warehouse.acme.com/ws", SoapUtil.getPayloadNamespaceUris(d)[0]);
    }

    public void testFindActivePrefixForNamespace() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);

        Element env = d.getDocumentElement();
        Element header = XmlUtil.findFirstChildElement(env);
        Element sec1 = XmlUtil.findFirstChildElement(header);

        String prefix = XmlUtil.findActivePrefixForNamespace(sec1, SoapUtil.SECURITY_NAMESPACE4);
        assertEquals(prefix, "sec1");

        prefix = XmlUtil.findActivePrefixForNamespace(sec1, "http://schemas.xmlsoap.org/soap/envelope/");
        assertEquals(prefix, "s");

        prefix = XmlUtil.findActivePrefixForNamespace(sec1, "http://blah.bletch");
        assertNull(prefix);

        prefix = XmlUtil.findActivePrefixForNamespace(header, SoapUtil.SECURITY_NAMESPACE4);
        assertNull(prefix);

    }

    public void testParseEmptyString() {
        try {
            Document d = XmlUtil.stringToDocument("");
            fail("Expected exception not thrown.  Returned d=" + d);
        } catch (Exception e) {
            // Ok
            logger.log(Level.INFO, "The expected exception was thrown: " + e.getMessage());
        }
    }

    public void testParseNull() {
        try {
            Document d = XmlUtil.stringToDocument(null);
            fail("Expected exception not thrown.  Returned d=" + d);
        } catch (Exception e) {
            // Ok
            logger.log(Level.INFO, "The expected exception was thrown: " + e.getMessage());
        }
    }

    public void testGetNamespaceMap() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        Element header = XmlUtil.findFirstChildElement(d.getDocumentElement());
        assertNotNull(header);
        Element sec2 = XmlUtil.findFirstChildElementByName(header, "http://schemas.xmlsoap.org/ws/2002/12/secext", "Security");
        assertNotNull(sec2);
        Element t = XmlUtil.findFirstChildElement(sec2);
        assertNotNull(t);
        Map nsmap = XmlUtil.getNamespaceMap(t);
        assertEquals(nsmap.size(), 3);
        assertTrue(nsmap.containsKey("s"));
        assertTrue(nsmap.containsValue("urn:testblah"));
        assertEquals(nsmap.get("sec2"), "http://schemas.xmlsoap.org/ws/2002/12/secext");
    }

    public void testStripWhitespace() throws Exception {
        Document d = XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS);
        XmlUtil.stripWhitespace(d.getDocumentElement());
        String stripped = XmlUtil.nodeToString(d);
        String wantstripped = XmlUtil.nodeToString(XmlUtil.stringToDocument(DOC_WITH_SEC_HEADERS_STRIPPED));
        assertEquals(wantstripped, stripped);
    }

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
}