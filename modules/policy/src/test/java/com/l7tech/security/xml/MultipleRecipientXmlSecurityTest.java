/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 26, 2005<br/>
 */
package com.l7tech.security.xml;

import com.l7tech.message.Message;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessor;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.junit.Test;
import org.junit.Assert;

import javax.xml.soap.SOAPConstants;
import java.util.logging.Logger;

/**
 * Tests decoration and processing of soap messages secured for multiple recipients.
 *
 * @author flascelles@layer7-tech.com
 */
public class MultipleRecipientXmlSecurityTest {
    private static Logger logger = Logger.getLogger(MultipleRecipientXmlSecurityTest.class.getName());
    private static WssDecorator decorator = new WssDecoratorImpl();
    private static WssProcessor processor = new WssProcessorImpl();

    static {
        JceProvider.init();
    }

    @Test
    public void testSignedForOneRecipientEncryptedForOther() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");
        final String alternaterecipient = "signatureonlyrecipient";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        decorator.decorateMessage(new Message(doc), req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        req.getElementsToEncrypt().add(body);
        decorator.decorateMessage(new Message(doc), req);

        logger.info("Document signed for two recipients and encrypted for second:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
        Assert.assertTrue("The body was encrypted", checkEncryptedElement(res, body));

        logger.info("Document once processed by default recipient:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        Assert.assertNotNull("The security header for downstream actor is still present", alternateSecHeader);
        alternateSecHeader.removeAttributeNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        logger.info("Document once processed by both recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
    }

    public void testBodySignedForTwoRecipients() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        String alternaterecipient = "downstream";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        decorator.decorateMessage(new Message(doc), req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        decorator.decorateMessage(new Message(doc), req);
        logger.info("Document signed for two recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));

        logger.info("Document once processed:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");


        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        Assert.assertNotNull("The security header for downstream actor is still present", alternateSecHeader);
        alternateSecHeader.removeAttributeNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
    }

    @Test
    public void testEncapsulatingEncryptedElementsForDifferentRecipients() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(doc);
        Element prodidEl = getElementByName(doc, "productid");

        logger.info("Original document:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        String alternaterecipient = "downstream";

        // FIRST DECORATION
        DecorationRequirements req = otherDecorationRequirements(doc, alternaterecipient);
        req.getElementsToEncrypt().add(prodidEl);
        decorator.decorateMessage(new Message(doc), req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        req.getElementsToEncrypt().add(body);
        decorator.decorateMessage(new Message(doc), req);

        logger.info("Document signed for two recipients and encrypted for second:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
        Assert.assertTrue("The body was encrypted", checkEncryptedElement(res, body));

        logger.info("Document once processed by default recipient:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        Assert.assertNotNull("The security header for downstream actor is still present", alternateSecHeader);
        Element currentSecHeader = SoapUtil.getSecurityElementForL7(doc);
        if (currentSecHeader == null) {
            currentSecHeader = SoapUtil.getSecurityElement(doc);
        }
        if (currentSecHeader != null) {
            currentSecHeader.getParentNode().removeChild(currentSecHeader);
        }
        alternateSecHeader.removeAttributeNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        logger.info("Document once processed by both recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
        Assert.assertTrue("The accountid was encrypted", checkEncryptedElement(res, prodidEl));
    }

    @Test
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
        decorator.decorateMessage(new Message(doc), req);

        // SECOND DECORATION
        req = defaultDecorationRequirements(doc);
        req.getElementsToEncrypt().add(acctidEl);
        decorator.decorateMessage(new Message(doc), req);

        logger.info("Document signed for two recipients and encrypted for second:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // FIRST PROCESSING
        ProcessorResult res = process(doc);

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
        Assert.assertTrue("The accountid was encrypted", checkEncryptedElement(res, acctidEl));

        logger.info("Document once processed by default recipient:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        // ACTOR PROMOTION
        Element alternateSecHeader = SoapUtil.getSecurityElement(doc, alternaterecipient);
        Assert.assertNotNull("The security header for downstream actor is still present", alternateSecHeader);
        Element currentSecHeader = SoapUtil.getSecurityElementForL7(doc);
        if (currentSecHeader == null) {
            currentSecHeader = SoapUtil.getSecurityElement(doc);
        }
        if (currentSecHeader != null) {
            currentSecHeader.getParentNode().removeChild(currentSecHeader);
        }
        alternateSecHeader.removeAttributeNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,SoapUtil.ACTOR_ATTR_NAME);

        // SECOND PROCESSING
        res = process(doc);

        logger.info("Document once processed by both recipients:\n" + XmlUtil.nodeToFormattedString(doc) + "\n\n");

        Assert.assertTrue("The body was signed", checkSignedElement(res, body));
        Assert.assertTrue("The accountid was encrypted", checkEncryptedElement(res, prodidEl));
    }

    private boolean checkEncryptedElement(ProcessorResult res, Element el) {
        ParsedElement[] encrypted = res.getElementsThatWereEncrypted();
        for ( ParsedElement encedel : encrypted ) {
            if ( encedel.asElement().getLocalName().equals( el.getLocalName() ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean checkSignedElement(ProcessorResult res, Element el) {
        SignedElement[] signed = res.getElementsThatWereSigned();
        for ( SignedElement signedElement : signed ) {
            if ( signedElement.asElement().getLocalName().equals( el.getLocalName() ) ) {
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
        return processor.undecorateMessage(new Message(doc),
                                           null,
                                           null,
                                           new WrapSSTR(TestDocuments.getDotNetServerCertificate(),
                                                        TestDocuments.getDotNetServerPrivateKey()));
    }
}
