package com.l7tech.common.message;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeBodyTest;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class KnobblyMessageTest extends TestCase {
    private static final Logger logger = Logger.getLogger(KnobblyMessageTest.class.getName());

    /**
     * test <code>KnobblyMessageTest</code> constructor
     */
    public KnobblyMessageTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * KnobblyMessageTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(KnobblyMessageTest.class);
        return suite;
    }

    public void testFacetlessMessage() {
        Message msg = new Message();
        assertNull(msg.getKnob(MimeKnob.class));
        assertNull(msg.getKnob(XmlKnob.class));
        assertNull(msg.getKnob(SoapKnob.class));

        try {
            msg.getMimeKnob();
            fail();
        } catch (IllegalStateException e) {
            // ok
        }
    }

    public void testGetMimeKnobSinglepart() throws Exception {
        Message msg = new Message();
        final String bodyString = "blah \u9281 blah \000 blah";
        msg.initialize(new ByteArrayStashManager(),
                              ContentTypeHeader.OCTET_STREAM_DEFAULT,
                              new ByteArrayInputStream(bodyString.getBytes(ContentTypeHeader.OCTET_STREAM_DEFAULT.getEncoding())));
        byte[] got = HexUtils.slurpStream(msg.getMimeKnob().getFirstPart().getInputStream(true));
        assertTrue(Arrays.equals(got, bodyString.getBytes(ContentTypeHeader.OCTET_STREAM_DEFAULT.getEncoding())));

        assertNotNull(msg.getKnob(MimeKnob.class));
        assertNull(msg.getKnob(XmlKnob.class));
        assertNull(msg.getKnob(SoapKnob.class));

        try {
            msg.getXmlKnob();
            fail();
        } catch (SAXException e) {
            // ok
        }

        try {
            msg.getSoapKnob();
            fail();
        } catch (MessageNotSoapException e) {
            // ok
        } catch (SAXException e) {
            // ok
        }
    }

    public void testGetMimePartMultipart() throws Exception {
        Message msg = new Message(new ByteArrayStashManager(),
                                  ContentTypeHeader.parseValue(MimeBodyTest.MESS_CONTENT_TYPE),
                                  new ByteArrayInputStream(MimeBodyTest.MESS.getBytes()));

        // Consume the first part
        byte[] got = HexUtils.slurpStream(msg.getMimeKnob().getFirstPart().getInputStream(true));
        assertTrue(Arrays.equals(got, MimeBodyTest.SOAP.getBytes("UTF-8")));

        // XML should fail (part consumed)
        try {
            // getKnob should succeed...
            XmlKnob xmlKnob = msg.getXmlKnob();

            // ...but getDocumentReadOnly should fail
            Document d = xmlKnob.getDocumentReadOnly();
            fail("Failed to get expected exception.  d=" + d);
        } catch (SAXException e) {
            logger.info("Got expected exception: " + e);
        }

        // SOAP should fail (same)
        try {
            // Should fail right away when looking for soap envelope
            SoapKnob soapKnob = msg.getSoapKnob();
            fail("Failed to get expected exception.  soapKnob=" + soapKnob);
        } catch (SAXException e) {
            logger.info("Got expected exception: " + e);
        }

        // Reinstall first part
        msg.getMimeKnob().getFirstPart().setBodyBytes(got);

        // now SOAP should succeed
        assertEquals(MimeBodyTest.MESS_PAYLOAD_NS, msg.getSoapKnob().getPayloadNamespaceUri());
    }

    public void testGetXmlKnob() throws Exception {
        Message msg = new Message(new ByteArrayStashManager(),
                                  ContentTypeHeader.XML_DEFAULT,
                                  new ByteArrayInputStream("<getquote>MSFT</getquote>".getBytes()));
        logger.info(XmlUtil.nodeToString(msg.getXmlKnob().getDocumentReadOnly()));
        assertNotNull(msg.getMimeKnob());

        // Soap knob has not been asked for yet
        assertNull(msg.getKnob(SoapKnob.class));

        // Lazy instantiation of soap knob should fail on non-soap message
        try {
            msg.getSoapKnob();
            fail("Expected exception was not thrown");
        } catch (MessageNotSoapException e) {
            logger.info("Expected exception was thrown: " + e);
        }
    }

    public void testCombinedDomAndByteOperations() throws Exception {
        final String xml = "<foobarbaz/>";
        Message msg = new Message(XmlUtil.stringToDocument(xml));

        // Get read-only doc
        Document docro = msg.getXmlKnob().getDocumentReadOnly();
        NodeList rofoos = docro.getElementsByTagName("foobarbaz");
        assertTrue(rofoos.getLength() > 0);

        // Upgrade to writable
        Document docrw = msg.getXmlKnob().getDocumentWritable();
        assertTrue(docrw == docro);

        // Change document
        docro.getDocumentElement().appendChild(XmlUtil.createTextNode(docro, "Blah!"));
        String curDoc = XmlUtil.nodeToString(docro);
        final String xmlWithBlah = "<foobarbaz>Blah!</foobarbaz>";
        assertEquals(curDoc, xmlWithBlah);

        // Make sure change is reflected in the bytes message
        PartInfo fp1 = msg.getMimeKnob().getFirstPart();
        byte[] bb1a = HexUtils.slurpStream(fp1.getInputStream(false));
        assertEquals(new String(bb1a), xmlWithBlah);

        // Change the bytes message
        final String xmlFooBar = "<foo><bar/></foo>";
        fp1.setBodyBytes(xmlFooBar.getBytes());

        // Make sure change is reflected in the document
        Document docFooBar = msg.getXmlKnob().getDocumentReadOnly();
        String xmlDocFooBar = XmlUtil.nodeToString(docFooBar);
        byte[] bb1b = HexUtils.slurpStream(msg.getMimeKnob().getParts().next().getInputStream(false));
        assertEquals(new String(bb1b), xmlDocFooBar);
    }

    public void testGetSoapKnob() throws Exception {
        Message msg = new Message();
        assertNull(msg.getKnob(MimeKnob.class));
        assertNull(msg.getKnob(XmlKnob.class));
        assertNull(msg.getKnob(SoapKnob.class));

        try {
            msg.getMimeKnob();
            fail();
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            msg.getXmlKnob();
            fail();
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            msg.getSoapKnob();
            fail();
        } catch (MessageNotSoapException e) {
            // ok
        } catch (IllegalStateException e) {
            // ok
        }

        GlobalTarariContext context = TarariLoader.getGlobalContext();
        if (context != null) {
            logger.info("Initializing XML Hardware Acceleration");
            context.compile();
        }
        msg.initialize(new ByteArrayStashManager(),
                              ContentTypeHeader.XML_DEFAULT,
                              TestDocuments.getInputStream(TestDocuments.PLACEORDER_CLEARTEXT));
        SoapKnob soapKnob = msg.getSoapKnob();
        if (msg.isSoap()) {
            String uri = soapKnob.getPayloadNamespaceUri();
            boolean sec = soapKnob.isSecurityHeaderPresent();
            logger.info("SOAP payload namespace URI = " + uri);
            logger.info("Security header " + (sec ? "" : "not ") + "found");
        }
        logger.info(soapKnob.getPayloadNamespaceUri());
        assertNotNull(msg.getXmlKnob());
        assertNotNull(msg.getMimeKnob());
    }

    /**
     * Test <code>KnobblyMessageTest</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }
}