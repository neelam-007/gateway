package com.l7tech.common.util;

import com.l7tech.common.xml.TestDocuments;
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
        return new TestDocuments().getTestDocument( TestDocuments.PLACEORDER_WITH_MAJESTY );

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

        Element security = XmlUtil.findOnlyOneChildElementByName( header, SoapUtil.SECURITY_NAMESPACE, SoapUtil.SECURITY_EL_NAME );
        assertNotNull(security);

        try {
            Element firstSignature = XmlUtil.findOnlyOneChildElementByName( security,
                                                                          SoapUtil.DIGSIG_URI,
                                                                          SoapUtil.SIGNATURE_EL_NAME );
            fail("Expected exception not thrown");
        } catch ( XmlUtil.MultipleChildElementsException e ) {
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
        assertTrue( XmlUtil.elementToString( firstSignature ).indexOf( "#signref1" ) >= 0 );
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
            assertTrue( XmlUtil.elementToString( child ).indexOf( "#signref" + (i+1) ) >= 0 );
        }

        Element sctoken = XmlUtil.findFirstChildElement( security );
        // Make sure it doesn't find spurious children
        List none = XmlUtil.findChildElementsByName( sctoken, SoapUtil.XMLENC_NS, "Foo" );
        assertTrue(none.isEmpty());
    }
}