package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.Keys;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.SoapMsgSigner;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapRequestGenerator;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
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

    public void testSignXpathSelectedEnvelope() throws Exception {
        SoapRequestGenerator sg = new SoapRequestGenerator();
        SoapRequestGenerator.SOAPRequest[] requests = sg.generate(TestDocuments.WSDL);

        signAndValidateXpathSelection(requests, "/soapenv:Envelope");
    }

    public void testSignXpathSelectedBody() throws Exception {
        SoapRequestGenerator sg = new SoapRequestGenerator();
        SoapRequestGenerator.SOAPRequest[] requests = sg.generate(TestDocuments.WSDL);
        signAndValidateXpathSelection(requests, "/soapenv:Envelope/soapenv:Body");
    }


    private void signAndValidateXpathSelection(SoapRequestGenerator.SOAPRequest[] requests, String xp) throws Exception {
        List securedElements = new ArrayList();

        for (int i = 0; i < requests.length; i++) {
            SoapRequestGenerator.SOAPRequest request = requests[i];
            Map namespaces = XpathEvaluator.getNamespaces(request.getSOAPMessage());
            XpathExpression xpathExpression = new XpathExpression(xp, namespaces);
            final ElementSecurity elementSecurity =
              new ElementSecurity(xpathExpression, false, ElementSecurity.DEFAULT_CIPHER, ElementSecurity.DEFAULT_KEYBITS);
            securedElements.add(elementSecurity);
        }
        ElementSecurity[] data = (ElementSecurity[])securedElements.toArray(new ElementSecurity[]{});
        String signReferenceId = "signref";
        int signReferenceIdSuffix = 1;
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        Document[] documents = new Document[data.length];

        for (int i = 0; i < data.length; i++) {
            SoapRequestGenerator.SOAPRequest request = requests[i];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            request.getSOAPMessage().writeTo(bos);
            Document soapmsg = XmlUtil.stringToDocument(bos.toString());

            ElementSecurity elementSecurity = data[i];
            // XPath match?
            XpathExpression xpath = elementSecurity.getXpathExpression();

            List nodes = XpathEvaluator.newEvaluator(soapmsg, xpath.getNamespaces()).select(xpath.getExpression());
            if (nodes.isEmpty()) {
                fail(xpath.getExpression() + " should have selected document element.");
            }
            Element element = (Element)nodes.get(0);
            // digital sighnature
            dsigHelper.signElement(soapmsg, element,
              signReferenceId + signReferenceIdSuffix,
              signerInfo.getPrivate(), signerInfo.getCertificate());
// System.out.println(XmlUtil.documentToString(soapmsg));

            ++signReferenceIdSuffix;
            documents[i] = soapmsg;
        }

        for (int i = 0; i < documents.length; i++) {
            Document document = documents[i];
            ElementSecurity elementSecurity = data[i];
            // XPath match?
            XpathExpression xpath = elementSecurity.getXpathExpression();

            List nodes = XpathEvaluator.newEvaluator(document, xpath.getNamespaces()).select(xpath.getExpression());
            if (nodes.isEmpty()) {
                fail(xpath.getExpression() + " should have selected document element.");
            }
            Element element = (Element)nodes.get(0);
            dsigHelper.validateSignature(document, element);
        }
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
