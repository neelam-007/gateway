package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.MessageNotSoapException;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * The signer is the <code>SecurityProcessor</code> implementation
 * that encapsulates the message signing and optional encryption.
 * This class is shared by client and the server. It cannot be directly
 * instantiated (it is a package private). Use the <code>SecurityProcessor</code>
 * factory method to obtain an instance.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
class SenderXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = Logger.getLogger(SenderXmlSecurityProcessor.class.getName());
    private SignerInfo signerInfo;
    private WssDecorator decorator;
    private X509Certificate recipientCertificate;

    /**
     * Create the new instance with the signer information, session, optional
     * encryption key and the security elements.
     *
     * @param si       the signer information (private key, and certificate)
     * @param elementsToDecorate the security elements
     */
    SenderXmlSecurityProcessor(SignerInfo si,
                               X509Certificate recipientCertificate,
                               ElementSecurity[] elementsToDecorate)
    {
        super(elementsToDecorate);
        this.signerInfo = si;
        if (si == null)
            throw new IllegalArgumentException();
        this.recipientCertificate = recipientCertificate;
        decorator = new WssDecoratorImpl();
    }

    /**
     * Process the document according to the security rules. The rules are applied
     * directly on the document.
     *
     * @param document the document to sign
     * @return the security processor result {@link SecurityProcessor.Result}
     * @throws SecurityProcessorException
     * @throws GeneralSecurityException if unsupported key length or algorithm, or miscellaneous DOM trouble
     * @throws IOException
     */
    public Result processInPlace(Document document)
            throws SecurityProcessorException, GeneralSecurityException, IOException
    {
        Element envelope = document.getDocumentElement();

        // Always sign timestamp unless the whole Envelope is signed
        boolean signTimestamp = true;

        List toSign = new ArrayList();
        List toCrypt = new ArrayList();

        boolean atLeastOneElementWasProcessed = false;

        try {
            for (int i = 0; i < elements.length; i++) {
                ElementSecurity elementSecurity = elements[i];

                boolean preconditionMatched = preconditionMatches(elementSecurity, document);
                if (!preconditionMatched)
                    continue; // skip: there was a precondition and it failed

                FoundElements found = findXpathElement(document, elementSecurity);
                if (found.foundElements.isEmpty())
                    throw new SecurityProcessorException("Unable to sign elements: the precondition xpath matched, " +
                                                         "but the element xpath found no matching elements");

                for (Iterator ei = found.foundElements.iterator(); ei.hasNext();) {
                    Element element = (Element) ei.next();
                    boolean isEnvelope = element == envelope;

                    // Don't need to sign timestamp if we are signing the whole envelope
                    if (isEnvelope)
                        signTimestamp = false;

                    // Always sign it
                    toSign.add(element);
                    atLeastOneElementWasProcessed = true;

                    // Do we need to encrypt it?
                    if (elementSecurity.isEncryption()) {
                        Element encElement = element;

                        if (isEnvelope) {
                            // Can't encrypt the whole envelope, so encrypt just the body instead.
                            encElement = SoapUtil.getBodyElement(document);
                            if (encElement == null)
                                throw new SecurityProcessorException("Message has no SOAP body",
                                                                     new MessageNotSoapException("Message has no SOAP body"));
                        }

                        if (!XmlUtil.elementIsEmpty(encElement))
                            toCrypt.add(encElement);
                        else
                            logger.warning("Encrypt requested XPath '" + found.expression.getExpression() + "'" + " but no child nodes exist, skipping encryption");

                    }
                }
            }

            decorator.decorateMessage(document,
                                      recipientCertificate,
                                      signerInfo.getCertificateChain()[0],
                                      signerInfo.getPrivate(),
                                      signTimestamp,
                                      (Element[])toCrypt.toArray(new Element[0]),
                                      (Element[])toSign.toArray(new Element[0]),
                                      null);

            return atLeastOneElementWasProcessed
                        ? Result.ok(document, signerInfo.getCertificateChain())
                        : Result.notApplicable();

        } catch (InvalidDocumentFormatException e) {
            throw new SecurityProcessorException(e.getMessage(), e); // halt processing for this
        } catch (WssDecorator.DecoratorException e) {
            throw new SecurityProcessorException(e.getMessage(), e); // halt processing for this
        } catch (JaxenException e) {
            throw new SecurityProcessorException(e.getMessage(), e); // halt processing for this
        }
    }

    private static final class FoundElements {
        private FoundElements(XpathExpression expression, List foundElements) {
            this.expression = expression;
            this.foundElements = foundElements;
        }
        private final XpathExpression expression;
        private final List foundElements;
    }

    /** @return a list, possibly empty, of elements that matched elementSecurity's elementXpath xpath expression */
    private FoundElements findXpathElement(Document document, ElementSecurity elementSecurity) throws JaxenException
    {
        XpathExpression elementXpath = elementSecurity.getElementXpath();
        if (elementXpath != null) {
            List nodes = XpathEvaluator.newEvaluator(document, elementXpath.getNamespaces()).select(elementXpath.getExpression());
            return new FoundElements(elementXpath, nodes);
        } else {
            return new FoundElements(elementXpath, Arrays.asList(new Object[] { document.getDocumentElement() }));
        }
    }

    /** @return true iff. the xpath precondition matched or was empty. */
    private static boolean preconditionMatches(ElementSecurity elementSecurity, Document document) throws JaxenException {
        XpathExpression xpath = elementSecurity.getPreconditionXpath();
        if (xpath == null)
            return true;
        List nodes = XpathEvaluator.newEvaluator(document, xpath.getNamespaces()).select(xpath.getExpression());
        if (nodes.isEmpty())
            logger.fine("The XPath precondition result is empty '" + xpath.getExpression() + "' skipping");
        return !nodes.isEmpty();
    }
}