/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 26, 2005<br/>
 */
package com.l7tech.common.security.xml;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.token.ParsedElement;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessor;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.logging.Logger;

/**
 * Tests decoration and processing of soap messages secured for multiple recipients.
 *
 * @author flascelles@layer7-tech.com
 */
public class MultipleRecipientXmlSecurityTest extends TestCase {
    private static Logger logger = Logger.getLogger(MultipleRecipientXmlSecurityTest.class.getName());
    private static WssDecorator decorator = new WssDecoratorImpl();
    private static WssProcessor processor = new WssProcessorImpl();

    static {
        JceProvider.init();
    }

    public MultipleRecipientXmlSecurityTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MultipleRecipientXmlSecurityTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSignedForOneRecipientEncryptedForOther() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");
        final String alternaterecipient = "signatureonlyrecipient";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        decorator.decorateMessage(doc, req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        req.getElementsToEncrypt().add(body);
        decorator.decorateMessage(doc, req);

        logger.info("Document signed for two recipients and encrypted for second:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        assertTrue("The body was signed", checkSignedElement(res, body));
        assertTrue("The body was encrypted", checkEncryptedElement(res, body));

        logger.info("Document once processed by default recipient:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        assertTrue("The security header for downstream actor is still present", alternateSecHeader != null);
        alternateSecHeader.removeAttribute(SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        logger.info("Document once processed by both recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        assertTrue("The body was signed", checkSignedElement(res, body));
    }

    public void testBodySignedForTwoRecipients() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        String alternaterecipient = "downstream";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        decorator.decorateMessage(doc, req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        decorator.decorateMessage(doc, req);
        logger.info("Document signed for two recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        assertTrue("The body was signed", checkSignedElement(res, body));

        logger.info("Document once processed:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");


        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        assertTrue("The security header for downstream actor is still present", alternateSecHeader != null);
        alternateSecHeader.removeAttribute(SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        assertTrue("The body was signed", checkSignedElement(res, body));
    }

    public void testEncapsulatingEncryptedElementsForDifferentRecipients() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        Element prodidEl = getElementByName(doc, "productid");

        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        String alternaterecipient = "downstream";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        req.getElementsToEncrypt().add(prodidEl);
        decorator.decorateMessage(doc, req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        req.getElementsToEncrypt().add(body);
        decorator.decorateMessage(doc, req);

        logger.info("Document signed for two recipients and encrypted for second:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        assertTrue("The body was signed", checkSignedElement(res, body));
        assertTrue("The body was encrypted", checkEncryptedElement(res, body));

        logger.info("Document once processed by default recipient:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        assertTrue("The security header for downstream actor is still present", alternateSecHeader != null);
        Element currentSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
        if (currentSecHeader == null) {
            currentSecHeader = SoapUtil.getSecurityElement(doc);
        }
        if (currentSecHeader != null) {
            currentSecHeader.getParentNode().removeChild(currentSecHeader);
        }
        alternateSecHeader.removeAttribute(SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        logger.info("Document once processed by both recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        assertTrue("The body was signed", checkSignedElement(res, body));
        assertTrue("The accountid was encrypted", checkEncryptedElement(res, prodidEl));
    }

    public void testAdjacentEncryptedElementsForDifferentRecipients() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        Element prodidEl = getElementByName(doc, "productid");
        Element acctidEl = getElementByName(doc, "accountid");

        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        String alternaterecipient = "downstream";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        req.getElementsToEncrypt().add(prodidEl);
        decorator.decorateMessage(doc, req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        req.getElementsToEncrypt().add(acctidEl);
        decorator.decorateMessage(doc, req);

        logger.info("Document signed for two recipients and encrypted for second:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        assertTrue("The body was signed", checkSignedElement(res, body));
        assertTrue("The accountid was encrypted", checkEncryptedElement(res, acctidEl));

        logger.info("Document once processed by default recipient:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        assertTrue("The security header for downstream actor is still present", alternateSecHeader != null);
        Element currentSecHeader = SoapUtil.getSecurityElement(doc, SecurityActor.L7ACTOR.getValue());
        if (currentSecHeader == null) {
            currentSecHeader = SoapUtil.getSecurityElement(doc);
        }
        if (currentSecHeader != null) {
            currentSecHeader.getParentNode().removeChild(currentSecHeader);
        }
        alternateSecHeader.removeAttribute(SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        logger.info("Document once processed by both recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        assertTrue("The body was signed", checkSignedElement(res, body));
        assertTrue("The accountid was encrypted", checkEncryptedElement(res, prodidEl));
    }

    private boolean checkEncryptedElement(ProcessorResult res, Element el) {
        ParsedElement[] encrypted = res.getElementsThatWereEncrypted();
        for (int i = 0; i < encrypted.length; i++) {
            ParsedElement encedel = encrypted[i];
            if (encedel.asElement().getLocalName().equals(el.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSignedElement(ProcessorResult res, Element el) {
        SignedElement[] signed = res.getElementsThatWereSigned();
        for (int i = 0; i < signed.length; i++) {
            SignedElement signedElement = signed[i];
            if (signedElement.asElement().getLocalName().equals(el.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    private Element getElementByName(Document doc, String childName) throws Exception {
        NodeList list = doc.getElementsByTagName(childName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element) {
                Element el = (Element)node;
                if (el.getLocalName().equals(childName)) return el;
            }
        }
        throw new RuntimeException("Element " + childName + " not found in " + XmlUtil.nodeToFormattedString(doc));
    }

    private DecorationRequirements defaultDecorationRequirements(Document doc) throws Exception {
        DecorationRequirements output = new DecorationRequirements();
        output.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        output.setSenderMessageSigningCertificate(TestDocuments.getFrancoCertificate());
        output.setSenderMessageSigningPrivateKey(TestDocuments.getFrancoPrivateKey());
        output.setSignTimestamp();
        Element body = SoapUtil.getBodyElement(doc);
        output.getElementsToSign().add(body);
        return output;
    }

    private DecorationRequirements otherDecorationRequirements(Document doc, String actor) throws Exception {
        DecorationRequirements output = new DecorationRequirements();
        output.setRecipientCertificate(TestDocuments.getDotNetServerCertificate());
        output.setSenderMessageSigningCertificate(TestDocuments.getFrancoCertificate());
        output.setSenderMessageSigningPrivateKey(TestDocuments.getFrancoPrivateKey());
        output.setSignTimestamp();
        Element body = SoapUtil.getBodyElement(doc);
        output.getElementsToSign().add(body);
        output.setSecurityHeaderActor(actor);
        return output;
    }

    private ProcessorResult process(Document doc) throws Exception {
        return processor.undecorateMessage(doc,
                                          TestDocuments.getDotNetServerCertificate(),
                                          TestDocuments.getDotNetServerPrivateKey(),
                                          null);
    }
}
