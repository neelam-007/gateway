/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.mortbay.util.WriterOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

/**
 * Test XML encryption and decryption.
 * User: mike
 * Date: Aug 26, 2003
 * Time: 11:51:43 AM
 */
public class XmlManglerTest extends TestCase {
    private static Logger log = Logger.getLogger(XmlManglerTest.class.getName());
    private static final String xmlencNS = "http://www.w3.org/2001/04/xmlenc#";
    static final String SOAP_METHOD_NS = "http://www.l7tech.com/tests/quoter3333";

    public XmlManglerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(XmlManglerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static class RawKey implements SecretKey {
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

    public static Document makeTestMessage() throws Exception {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage soapMsg = mf.createMessage();
        SOAPEnvelope env = soapMsg.getSOAPPart().getEnvelope();
        String p = "gq";
        env.getBody().detachNode();
        SOAPBody body = env.addBody();
        SOAPBodyElement belm = body.addBodyElement(env.createName("getQuote", p, SOAP_METHOD_NS));
        belm.addChildElement("symbol").addTextNode("IBM");

        StringWriter sw = new StringWriter();
        soapMsg.writeTo(new WriterOutputStream(sw));
        StringInputStream sis = new StringInputStream(sw.toString());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document soapDocument = dbf.newDocumentBuilder().parse(sis);

        return soapDocument;
    }

    public static Document makeTestMessageMultipleNodes() throws Exception {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage soapMsg = mf.createMessage();
        SOAPEnvelope env = soapMsg.getSOAPPart().getEnvelope();
        String p = "gq";
        String ns = "http://www.l7tech.com/tests/quoter3333";
        env.getBody().detachNode();
        SOAPBody body = env.addBody();
        SOAPBodyElement belm = body.addBodyElement(env.createName("getQuote", p, ns));
        belm.addChildElement("symbol").addTextNode("IBM");
        belm.addChildElement("date").addTextNode(new Date().toString());

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
        Key encryptionKey = new RawKey(16);

        //log.info("Document before encryption: \n" + documentToString(soapDocument));
        XmlMangler.encryptXml(soapDocument, encryptionKey.getEncoded(), "MyKeyName");
        //log.info("Document after encryption: \n" + documentToString(soapDocument));

        Node cval = soapDocument.getElementsByTagNameNS(xmlencNS, "CipherValue").item(0);
        assertTrue(cval != null);
        assertTrue(cval.getChildNodes().item(0).getNodeValue().length() > 20);
    }

    public void testDecryption() throws Exception {
        Document orig = makeTestMessage();
        Key encryptionKey = new RawKey(16);
        Document crypted = stringToDocument(documentToString(orig));  // preserve a copy of original
        XmlMangler.encryptXml(crypted, encryptionKey.getEncoded(), "MyKeyName");

        // Ensure that no info not in the serialized XML is retained
        crypted = stringToDocument(documentToString(crypted));
        Node cipherData = crypted.getElementsByTagNameNS(xmlencNS, "CipherData").item(0);
        assertTrue(cipherData != null);

        Document decrypted = stringToDocument(documentToString(crypted));
        XmlMangler.decryptDocument(decrypted, encryptionKey);
        //log.info("Document after decryption: \n" + documentToString(decrypted));
    }

    public void testMultiplElementEncryption() throws Exception {
        Document soapDocument = makeTestMessageMultipleNodes();
        Key encryptionKey = new RawKey(16);
        // XmlUtil.documentToOutputStream(soapDocument, System.out);

        NodeList list = soapDocument.getElementsByTagNameNS(SOAP_METHOD_NS, "symbol");
        if (list.getLength() == 0) {
            fail("Should have returned 'symbol' node");
        }
        Element element = (Element)list.item(0);
        XmlMangler.encryptXml(element, encryptionKey.getEncoded(), "MyKeyName", "ref1");

        // XmlUtil.documentToOutputStream(soapDocument, System.out);

        list = soapDocument.getElementsByTagNameNS(SOAP_METHOD_NS, "date");
        if (list.getLength() == 0) {
            fail("Should have returned 'date' node");
        }
        element = (Element)list.item(0);
        XmlMangler.encryptXml(element, encryptionKey.getEncoded(), "MyKeyName", "ref2");

        // log.info("Document after encryption :\n" + documentToString(soapDocument));

        NodeList nl = soapDocument.getElementsByTagNameNS(xmlencNS, "EncryptedData");
        assertTrue(nl != null);
        assertTrue(nl.getLength() == 2);

        // Avoid invalidating nodelist while we are iterating it
        List nll = new ArrayList();
        final int length = nl.getLength();
        for (int i = 0; i < length; i++)
            nll.add(nl.item(i));

        System.out.println("Encrypted document: " + XmlUtil.documentToString(soapDocument));
        for (Iterator i = nll.iterator(); i.hasNext();) {
            element = (Element)i.next();
            element = (Element)element.getParentNode();
            System.out.println("Decrypting element: " + XmlUtil.elementToString(element));
            XmlMangler.decryptElement((Element) element, encryptionKey);
            // log.info("Document after decryption "+i+" :\n" + documentToString(soapDocument));
        }
        nl = soapDocument.getElementsByTagNameNS(xmlencNS, "CipherValue");
        assertTrue(nl != null);
        assertTrue(nl.getLength() == 0);

        System.out.println("Document after decryption: " + XmlUtil.documentToString(soapDocument));        
    }

    /**
     * @throws Exception
     */
    public void testXpathSelectedNodeFromSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        Map namespaces = XpathEvaluator.getNamespaces(sm);

        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namespaces);
        List nodes = xe.select("//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice");
        assertTrue("Size should have been 1", nodes.size() == 1);

        Key encryptionKey = new RawKey(16);
        // log.info("Document before encryption: \n" + documentToString(doc));
        XmlMangler.encryptXml((Element)nodes.get(0), encryptionKey.getEncoded(), "MyKeyName", "ref1");
        // log.info("Document after encryption: \n" + documentToString(doc));

        NodeList nl = doc.getElementsByTagNameNS(xmlencNS, "EncryptedData");
        assertTrue(nl != null);
        assertTrue(nl.getLength() == 1);

        final int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            Element element = (Element)nl.item(i);
            XmlMangler.decryptElement((Element) element.getParentNode(), encryptionKey);
            //log.info("Document after decryption "+i+" :\n" + documentToString(doc));
        }
        nl = doc.getElementsByTagNameNS(xmlencNS, "CipherValue");
        assertTrue(nl != null);
        assertTrue(nl.getLength() == 0);
    }

    public void testDecryptingAMessageThatsNotEncrypted() throws Exception {
        Document orig = makeTestMessage();
        Key key = new RawKey(16);
        try {
            XmlMangler.decryptDocument(orig, key);
            fail("Should have thrown XMLSecurityElementNotFound");
        } catch (XMLSecurityElementNotFoundException e) {
            // ok
        }
    }
}
