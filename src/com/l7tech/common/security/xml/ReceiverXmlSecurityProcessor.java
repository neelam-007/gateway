/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 2:40:10 PM
 */
package com.l7tech.common.security.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;

class ReceiverXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = Logger.getLogger(ReceiverXmlSecurityProcessor.class.getName());
    private WssProcessor.SignedElement[] elementsThatWereSigned;
    private Element[] elementsThatWereEncrypted;

    ReceiverXmlSecurityProcessor(WssProcessor.ProcessorResult processorResult, ElementSecurity[] elementsOfInterest) {
        super(elementsOfInterest);
        elementsThatWereSigned = processorResult.getElementsThatWereSigned();
        elementsThatWereEncrypted = processorResult.getElementsThatWereEncrypted();
    }

    public SecurityProcessor.Result processInPlace(Document document) throws IOException {

        boolean atLeastOneElementSecurityApplied = false;
        Set signingTokens = new HashSet();

        for (int i = 0; i < elements.length; i++) {
            ElementSecurity elementSecurity = elements[i];
            XpathExpression preconditionXpath = elementSecurity.getPreconditionXpath();
            if (preconditionXpath != null) {
                try {
                    XpathEvaluator preconditionEval = XpathEvaluator.newEvaluator(document,preconditionXpath.getNamespaces());
                    List preconditionMatches = preconditionEval.select( preconditionXpath.getExpression() );
                    if ( preconditionMatches == null || preconditionMatches.size() == 0 )
                        // Was precondition, but wasn't matched by this message.  Ignore this ElementSecurity.
                        continue;
                } catch ( JaxenException e ) {
                    return Result.error(e);
                }
            }

            // precondition satisfied
            atLeastOneElementSecurityApplied = true;

            XpathExpression elementXpath = elementSecurity.getElementXpath();
            List elementMatches = null;
            List cryptMatches = null;
            if (elementXpath != null) {
                // We have an element xpath
                XpathEvaluator elementEval = XpathEvaluator.newEvaluator(document, elementXpath.getNamespaces());
                try {
                    elementMatches = elementEval.select(elementXpath.getExpression());
                    if (elementSecurity.isEncryption())
                        cryptMatches = elementMatches;
                } catch (JaxenException e) {
                    return Result.error(e);
                }
                if ( elementMatches == null || elementMatches.size() == 0 ) {
                    return Result.error(new SecurityProcessorException("Precondition matched but element not found"));
                }
            } else {
                // null element xpath.. this means envelope must be signed and body must encrypted
                elementMatches = Arrays.asList(new Element[] { document.getDocumentElement() });

                if (elementSecurity.isEncryption()) {
                    try {
                        Element body = SoapUtil.getBodyElement(document);
                        if (body == null)
                            return Result.error(new MessageNotSoapException("Message contains no Body element"));
                        cryptMatches = Arrays.asList(new Element[] { body });
                    } catch (InvalidDocumentFormatException e) {
                        return Result.error(e);
                    }
                }
            }

            // Ensure that every matching element was signed and/or encrypted
            for (Iterator ei = elementMatches.iterator(); ei.hasNext();) {
                Element targetElement = (Element) ei.next();

                List signedElements = getSigningTokens(targetElement);
                if (signedElements == null || signedElements.size() < 1)
                    return Result.policyViolation(new SecurityProcessorException("Element " +
                                                                                 targetElement.getLocalName() +
                                                                                 " was not signed"));
                signingTokens.addAll(signedElements);
            }

            if (elementSecurity.isEncryption() && cryptMatches != null) {
                // Ensure that every matching element was encrypted
                for (Iterator eci = cryptMatches.iterator(); eci.hasNext();) {
                    Element targetElement = (Element) eci.next();

                    if (!XmlUtil.elementIsEmpty(targetElement)) {
                        if (!elementWasDirectlyOrIndirectlyEncrypted(targetElement))
                            return Result.policyViolation(new SecurityProcessorException("Element " +
                                                                                         targetElement.getLocalName() +
                                                                                         " was non-empty but was not encrypted"));
                    }
                }
            }
        }

        if (!atLeastOneElementSecurityApplied)
            return Result.notApplicable();

        // This can't happen, but just to cover all bases
        if (signingTokens.size() < 1)
            return Result.error(new SecurityProcessorException("At least one element was supposed to be signed, " +
                                                               "but no BinarySecurityToken was found"));

        // TODO decide if we are willing to allow different parts to be signed by different certificates
        // and if so, how this will affect our logic when used as a credential source.
        if (signingTokens.size() > 1)
            return Result.error(new SecurityProcessorException("At least one element was signed, " +
                                                               "but more than one BinarySecurityToken was used"));

        // TODO think about changing TROGDOR to read chains instead of single certs from BinarySecurityToken
        WssProcessor.X509SecurityToken signingToken = (WssProcessor.X509SecurityToken) signingTokens.iterator().next();
        X509Certificate signingCert = signingToken.asX509Certificate();
        return Result.ok(document, new X509Certificate[] {signingCert});
    }

    /**
     * Returns a list of SignedElement intances whose accompanying X509 security tokens directly or indirectly
     * signed the specified element in the undecorated message.
     * @param element the element to examine
     * @return a list of WssProcessor.SignedElement instances.  Empty list if element was not signed.
     */
    private List getSigningTokens(Element element) {
        List signingTokens = new ArrayList();
        for (int si = 0; si < elementsThatWereSigned.length; si++) {
            WssProcessor.SignedElement signedElement = elementsThatWereSigned[si];
            if (XmlUtil.isElementAncestor(element, signedElement.asElement()))
                signingTokens.add(signedElement);
        }
        return signingTokens;
    }

    /**
     * Check if the specified element was encrypted in the original message.
     * @param element the element to examine
     * @return true iff. this element or a direct ancestor had all of its non-whitespace content encrypted
     */
    private boolean elementWasDirectlyOrIndirectlyEncrypted(Element element) {
        for (int ci = 0; ci < elementsThatWereEncrypted.length; ci++) {
            Element encryptedElement = elementsThatWereEncrypted[ci];
            if (XmlUtil.isElementAncestor(element, encryptedElement))
                return true;
        }
        return false;
    }
}