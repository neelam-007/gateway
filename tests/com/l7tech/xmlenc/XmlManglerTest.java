/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.xmlenc;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.mortbay.util.WriterOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;

import com.l7tech.common.security.xml.XMLSecurityElementNotFoundException;
import com.l7tech.common.security.xml.XmlMangler;

/**
 * Test XML encryption and decryption.
 * User: mike
 * Date: Aug 26, 2003
 * Time: 11:51:43 AM
 */
public class XmlManglerTest extends TestCase {
    private static Logger log = Logger.getLogger(XmlManglerTest.class.getName());
    private static final String xmlencNS = "http://www.w3.org/2001/04/xmlenc#";

    public XmlManglerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(XmlManglerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static class RawKey implements SecretKey {
        private byte[] bytes;

        // Generate a random Raw Key with the specified number of bytes
        public RawKey(int numBytes) {
            try {
                SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                this.bytes = new byte[numBytes];
                sr.nextBytes(this.bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen
            }
        }

        public RawKey(byte[] bytes) {
            this.bytes = bytes;
        }

        public String getAlgorithm() {
            return "AES";
        }

        public String getFormat() {
            return "RAW";
        }

        public byte[] getEncoded() {
            return bytes;
        }
    }

    private static String documentToString(Document doc) throws IOException {
        final StringWriter sw = new StringWriter();
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(doc);
        return sw.toString();
    }

    private static Document stringToDocument(String xml) throws Exception {
        StringInputStream sis = new StringInputStream(xml);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document soapDocument = dbf.newDocumentBuilder().parse(sis);

        return soapDocument;
    }

    private Document makeTestMessage() throws Exception {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage soapMsg = mf.createMessage();
        SOAPEnvelope env = soapMsg.getSOAPPart().getEnvelope();
        String p = "gq";
        String ns = "http://www.l7tech.com/tests/quoter3333";
        env.getBody().detachNode();
        SOAPBody body = env.addBody();
        SOAPBodyElement belm = body.addBodyElement(env.createName("getQuote", p, ns));
        belm.addChildElement("symbol").addTextNode("IBM");

        StringWriter sw = new StringWriter();
        soapMsg.writeTo(new WriterOutputStream(sw));
        StringInputStream sis = new StringInputStream(sw.toString());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document soapDocument = dbf.newDocumentBuilder().parse(sis);

        return soapDocument;
    }

    public void testEncryption() throws Exception {
        Document soapDocument = makeTestMessage();
        Key encryptionKey = new RawKey(32);

        log.info("Document before encryption: \n" + documentToString(soapDocument));
        XmlMangler.encryptXml(soapDocument, encryptionKey.getEncoded(), "MyKeyName");
        log.info("Document after encryption: \n" + documentToString(soapDocument));

        Node cval = soapDocument.getElementsByTagNameNS(xmlencNS, "CipherValue").item(0);
        assertTrue(cval != null);
        assertTrue(cval.getChildNodes().item(0).getNodeValue().length() > 20);
    }

    public void testDecryption() throws Exception {
        Document orig = makeTestMessage();
        Key encryptionKey = new RawKey(32);
        Document crypted = stringToDocument(documentToString(orig));  // preserve a copy of original
        XmlMangler.encryptXml(crypted, encryptionKey.getEncoded(), "MyKeyName");

        // Ensure that no info not in the serialized XML is retained
        crypted = stringToDocument(documentToString(crypted));
        Node cipherData = crypted.getElementsByTagNameNS(xmlencNS, "CipherData").item(0);
        assertTrue(cipherData != null);

        Document decrypted = stringToDocument(documentToString(crypted));
        XmlMangler.decryptXml(decrypted, encryptionKey);
        log.info("Document after decryption: \n" + documentToString(decrypted));
    }

    public void testDecryptingAMessageThatsNotEncrypted() throws Exception {
        Document orig = makeTestMessage();
        Key key = new RawKey(32);
        try {
            XmlMangler.decryptXml(orig, key);
            fail("Should have thrown XMLSecurityElementNotFound");
        } catch (XMLSecurityElementNotFoundException e) {
            // ok
        }
    }
}
