package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.Keys;
import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.ElementSecurity;
import com.l7tech.common.security.xml.SecurityProcessor;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.server.SessionManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyException;
import java.security.SignatureException;
import java.util.Map;

/**
 * Test the element security based assertions.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ElementSecurityTest extends TestCase {
    private Keys testKeys;
    private SignerInfo signerInfo;

    public ElementSecurityTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ElementSecurityTest.class);
    }

    protected void setUp() throws Exception {
        testKeys = new Keys(1024);
        String subject = "CN=fred";
        signerInfo = testKeys.asSignerInfo(subject);
    }

    protected void tearDown() throws Exception {
        //
    }


    public void testSignerSignsBody() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);
        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, false, ElementSecurity.DEFAULT_CIPHER, ElementSecurity.DEFAULT_KEYBITS)
        };
        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);

        Document[] documents = soapMessagesAsDocuments(requests);

        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];
            SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, key, data);
            SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, data);
            Document secureDoc = signer.process(document).getDocument();
// System.out.println(XmlUtil.documentToString(secureDoc));
            Document verifiedDoc = verifier.processInPlace(secureDoc).getDocument();
// System.out.println(XmlUtil.documentToString(verifiedDoc));
        }
    }

    public void testSignerSignsAndEncryptsBody() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);
        Document[] documents = soapMessagesAsDocuments(requests);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);

        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, true,
              ElementSecurity.DEFAULT_CIPHER,
              ElementSecurity.DEFAULT_KEYBITS)
        };

        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);


        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];
            SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, key, data);
            SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, data);
            Document secureDoc = signer.process(document).getDocument();
//System.out.println(XmlUtil.documentToString(secureDoc));
            Document verifiedDoc = verifier.processInPlace(secureDoc).getDocument();
// System.out.println(XmlUtil.documentToString(verifiedDoc));
        }
    }

    public void testKeyNotSpecifiedAndEncryptRequested() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);
        Document[] documents = soapMessagesAsDocuments(requests);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);

        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, true,
              ElementSecurity.DEFAULT_CIPHER,
              ElementSecurity.DEFAULT_KEYBITS)
        };

        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);
        Document document = documents[0];
        try {
            SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, null, data);
            signer.process(document);
            fail("KeyException expected");
        } catch (KeyException e) {
            // expected
        }
        SecurityProcessor signer = SecurityProcessor.getSigner(session, signerInfo, key, data);
        Document securedDocument = signer.process(document).getDocument();
        try {
            SecurityProcessor verifier = SecurityProcessor.getVerifier(session, null, data);
            verifier.process(securedDocument);
            fail("KeyException expected");
        } catch (KeyException e) {
            // expected
        }
        SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, data);
        Document verifiedDocument = verifier.process(securedDocument).getDocument();
        // System.out.println(XmlUtil.documentToString(verifiedDocument));
    }

    public void testVerifyUnsecureDocument() throws Exception {
        SoapMessageGenerator sg = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sg.generateRequests(TestDocuments.WSDL);
        Document[] documents = soapMessagesAsDocuments(requests);

        Map namespaces = XpathEvaluator.getNamespaces(requests[0].getSOAPMessage());
        XpathExpression xpathExpression = new XpathExpression("/soapenv:Envelope/soapenv:Body", namespaces);

        ElementSecurity[] data = new ElementSecurity[]{
            new ElementSecurity(xpathExpression, null, true,
              ElementSecurity.DEFAULT_CIPHER,
              ElementSecurity.DEFAULT_KEYBITS)
        };

        Session session = SessionManager.getInstance().createNewSession();
        final Key key = new AesKey(session.getKeyReq(), 128);
        Document document = documents[0];
        SecurityProcessor verifier = SecurityProcessor.getVerifier(session, key, data);
        try {
            Document verifiedDocument = verifier.process(document).getDocument();
            fail("SignatureException expected");
        } catch (SignatureException e) {
            // expected
        }
    }


    private Document[] soapMessagesAsDocuments(SoapMessageGenerator.Message[] requests)
      throws IOException, SOAPException, SAXException {
        Document[] documents = new Document[requests.length];
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            request.getSOAPMessage().writeTo(bos);
            documents[i] = XmlUtil.stringToDocument(bos.toString());
        }
        return documents;

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
