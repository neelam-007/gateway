/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 2:40:10 PM
 */
package com.l7tech.common.security.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
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
            XpathExpression elementXpath = elementSecurity.getElementXpath();
            XpathEvaluator elementEval = XpathEvaluator.newEvaluator(document, elementXpath.getNamespaces());
            List elementMatches = null;
            try {
                elementMatches = elementEval.select(elementXpath.getExpression());
            } catch (JaxenException e) {
                return Result.error(e);
            }
            if ( elementMatches == null || elementMatches.size() == 0 ) {
                return Result.error(new SecurityProcessorException("Precondition matched but element not found"));
            }


            // Ensure that every matching element was signed and/or encrypted
            for (Iterator ei = elementMatches.iterator(); ei.hasNext();) {
                Element targetElement = (Element) ei.next();

                boolean signed = false;
                for (int si = 0; si < elementsThatWereSigned.length; si++) {
                    WssProcessor.SignedElement signedElement = elementsThatWereSigned[si];
                    if (XmlUtil.isElementAncestor(targetElement, signedElement.asElement())) {
                        signed = true;
                        signingTokens.add(signedElement);
                    }
                }

                if (!signed)
                    return Result.policyViolation(new SecurityProcessorException("Element " +
                                                                                 targetElement.getLocalName() +
                                                                                 " was not signed"));

                if (elementSecurity.isEncryption() && !XmlUtil.elementIsEmpty(targetElement)) {
                    boolean encrypted = false;
                    for (int ci = 0; ci < elementsThatWereEncrypted.length; ci++) {
                        Element encryptedElement = elementsThatWereEncrypted[ci];
                        if (XmlUtil.isElementAncestor(targetElement, encryptedElement)) {
                            encrypted = true;
                            break;
                        }
                    }

                    if (!encrypted)
                        return Result.policyViolation(new SecurityProcessorException("Element " +
                                                                                     targetElement.getLocalName() +
                                                                                     " was not encrypted"));
                }
            }

            atLeastOneElementSecurityApplied = true;
        }

        if (!atLeastOneElementSecurityApplied)
            return Result.notApplicable();

        // This can't happen, but just to cover all bases
        if (signingTokens.size() < 1)
            return Result.error(new SecurityProcessorException("At least one element was signed, " +
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
}