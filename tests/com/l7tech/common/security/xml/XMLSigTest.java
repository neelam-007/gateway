package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.util.DOMParserNS;
import com.ibm.xml.sax.StandardErrorHandler;
import com.l7tech.common.util.HexUtils;
import junit.framework.TestCase;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * LAYER 7 TECHNOLOGIES, INC
 * <p/>
 * User: flascell
 * Date: Aug 28, 2003
 * Time: 2:01:06 PM
 * $Id$
 * <p/>
 * These tests use a keystore and a certificate in the directory of the test class.
 * The tests director must be in the classpath for this to work
 */
public class XMLSigTest extends TestCase {
    public XMLSigTest() throws Exception {
        // load private key and certificate
        privateKey = getTestKey();
        cert = getTestCert();
    }

    /**
     * Create and validate a valid signature
     */
    public void testAndValidateDSig() throws Exception {
        // get document
        Document doc = readDocFromString(simpleDoc);
        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);
        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // validate the signature (will throw if doesn't validate)
        X509Certificate cert2 = SoapMsgSigner.validateSignature(doc);

        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));

        // test with other documents
        doc = readDocFromString(simpleDocWithHeader);
        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);
        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // validate the signature (will throw if doesn't validate)
        cert2 = SoapMsgSigner.validateSignature(doc);

        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));

        doc = readDocFromString(simpleDocWithSecurity);
        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);
        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // validate the signature (will throw if doesn't validate)
        cert2 = SoapMsgSigner.validateSignature(doc);

        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));

        doc = readDocFromString(simpleDocWithSecurityContext);
        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);
        // sign it

        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // validate the signature (will throw if doesn't validate)
        cert2 = SoapMsgSigner.validateSignature(doc);

        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));
    }

    /**
     * Create and validate a valid signature for a single element
     */
    public void testSingleElementSignature() throws Exception {
        // get document
        Document doc = readDocFromString(simpleDoc);
        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);

        NodeList nodes = doc.getElementsByTagNameNS("http://fabrikam123.com/payloads", "StockSymbol");
        if (nodes.getLength() < 1) {
            throw new IllegalStateException("could not find the element");
        }
        Element toSign = (Element)nodes.item(0);
          
        // sign it
          
        SoapMsgSigner.signElement(doc, toSign, "stock_symbol_signature", privateKey, cert);
          // validate the signature (will throw if doesn't validate)
        X509Certificate cert2 = SoapMsgSigner.validateSignature(doc, toSign);

        assertTrue("The signature verification failed", cert.equals(cert2));
    }

    public void testAndValidateDSigOnTrickyDoc() throws Exception {
        // get document
        Document doc = readDocFromString(simpleDocWithComplexTextEl);
        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // serialize and deserialize
        doc = readDocFromString(serializeDoc(doc));

        // validate the signature (will throw if doesn't validate)
        X509Certificate cert2 = SoapMsgSigner.validateSignature(doc);
        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));

        // same thing with trickyer doc
        doc = readDocFromString(simpleDocWithComplexTextEl2);
        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // serialize and deserialize
        doc = readDocFromString(serializeDoc(doc));

        // validate the signature (will throw if doesn't validate)
        cert2 = SoapMsgSigner.validateSignature(doc);
        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));
    }

    /**
     * Try to validate an invalid signature
     */
    public void testAndInvalidateDSig() throws Exception {
        // get document
        Document doc = readDocFromString(simpleDoc);

        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // append something AFTER the signature is made so that signature is no longer valid
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);

        // validate the signature (will throw if doesn't validate)
        boolean signaturevalid = true;
        try {
            SoapMsgSigner.validateSignature(doc);
        } catch (InvalidSignatureException e) {
            signaturevalid = false;
        }

        assertTrue("SIGNATURE SHOULD NOT VALIDATE", !signaturevalid);
    }

    /**
     * make sure signature is still validable after being serialized and parsed again
     * 
     * @throws Exception 
     */
    public void testValidateDSigFromSerializedDocument() throws Exception {
        // get document
        Document doc = readDocFromString(simpleDoc);

        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);

        // sign it
        SoapMsgSigner.signEnvelope(doc, privateKey, cert);

        // serialize and reparse
        Document doc2 = readDocFromString(serializeDoc(doc));

        // validate the signature (will throw if doesn't validate)
        X509Certificate cert2 = SoapMsgSigner.validateSignature(doc2);

        // can still get the cert out of it
        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));
    }

    private Document readDocFromString(String docStr) throws Exception {
        DocumentBuilder builder = DOMParserNS.createBuilder();
        builder.setErrorHandler(new StandardErrorHandler());
        return builder.parse(new InputSource(new StringReader(docStr)));
    }

    private PrivateKey getTestKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream is = getClass().getResourceAsStream("test_ks");
        if (is == null) {
            throw new FileNotFoundException("cannot load resource test_ks. is tests in classpath?");
        }
        ks.load(is, "tralala".toCharArray());
        return (PrivateKey)ks.getKey("tomcat", "tralala".toCharArray());
    }

    private X509Certificate getTestCert() throws Exception {
        InputStream is = getClass().getResourceAsStream("test_cer");
        if (is == null) {
            throw new FileNotFoundException("cannot load resource test_cer. is tests in classpath?");
        }
        return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(HexUtils.slurpStream(is, 16384)));
    }

    private String serializeDoc(Document doc) throws Exception {
        final StringWriter sw = new StringWriter();
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        try {
            OutputFormat of = new OutputFormat();
            of.setIndent(4);
            xmlSerializer.setOutputFormat(of);
            xmlSerializer.serialize(doc);
        } catch (Exception e) {}
        return sw.toString();
    }

    private String simpleDoc = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
      "<S:Body>" +
      "<tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">" +
      "QQQ" +
      "</tru:StockSymbol>" +
      "</S:Body>" +
      "</S:Envelope>";

    private String simpleDocWithComplexTextEl = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
      "<S:Body>" +
      "<blah:BodyContent xmlns:blah=\"http://blah.com/blahns\">" +
      "blahblah blahblah blahblah blahblah</blah:BodyContent>" +
      "</S:Body>" +
      "</S:Envelope>";

    private String simpleDocWithComplexTextEl2 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
      "<S:Body>" +
      "<blah:BodyContent xmlns:blah=\"http://blah.com/blahns\">" +
      " blahblah\tblahblah\t\tblahblah\t\t\r\nblahblah\n\n\n</blah:BodyContent>" +
      "</S:Body>" +
      "</S:Envelope>";

    private String simpleDocWithHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
      "<S:Header></S:Header>" +
      "<S:Body>" +
      "<tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">" +
      "QQQ" +
      "</tru:StockSymbol>" +
      "</S:Body>" +
      "</S:Envelope>";

    private String simpleDocWithSecurity = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
      "<S:Header>" +
      "<wsse:Security xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/12/secext\">" +
      "</wsse:Security>" +
      "</S:Header>" +
      "<S:Body>" +
      "<tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">" +
      "QQQ" +
      "</tru:StockSymbol>" +
      "</S:Body>" +
      "</S:Envelope>";
    private String simpleDocWithSecurityContext = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\">" +
      "<S:Header>" +
      "<wsse:Security xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/12/secext\">" +
      "<wsse:SecurityContextToken></wsse:SecurityContextToken>" +
      "</wsse:Security>" +
      "</S:Header>" +
      "<S:Body>" +
      "<tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">" +
      "QQQ" +
      "</tru:StockSymbol>" +
      "</S:Body>" +
      "</S:Envelope>";

    private PrivateKey privateKey;
    private X509Certificate cert;
}
