package com.l7tech.message;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.DomUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.beans.EventHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author alex
 * @version $Revision$
 */
public class KnobblyMessageTest {
    private static final Logger logger = Logger.getLogger(KnobblyMessageTest.class.getName());

    @Test
    public void testFacetlessMessage() throws IOException {
        Message msg = new Message();
        assertNull(msg.getKnob( MimeKnob.class));
        assertNull(msg.getKnob( XmlKnob.class));
        assertNull(msg.getKnob( SoapKnob.class));
        assertFalse(msg.isInitialized());
        MimeKnob mk = msg.getMimeKnob();
        assertFalse(mk.isMultipart());
        assertTrue(0 == mk.getContentLength());
        assertEquals(ContentTypeHeader.OCTET_STREAM_DEFAULT.getFullValue(), mk.getOuterContentType().getFullValue());
        assertFalse("Lazily-added 'empty' MimeKnob doesn't count as proper initialization", msg.isInitialized());
    }

    @Test
    public void testGetMimeKnobSinglepart() throws Exception {
        Message msg = new Message();
        final String bodyString = "blah \u9281 blah \000 blah";
        msg.initialize(new ByteArrayStashManager(),
                              ContentTypeHeader.OCTET_STREAM_DEFAULT,
                              new ByteArrayInputStream(bodyString.getBytes(ContentTypeHeader.OCTET_STREAM_DEFAULT.getEncoding())));
        byte[] got = IOUtils.slurpStream(msg.getMimeKnob().getFirstPart().getInputStream(true));
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

    @Test
    public void testGetMimePartMultipart() throws Exception {
        Message msg = new Message(new ByteArrayStashManager(),
                                  ContentTypeHeader.parseValue(MESS_CONTENT_TYPE),
                                  new ByteArrayInputStream(MESS.getBytes()));

        // Consume the first part
        byte[] got = IOUtils.slurpStream(msg.getMimeKnob().getFirstPart().getInputStream(true));
        assertTrue(Arrays.equals(got, SOAP.getBytes("UTF-8")));

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
        assertEquals(MESS_PAYLOAD_NS, msg.getSoapKnob().getPayloadNames()[0].getNamespaceURI());
    }

    @Test
    public void testGetXmlKnob() throws Exception {
        Message msg = new Message(new ByteArrayStashManager(),
                                  ContentTypeHeader.XML_DEFAULT,
                                  new ByteArrayInputStream("<getquote>MSFT</getquote>".getBytes()));
        logger.info( XmlUtil.nodeToString(msg.getXmlKnob().getDocumentReadOnly()));
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


    @Test
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
        docro.getDocumentElement().appendChild(DomUtils.createTextNode(docro, "Blah!"));
        String curDoc = XmlUtil.nodeToString(docro);
        final String xmlWithBlah = "<foobarbaz>Blah!</foobarbaz>";
        assertEquals(curDoc, xmlWithBlah);

        // Make sure change is reflected in the bytes message
        PartInfo fp1 = msg.getMimeKnob().getFirstPart();
        byte[] bb1a = IOUtils.slurpStream(fp1.getInputStream(false));
        assertEquals(new String(bb1a), xmlWithBlah);

        // Change the bytes message
        final String xmlFooBar = "<foo><bar/></foo>";
        fp1.setBodyBytes(xmlFooBar.getBytes());

        // Make sure change is reflected in the document
        Document docFooBar = msg.getXmlKnob().getDocumentReadOnly();
        String xmlDocFooBar = XmlUtil.nodeToString(docFooBar);
        byte[] bb1b = IOUtils.slurpStream(msg.getMimeKnob().getParts().next().getInputStream(false));
        assertEquals(new String(bb1b), xmlDocFooBar);
    }

    @Test
    public void testModifiedDomWithIteratedPartInfoStream() throws Exception {
        Message msg = new Message();
        msg.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, TestDocuments.getTestDocumentURL(TestDocuments.PLACEORDER_CLEARTEXT).openStream());

        // Mutate the document
        Document doc = msg.getXmlKnob().getDocumentWritable();
        String tag = "blahMutant";
        SoapUtil.getPayloadElement(doc).appendChild(doc.createElement(tag));
        String mutatedStr = XmlUtil.nodeToString(doc);

        // Stream it out
        byte[] bytes = IOUtils.slurpStream(msg.getMimeKnob().getParts().next().getInputStream(false));
        String streamedStr = new String(bytes, "UTF-8");

        // Make sure they stayed in sync
        assertEquals(mutatedStr, streamedStr);
    }

    @Test
    public void testModifiedDomWithPartInfoStream() throws Exception {
        Message msg = new Message();
        msg.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, TestDocuments.getTestDocumentURL(TestDocuments.PLACEORDER_CLEARTEXT).openStream());

        // Mutate the document
        Document doc = msg.getXmlKnob().getDocumentWritable();
        String tag = "blahMutant";
        SoapUtil.getPayloadElement(doc).appendChild(doc.createElement(tag));
        String mutatedStr = XmlUtil.nodeToString(doc);

        // Stream it out
        byte[] bytes = IOUtils.slurpStream(msg.getMimeKnob().getPart(0).getInputStream(false));
        String streamedStr = new String(bytes, "UTF-8");

        // Make sure they stayed in sync
        assertEquals(mutatedStr, streamedStr);
    }

    @Test
    public void testDomCommitDelayedUntilPartInfoBytesUsed() throws Exception {
        Message msg = new Message();
        msg.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, TestDocuments.getTestDocumentURL(TestDocuments.PLACEORDER_CLEARTEXT).openStream());

        // Mutate the document
        Document doc = msg.getXmlKnob().getDocumentWritable();
        SoapUtil.getPayloadElement(doc).appendChild(doc.createElement("blahMutant1"));

        // Iterate the parts but do not access their bytes.  This should not invalidate our DOM.
        for (PartInfo partInfo : msg.getMimeKnob()) {
            partInfo.getContentType();
            partInfo.getContentId(true);
            partInfo.getHeaders().size();
        }

        // Now mutate the document some more
        SoapUtil.getPayloadElement(doc).appendChild(doc.createElement("blahMutant2"));
        String mutatedStr = XmlUtil.nodeToString(doc);

        // Stream it back out
        String streamedStr = new String(IOUtils.slurpStream(msg.getMimeKnob().getEntireMessageBodyAsInputStream()), "UTF-8");

        // Iterating the parts should not have revoked the DOM
        assertEquals(mutatedStr, streamedStr);

        // Now mutate the document some more still
        doc = msg.getXmlKnob().getDocumentWritable();
        SoapUtil.getPayloadElement(doc).appendChild(doc.createElement("blahMutant3"));

        // Iterate the parts but peek at their bytes.  This should invalidate the dom
        for (PartInfo partInfo : msg.getMimeKnob())
            partInfo.getInputStream(false).close();

        // Now mutate the document some more
        SoapUtil.getPayloadElement(doc).appendChild(doc.createElement("blahMutant4"));
        mutatedStr = XmlUtil.nodeToString(doc);

        // Stream it back out
        streamedStr = new String(IOUtils.slurpStream(msg.getMimeKnob().getEntireMessageBodyAsInputStream()), "UTF-8");

        assertFalse(mutatedStr.equalsIgnoreCase(streamedStr));
    }

    @Test
    public void testGetSoapKnob() throws Exception {
        Message msg = new Message();
        assertNull(msg.getKnob(MimeKnob.class));
        assertNull(msg.getKnob(XmlKnob.class));
        assertNull(msg.getKnob(SoapKnob.class));
        assertFalse(msg.isInitialized());

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
        }

        assertFalse(msg.isInitialized());
        msg.initialize(new ByteArrayStashManager(),
                              ContentTypeHeader.XML_DEFAULT,
                              TestDocuments.getInputStream(TestDocuments.PLACEORDER_CLEARTEXT));
        assertTrue(msg.isInitialized());
        SoapKnob soapKnob = msg.getSoapKnob();
        if (msg.isSoap()) {
            String uri = soapKnob.getPayloadNames()[0].getNamespaceURI();
            boolean sec = soapKnob.isSecurityHeaderPresent();
            logger.info("SOAP payload namespace URI = " + uri);
            logger.info("Security header " + (sec ? "" : "not ") + "found");
        }
        logger.info(soapKnob.getPayloadNames()[0].getNamespaceURI());
        assertNotNull(msg.getXmlKnob());
        assertNotNull(msg.getMimeKnob());
    }

    @Test
    public void testHttpKnobsArePreserved() throws Exception {
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("blah blah".getBytes(Charsets.UTF8)));

        InvocationHandler fakeIh = new EventHandler(new Object(), "blah", null, null);
        HttpRequestKnob requestKnob = (HttpRequestKnob) Proxy.newProxyInstance(HttpRequestFacet.class.getClassLoader(), new Class[]{HttpRequestKnob.class}, fakeIh);
        HttpResponseKnob responseKnob = (HttpResponseKnob) Proxy.newProxyInstance(HttpRequestFacet.class.getClassLoader(), new Class[]{HttpResponseKnob.class}, fakeIh);

        mess.attachHttpRequestKnob(requestKnob);
        mess.attachHttpResponseKnob(responseKnob);

        // Reinitialize message, ensure knobs are preserved

        mess.initialize(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream("otherblah".getBytes(Charsets.UTF8)));

        assertTrue(requestKnob == mess.getKnob(HttpRequestKnob.class));
        assertTrue(responseKnob == mess.getKnob(HttpResponseKnob.class));
    }

    public interface MyNonPreservableKnob extends MessageKnob {
    }

    public interface MyPreservableKnob extends MessageKnob {
        String getPayload();
    }

    @Test
    public void testOnlyPreservableKnobsArePreserved() throws Exception {

        // Initiailize and attach facets

        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("blah blah".getBytes(Charsets.UTF8)));

        // Attach preservable
        final MyPreservableKnob origPreservableKnob = new MyPreservableKnob() {
            @Override
            public String getPayload() {
                return "payload of knob";
            }
        };
        mess.attachKnob(origPreservableKnob, true, MyPreservableKnob.class);

        // Attach non-preservable
        final MyNonPreservableKnob origNonPreservableKnob = new MyNonPreservableKnob() {};
        mess.attachKnob(origNonPreservableKnob, MyNonPreservableKnob.class);

        // Ensure expected knobs are present

        MyPreservableKnob preservableKnob = mess.getKnob(MyPreservableKnob.class);
        assertTrue(preservableKnob == origPreservableKnob);
        assertEquals("payload of knob", preservableKnob.getPayload());

        MyNonPreservableKnob nonPreservableKnob = mess.getKnob(MyNonPreservableKnob.class);
        assertTrue(nonPreservableKnob == origNonPreservableKnob);

        // Reinitialize message, ensure only preservable knob is preserved

        mess.initialize(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream("otherblah".getBytes(Charsets.UTF8)));

        preservableKnob = mess.getKnob(MyPreservableKnob.class);
        assertNotNull(preservableKnob);
        assertTrue(preservableKnob == origPreservableKnob);
        assertEquals("payload of knob", preservableKnob.getPayload());

        nonPreservableKnob = mess.getKnob(MyNonPreservableKnob.class);
        assertNull(nonPreservableKnob);
    }

    @Test
    public void testPreservableKnobsArePreservedAfterInit_ctype_bytes_int() throws Exception {

        // Initiailize and attach facets

        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("blah blah".getBytes(Charsets.UTF8)));

        // Attach preservable
        final MyPreservableKnob origPreservableKnob = new MyPreservableKnob() {
            @Override
            public String getPayload() {
                return "payload of knob";
            }
        };
        mess.attachKnob(origPreservableKnob, true, MyPreservableKnob.class);

        // Attach non-preservable
        final MyNonPreservableKnob origNonPreservableKnob = new MyNonPreservableKnob() {};
        mess.attachKnob(origNonPreservableKnob, MyNonPreservableKnob.class);

        // Ensure expected knobs are present

        MyPreservableKnob preservableKnob = mess.getKnob(MyPreservableKnob.class);
        assertTrue(preservableKnob == origPreservableKnob);
        assertEquals("payload of knob", preservableKnob.getPayload());

        MyNonPreservableKnob nonPreservableKnob = mess.getKnob(MyNonPreservableKnob.class);
        assertTrue(nonPreservableKnob == origNonPreservableKnob);

        // Reinitialize message, ensure only preservable knob is preserved

        mess.initialize(ContentTypeHeader.OCTET_STREAM_DEFAULT, "otherblah".getBytes(Charsets.UTF8), 0);

        preservableKnob = mess.getKnob(MyPreservableKnob.class);
        assertNotNull(preservableKnob);
        assertTrue(preservableKnob == origPreservableKnob);
        assertEquals("payload of knob", preservableKnob.getPayload());

        nonPreservableKnob = mess.getKnob(MyNonPreservableKnob.class);
        assertNull(nonPreservableKnob);
    }

    private static class Bug4542ReproStashManager extends ByteArrayStashManager {
        boolean closed = false;
        @Override
        public void close() {
            try {
                super.close();
            } finally {
                closed = true;
            }
        }
    }

    @Test
    @BugNumber(4542)
    public void testBug4542LeakedStashManager() throws Exception {
        Message msg = new Message();
        Bug4542ReproStashManager sm = new Bug4542ReproStashManager();
        try {
            msg.initialize(sm, ContentTypeHeader.parseValue("multipart/related; boundary=iaintsendingthis"), new IOExceptionThrowingInputStream(new IOException("nope")));
            fail("expected IOException was not thrown");
        } catch (IOException e) {
            // Ok
            assertTrue("StashManager should have been closed when MimeBody constructor failed", sm.closed);
        }
    }

    private static final String MESS_BOUNDARY = "----=Part_-763936460.407197826076299";
    private static final String MESS_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MESS_BOUNDARY+ "\"; start=\"-76394136.15558\"";
    private static final String MESS_PAYLOAD_NS = "urn:EchoAttachmentsService";
    private static final String SOAP = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "    xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <env:Body>\n" +
            "    <n1:echoOne xmlns:n1=\"" + MESS_PAYLOAD_NS + "\"\n" +
            "        env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "      <file href=\"cid:-76392836.15558\"></file>\n" +
            "    </n1:echoOne>\n" +
            "  </env:Body>\n" +
            "</env:Envelope>\n";

    private static final String RUBY = "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/ssg/soap'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            "\tfout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n";

    private static final String MESS = "PREAMBLE GARBAGE\r\nBLAH BLAH BLAH\r\n------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Content-ID: -76394136.15558\r\n" +
            "\r\n" +
            SOAP +
            "\r\n" +
            "------=Part_-763936460.407197826076299\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-ID: <-76392836.15558>\r\n" +
            "\r\n" +
             RUBY +
            "\r\n" +
            "------=Part_-763936460.407197826076299--\r\n";

    @Test
    @BugId("SSG-7354")
    public void testMessageReInitialize() throws Exception {
        final String JSON_CONTENT = "{\n" +
                "\"input\": [\n" +
                "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
                "{ \"firstName\":\"Anna\" , \"lastName\":\"Smith\" }, \n" +
                "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
                "]\n" +
                "}";
        final String XML_CONTENT = "<foobar/>";

        // empty uninitialized message
        Message message = new Message();
        assertFalse(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertSame(0, IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()).length);
        assertFalse(message.isXml());
        try {
            message.getXmlKnob();
            fail("Non XML content should throw");
        } catch (SAXException ignore) { /* expected */ }
        assertFalse(message.isSoap());
        try {
            message.getSoapKnob();
            fail("Non SOAP content should throw");
        } catch (MessageNotSoapException ignore) { /* expected */ }
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertSame(0, IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()).length);

        // reinitialize the message with byte array
        message.initialize(ContentTypeHeader.OCTET_STREAM_DEFAULT, JSON_CONTENT.getBytes("UTF-8"));
        // verify that the message is properly reinitialized
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.OCTET_STREAM_DEFAULT));
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()), JSON_CONTENT.getBytes("UTF-8")));
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }
        assertFalse(message.isSoap());
        try {
            message.getSoapKnob();
            fail("Non SOAP content should throw");
        } catch (MessageNotSoapException ignore) { /* expected */ }
        assertFalse(message.isXml());
        try {
            message.getXmlKnob();
            fail("Non XML content should throw");
        } catch (SAXException ignore) { /* expected */ }
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()), JSON_CONTENT.getBytes("UTF-8")));

        // reinitialize the message with InputStream
        message.initialize(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(XML_CONTENT.getBytes("UTF-8")));
        // verify that the message is properly reinitialized
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.OCTET_STREAM_DEFAULT));
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()), XML_CONTENT.getBytes("UTF-8")));
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }
        assertFalse(message.isSoap());
        try {
            message.getSoapKnob();
            fail("Non SOAP content should throw");
        } catch (MessageNotSoapException ignore) { /* expected */ }
        assertFalse(message.isXml());
        try {
            message.getXmlKnob();
            fail("Non XML content should throw");
        } catch (SAXException ignore) { /* expected */ }
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()), XML_CONTENT.getBytes("UTF-8")));

        // initial content to Json
        message.initialize(ContentTypeHeader.APPLICATION_JSON, JSON_CONTENT.getBytes());
        // verify that the message is properly set
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.APPLICATION_JSON));
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()), JSON_CONTENT.getBytes("UTF-8")));
        assertFalse(message.isXml());
        try {
            message.getXmlKnob();
            fail("Non XML content should throw");
        } catch (SAXException ignore) { /* expected */ }
        assertFalse(message.isSoap());
        try {
            message.getSoapKnob();
            fail("Non SOAP content should throw");
        } catch (MessageNotSoapException ignore) { /* expected */ }
        assertTrue(message.isJson());
        assertEquals(message.getJsonKnob().getJsonData().getJsonData(), JSON_CONTENT);
        assertNotNull(message.getJsonKnob().getJsonData().getJsonObject());
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertTrue(Arrays.equals(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()), JSON_CONTENT.getBytes("UTF-8")));

        // reinitialize the message to Xml
        message.initialize(XmlUtil.stringToDocument(XML_CONTENT));
        // verify that the message is properly reinitialized
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.XML_DEFAULT));
        assertTrue(XmlUtil.stringToDocument(new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()))).isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }
        assertFalse(message.isSoap());
        try {
            message.getSoapKnob();
            fail("Non SOAP content should throw");
        } catch (MessageNotSoapException ignore) { /* expected */ }
        assertTrue(message.isXml());
        assertNotNull(message.getXmlKnob().getDocumentReadOnly().isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertTrue(XmlUtil.stringToDocument(new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()))).isEqualNode(XmlUtil.stringToDocument(XML_CONTENT)));

        // reinitialize the message to Soap
        message.initialize(XmlUtil.stringToDocument(SOAP));
        // verify that the message is properly reinitialized
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.XML_DEFAULT));
        assertTrue(XmlUtil.stringToDocument(new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()))).isEqualNode(XmlUtil.stringToDocument(SOAP)));
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }
        assertTrue(message.isSoap());
        assertNotNull(message.getSoapKnob().getPayloadNames());
        assertSame(1, message.getSoapKnob().getPayloadNames().length);
        assertEquals(MESS_PAYLOAD_NS, message.getSoapKnob().getPayloadNames()[0].getNamespaceURI());
        assertTrue(message.isXml());
        assertTrue(message.getXmlKnob().getDocumentReadOnly().isEqualNode(XmlUtil.stringToDocument(SOAP)));
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertTrue(XmlUtil.stringToDocument(new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()))).isEqualNode(XmlUtil.stringToDocument(SOAP)));

        // reinitialize the message to Soap using byte array and application/soap+xml content type
        message.initialize(ContentTypeHeader.SOAP_1_2_DEFAULT, SOAP.getBytes("UTF-8"));
        // verify that the message is properly reinitialized
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.SOAP_1_2_DEFAULT));
        assertTrue(XmlUtil.stringToDocument(new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()))).isEqualNode(XmlUtil.stringToDocument(SOAP)));
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }
        assertTrue(message.isSoap());
        assertNotNull(message.getSoapKnob().getPayloadNames());
        assertSame(1, message.getSoapKnob().getPayloadNames().length);
        assertEquals(MESS_PAYLOAD_NS, message.getSoapKnob().getPayloadNames()[0].getNamespaceURI());
        assertTrue(message.isXml());
        assertTrue(message.getXmlKnob().getDocumentReadOnly().isEqualNode(XmlUtil.stringToDocument(SOAP)));
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        assertTrue(XmlUtil.stringToDocument(new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()))).isEqualNode(XmlUtil.stringToDocument(SOAP)));

        // reinitialize the message with multi-parts
        message.initialize(ContentTypeHeader.parseValue(MESS_CONTENT_TYPE), MESS.getBytes("UTF-8"));
        // verify that the message is properly reinitialized
        assertTrue(message.isInitialized());
        assertNotNull(message.getMimeKnob());
        assertTrue(message.getMimeKnob().getOuterContentType().matches(ContentTypeHeader.parseValue(MESS_CONTENT_TYPE)));
        PartIterator parts = message.getMimeKnob().getParts();
        assertNotNull(parts);
        assertTrue(Arrays.equals(IOUtils.slurpStream(parts.next().getInputStream(false)), SOAP.getBytes("UTF-8")));
        assertTrue(Arrays.equals(IOUtils.slurpStream(parts.next().getInputStream(false)), RUBY.getBytes("UTF-8")));
        assertFalse(message.isJson());
        assertNotNull(message.getJsonKnob());
        try {
            message.getJsonKnob().getJsonData();
            fail("Non JSON content should throw");
        } catch (IOException ignore) { /* expected */ }

        assertTrue(message.isSoap());
        assertNotNull(message.getSoapKnob().getPayloadNames());
        assertSame(1, message.getSoapKnob().getPayloadNames().length);
        assertEquals(MESS_PAYLOAD_NS, message.getSoapKnob().getPayloadNames()[0].getNamespaceURI());
        assertTrue(message.isXml());
        assertTrue(message.getXmlKnob().getDocumentReadOnly().isEqualNode(XmlUtil.stringToDocument(SOAP)));
        // verify again against mime knob, making sure that mime knob is cached before initialize is called again
        assertNotNull(message.getMimeKnob());
        parts = message.getMimeKnob().getParts();
        assertNotNull(parts);
        assertTrue(Arrays.equals(IOUtils.slurpStream(parts.next().getInputStream(false)), SOAP.getBytes("UTF-8")));
        assertTrue(Arrays.equals(IOUtils.slurpStream(parts.next().getInputStream(false)), RUBY.getBytes("UTF-8")));
    }
}