package com.l7tech.xmlsig;

import org.w3c.dom.Document;
import com.ibm.xml.dsig.util.DOMParserNS;
import com.ibm.xml.sax.StandardErrorHandler;
import com.ibm.dom.util.XPathCanonicalizer;
import com.l7tech.common.util.HexUtils;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import javax.xml.parsers.DocumentBuilder;

/**
 * User: flascell
 * Date: Aug 19, 2003
 * Time: 10:33:17 AM
 * $Id$
 *
 * Test SoapMsgSigner with files
 */
public class SoapMsgSignerTest {
    public static void main(String[] args) throws Exception {
        // signmsg("/home/flascell/dev/sampleDocs/simplesoapreq.xml");

        try {
            validateSignature("/home/flascell/dev/sampleDocs/signedreq.xml");
        } catch (Exception e) {
            System.err.println();
            System.err.println();
            e.printStackTrace(System.err);
        }

        try {
            validateSignature("/home/flascell/dev/sampleDocs/incompletesig.xml");
        } catch (Exception e) {
            System.err.println();
            System.err.println();
            e.printStackTrace(System.err);
        }

        try {
            validateSignature("/home/flascell/dev/sampleDocs/invalidsig.xml");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

    public static Document signmsg(String doctosignpath) throws Exception {
        // get a sample document
        DocumentBuilder builder = DOMParserNS.createBuilder();
        builder.setErrorHandler(new StandardErrorHandler());
        Document sigdoc = builder.parse(doctosignpath);
        Document sigdocOrig = (Document)sigdoc.cloneNode(true);

        // get a cert
        byte[] certbytes = HexUtils.slurpStream(new FileInputStream("/home/flascell/dev/sampleDocs/user.cer"), 16384);
        ByteArrayInputStream bais = new ByteArrayInputStream(certbytes);
        X509Certificate usercert = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(bais);

        // get the private key from "/home/flascell/dev/sampleDocs/userks"
        Key privateKey = null;
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("/home/flascell/dev/sampleDocs/userks"), "tralala".toCharArray());
        privateKey = ks.getKey("tomcat", "tralala".toCharArray());

        // show what document looks like before signature
        System.out.println("\n\n\nSoap Message with NO signature:\n\n");
        Writer wr = new OutputStreamWriter(System.out, "UTF-8");
        XPathCanonicalizer.serializeAll(sigdoc, true, wr);
        wr.flush();


        SoapMsgSigner signer = new SoapMsgSigner();
        signer.signEnvelope(sigdoc, (PrivateKey)privateKey, usercert);

        // show what document looks like after signature
        System.out.println("\n\n\nSoap Message with signature:\n\n");
        wr = new OutputStreamWriter(System.out, "UTF-8");
        XPathCanonicalizer.serializeAll(sigdoc, true, wr);
        wr.flush();

        return sigdoc;
    }

    public static void validateSignature(String documentpath) throws Exception {
        DocumentBuilder builder = DOMParserNS.createBuilder();
        builder.setErrorHandler(new StandardErrorHandler());
        Document soapmsg = builder.parse(documentpath);
        SoapMsgSigner signer = new SoapMsgSigner();

        X509Certificate cert = signer.validateSignature(soapmsg);
        System.out.println("signature checks. cert: " + cert);
    }
}
