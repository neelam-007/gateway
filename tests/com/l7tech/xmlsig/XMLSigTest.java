package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import javax.xml.parsers.DocumentBuilder;

import com.ibm.xml.dsig.util.DOMParserNS;
import com.ibm.xml.sax.StandardErrorHandler;
import com.l7tech.common.util.HexUtils;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;

import junit.framework.TestCase;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 28, 2003
 * Time: 2:01:06 PM
 * $Id$
 *
 * These tests use a keystore and a certificate in the directory of the test class.
 * The tests director must be in the classpath for this to work
 */
public class XMLSigTest extends TestCase {

    public static void main(String[] args) throws Exception {
        XMLSigTest tester = new XMLSigTest();
        tester.testAndValidateDSig();

    }

    public XMLSigTest() throws Exception {
        // load private key and certificate
        privateKey = getTestKey();
        cert = getTestCert();
    }

    public void testAndValidateDSig() throws Exception {
        // get document
        Document doc = readDocFromString(simpleDoc);
        //Document doc = readDocFromString(simpleDocWithHeader);
        //Document doc = readDocFromString(simpleDocWithSecurity);
        //Document doc = readDocFromString(simpleDocWithSecurityContext);

        // append SecureConversationToken
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(doc, 666, 777);

        // sign it
        SoapMsgSigner signer = new SoapMsgSigner();
        signer.signEnvelope(doc, privateKey, cert);

        System.out.println("SIGNATURE RESULT:\n\n" + serializeDoc(doc));

        // validate the signature (will throw if doesn't validate)
        X509Certificate cert2 = signer.validateSignature(doc);

        assertTrue("OUTPUT CERT SAME AS INPUT ONE", cert.equals(cert2));
    }

    private Document readDocFromString(String docStr)  throws Exception {
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

    private String simpleDoc = "<?xml version=\"1.0\" encoding=\"utf-8\"?><S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\"><S:Body><tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">QQQ</tru:StockSymbol></S:Body></S:Envelope>";
    private String simpleDocWithHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?><S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\"><S:Header></S:Header><S:Body><tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">QQQ</tru:StockSymbol></S:Body></S:Envelope>";
    private String simpleDocWithSecurity = "<?xml version=\"1.0\" encoding=\"utf-8\"?><S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\"><S:Header><wsse:Security xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/12/secext\"></wsse:Security></S:Header><S:Body><tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">QQQ</tru:StockSymbol></S:Body></S:Envelope>";
    private String simpleDocWithSecurityContext = "<?xml version=\"1.0\" encoding=\"utf-8\"?><S:Envelope xmlns:S=\"http://www.w3.org/2001/12/soap-envelope\"><S:Header><wsse:Security xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2002/12/secext\"><wsse:SecurityContextToken></wsse:SecurityContextToken></wsse:Security></S:Header><S:Body><tru:StockSymbol xmlns:tru=\"http://fabrikam123.com/payloads\">QQQ</tru:StockSymbol></S:Body></S:Envelope>";

    private PrivateKey privateKey;
    private X509Certificate cert;
}
