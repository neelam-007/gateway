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
import java.util.logging.Logger;

class ReceiverXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = Logger.getLogger(ReceiverXmlSecurityProcessor.class.getName());

    ReceiverXmlSecurityProcessor(WssProcessor.ProcessorResult processorResult, ElementSecurity[] elementsOfInterest) {
        super(elementsOfInterest);
    }

    /**
     * Process the document according to the security rules.
     *
     * @param document the input document to process
     * @return the security processor result {@link SecurityProcessor.Result}
     * @throws java.io.IOException        on io error such as xml processing
     */
    public SecurityProcessor.Result processInPlace( Document document ) throws IOException {

        /*
        for each es in ElementSecurity[]:
            if !precondition.matches(clearDoc):
                continue;
            cipherNode = cipherDoc.xpath(es.elementXpath)

            if !successfulSigList.contains(cipherNode):
                throw failedEncList.get(cipherNode);

            if es.isEncryption:
                if !successfulEncList.contains(cipherNode):
                    throw;
        */
        boolean atLeastOneElementSecurityApplied = false;
        for ( int i = 0; i < elements.length; i++ ) {
            ElementSecurity elementSecurity = elements[i];
            XpathExpression preconditionXpath = elementSecurity.getPreconditionXpath();

            if ( preconditionXpath != null ) {
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
            // TODO: What if multiple actionable elements are found?
            if (elementMatches.size() > 1)
                logger.warning("Multiple elements match receiver XML xpath; ignoring all but the first one " +
                               "(xpath = " + elementXpath.getExpression() + ")");
            Element targetElement = (Element) elementMatches.get(0);

            // TODO convert to check trogdor results
//            SignedElementInfo sigInfo = null;
//            for ( Iterator sigi = signedElementInfos.iterator(); sigi.hasNext(); ) {
//                SignedElementInfo signedElementInfo = (SignedElementInfo) sigi.next();
//                if (targetElement == signedElementInfo.element) {
//                    sigInfo = signedElementInfo;
//                    break;
//                }
//            }

            // TODO throw if no signature found for this element
//            if (sigInfo == null) {
//                // This element was supposed to be signed, but we didn't find it when we
//                // processed all the signed elements.
//                return Result.policyViolation(new SecurityProcessorException("Element " +
//                                                                             targetElement.getLocalName() +
//                                                                             " was not signed"));
//            }

            // Now check for encryption
            if (elementSecurity.isEncryption() && !XmlUtil.elementIsEmpty(targetElement)) {

                // TODO throw if no encryption found for this element
//                if (encInfo == null) {
//                    // This element was supposed to be encrypted, but we didn't find it when we
//                    // processed all the encrypted elements.
//                    return Result.policyViolation(new SecurityProcessorException("Element " +
//                                                                                 targetElement.getLocalName() +
//                                                                                 " was not encrypted"));
//                }
            }

            atLeastOneElementSecurityApplied = true;
        }

        /*
        if successfulSigList.contains(Envelope):
            return new Result(document, preconditionMatched, documentCertificates);

            return new Result( cert )
        */
        if (!atLeastOneElementSecurityApplied)
            return Result.notApplicable();

        // TODO we don't need to return the certificate chain here
        return Result.ok(document, null);
    }
}