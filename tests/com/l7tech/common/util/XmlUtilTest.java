package com.l7tech.common.util;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.TooManyChildElementsException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPConstants;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class XmlUtilTest extends TestCase {
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

    public static final String DOC_WITH_SEC_HEADERS = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                                  "    <s:Header>\n" +
                                                  "        <sec1:Security xmlns:sec1=\"http://schemas.xmlsoap.org/ws/2002/xx/secext\"/>\n" +
                                                  "        <sec2:Security xmlns:sec2=\"http://schemas.xmlsoap.org/ws/2002/12/secext\"/>\n" +
                                                  "        <sec3:Security xmlns:sec3=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"/>\n" +
                                                  "        <sec4:Security xmlns:sec4=\"http://docs.oasis-open.org/asdfhalsfhasldkhf\"/>\n" +
                                                  "    </s:Header>\n" +
                                                  "    <s:Body/>\n" +
                                                  "</s:Envelope>";
}