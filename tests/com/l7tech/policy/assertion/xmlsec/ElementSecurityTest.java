package com.l7tech.policy.assertion.xmlsec;

import com.ibm.xml.dsig.KeyInfo;
import com.l7tech.common.security.Keys;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.HashMap;

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

        Document[] documents = soapMessagesAsDocuments(requests);

        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];

            // Sign doc
            SecurityProcessor signer = SecurityProcessor.createSenderSecurityProcessor(signerInfo, null, data);
            SecurityProcessor.Result pres = signer.process(document);
            assertEquals(SecurityProcessor.Result.Type.OK.desc, pres.getType().desc);
            Document secureDoc = pres.getDocument();
//System.out.println(XmlUtil.nodeToFormattedString(secureDoc));

            // Check doc
            WssProcessor.ProcessorResult procResult = new WssProcessorImpl().undecorateMessage(secureDoc, null, null);
            assertTrue(procResult.getElementsThatWereSigned().length == 2);
            assertTrue(procResult.getElementsThatWereEncrypted().length < 1);
            assertTrue(procResult.getTimestamp().isSigned());
            SecurityProcessor verifier = SecurityProcessor.createRecipientSecurityProcessor(procResult, data);
            SecurityProcessor.Result vres = verifier.processInPlace(procResult.getUndecoratedMessage());
            assertEquals(SecurityProcessor.Result.Type.OK.desc, vres.getType().desc);
            Document verifiedDoc = vres.getDocument();
// System.out.println(XmlUtil.nodeToFormattedString(verifiedDoc));
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

        X509Certificate serverCert = TestDocuments.getDotNetServerCertificate();
        PrivateKey serverKey = TestDocuments.getDotNetServerPrivateKey();

        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];
            SecurityProcessor signer = SecurityProcessor.createSenderSecurityProcessor(signerInfo, serverCert, data);
            SecurityProcessor.Result pres = signer.process(document);
            assertEquals(SecurityProcessor.Result.Type.OK.desc, pres.getType().desc);
            Document secureDoc = pres.getDocument();
//System.out.println(XmlUtil.nodeToString(secureDoc));

            WssProcessor.ProcessorResult procResult = new WssProcessorImpl().undecorateMessage(secureDoc, serverCert, serverKey);
            SecurityProcessor verifier = SecurityProcessor.createRecipientSecurityProcessor(procResult, data);
            SecurityProcessor.Result vres = verifier.processInPlace(procResult.getUndecoratedMessage());
            assertEquals(SecurityProcessor.Result.Type.OK.desc, vres.getType().desc);
            Document verifiedDoc = vres.getDocument();
// System.out.println(XmlUtil.nodeToFormattedString(verifiedDoc));
        }
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

        Document document = documents[0];
        WssProcessor.ProcessorResult procResult = new WssProcessorImpl().undecorateMessage(document, null, null);
        SecurityProcessor verifier = SecurityProcessor.createRecipientSecurityProcessor(procResult, data);
        SecurityProcessor.Result result = verifier.process(procResult.getUndecoratedMessage());
        assertTrue(result.getType() == SecurityProcessor.Result.Type.POLICY_VIOLATION);
    }

    public void testUndecorateSignedAndEncryptedEnvelopeRequest() throws Exception {
        Document decorated = TestDocuments.getTestDocument(TestDocuments.DOTNET_ENCRYPTED_REQUEST);

        System.out.println("Starting document: " + XmlUtil.nodeToString(decorated));
        Map nm = new HashMap();
        nm.put("soap", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        nm.put("s0", "http://warehouse.acme.com/ws");
        ElementSecurity elementSecurity = new ElementSecurity(new XpathExpression("/soap:Envelope/soap:Body", nm),
                                                              new XpathExpression("*", nm),
                                                              true,
                                                              ElementSecurity.DEFAULT_CIPHER,
                                                              ElementSecurity.DEFAULT_KEYBITS);
        ElementSecurity[] elements = new ElementSecurity[] { elementSecurity };

        X509Certificate serverCert = TestDocuments.getDotNetServerCertificate();
        PrivateKey serverKey = TestDocuments.getDotNetServerPrivateKey();

        WssProcessor.ProcessorResult procResult = new WssProcessorImpl().undecorateMessage(decorated, serverCert, serverKey);
        SecurityProcessor verifier = SecurityProcessor.createRecipientSecurityProcessor(procResult, elements);

        SecurityProcessor.Result result = verifier.processInPlace(procResult.getUndecoratedMessage());
        System.out.println("Undecorated status = " + result.getType().desc);
        //System.out.println("Undecorated document: " + XmlUtil.nodeToFormattedString(decorated));
        assertTrue(result.getType() == SecurityProcessor.Result.Type.OK);

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



