/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 2:40:10 PM
 */
package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.util.AdHocIDResolver;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

class ReceiverXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = Logger.getLogger(ReceiverXmlSecurityProcessor.class.getName());

    private Key decryptionKey;
    private long keyName;
    private DOMXPath encryptedElementXpath;

    /**
     * Create the new instance with the signer information, session, optional
     * encryption key and the security elements.
     *
     * @param session  the sesion, this may be <b>null</b> in case simple sessionless
     *                 signing is requested
     * @param key      the optional encryption key. May be null, that means
     *                 no encryption. The <code>KeyException</code> is trown
     *                 if the encryption is requested.
     * @param elements the security elements
     */
    ReceiverXmlSecurityProcessor(Session session, Key key, long keyName, ElementSecurity[] elements) {
        super(elements);
        this.keyName = keyName;
        this.decryptionKey = key;
    }

    private synchronized DOMXPath getEncryptedElementXpath() {
        if ( encryptedElementXpath == null ) {
            try {
                String pattern = "//*[xenc:EncryptedData/dsig:KeyInfo/dsig:KeyName='"+keyName+"']";
                encryptedElementXpath = new DOMXPath(pattern);
                encryptedElementXpath.addNamespace("xenc", SoapUtil.XMLENC_NS);
                encryptedElementXpath.addNamespace("dsig", SoapUtil.DIGSIG_URI);
            } catch ( JaxenException e ) {
                throw new RuntimeException( "Internal server error: Invalid XPath pattern", e );
            }
        }

        return encryptedElementXpath;
    }


    private static class SignedElementInfo {
        final String id;
        final Element element;
        final Element securityHeader;
        final boolean validityAchieved;
        final X509Certificate[] certificateChain;
        final Throwable throwable;
        
        SignedElementInfo(String id,
                          Element element,
                          Element securityHeader,
                          X509Certificate[] certificateChain,
                          Throwable throwable) {
            this.id = id;
            this.element = element;
            this.securityHeader = securityHeader;
            this.validityAchieved = certificateChain != null;
            this.certificateChain = certificateChain;
            this.throwable = throwable;
            if ((throwable == null) == (certificateChain == null))
                throw new IllegalArgumentException("Must have either throwable or certificateChain but not both");
        }
    }

    private static class EncryptedElementInfo {
        final Element element;
        final Throwable throwable;

        EncryptedElementInfo( Element element, Throwable throwable) {
            this.element = element;
            this.throwable = throwable;
        }
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
        for each node in cipherDocument.signedElements:
            if checkSignature:
                successfulSigList += ( node )
            else:
                failedSigList += (node, throwable)
        */
        SoapMsgSigner.normalizeDoc( document );
        Gathered gathered = null;
        try {
            gathered = gatherSignedElementInfos(document);
        } catch (Exception e) {
            return Result.error(e);
        }
        List signedElementInfos = gathered.signedElementInfos;
        SignedElementInfo envelopeSignedElementInfo = gathered.envelopeElementInfo;

        /*
        for each node in cipherDoc.encryptedElements:
            if shouldBeAbleToDecryptThis:
                if decrypt:
                    successfulEncList += ( node )
                else:
                    failedEncList += ( node, throwable )
        */

        ArrayList encryptedElementInfos = new ArrayList();
        try {
            List xpathResult = getEncryptedElementXpath().selectNodes( document );
            if ( xpathResult != null && xpathResult.size() > 0 ) {
                for ( Iterator i = xpathResult.iterator(); i.hasNext(); ) {
                    Object probablyElement = i.next();
                    if ( probablyElement instanceof Element ) {
                        Element encryptedElement = (Element)probablyElement;
                        Throwable throwable = null;
                        if (decryptionKey == null)
                            return Result.error(new SecurityProcessorException("No symmetric decryption key is available"));
                        try {
                            XmlMangler.decryptElement( encryptedElement, decryptionKey );
                        } catch ( ParserConfigurationException e ) {
                            return Result.error(e);
                        } catch ( SAXException e ) {
                            return Result.error(e);
                        } catch ( XMLSecurityElementNotFoundException e ) {
                            throwable = e;
                        } catch (GeneralSecurityException e) {
                            throwable = e;
                        }

                        EncryptedElementInfo eei = new EncryptedElementInfo( encryptedElement, throwable );
                        encryptedElementInfos.add(eei);
                    } else {
                        return Result.error(new SecurityProcessorException("XPath query was expected to find an Element, but found a " +
                                                                           probablyElement.getClass().getName() ));
                    }
                }
            }
        } catch ( JaxenException e ) {
            return Result.error(e);
        }

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

            SignedElementInfo sigInfo = null;
            for ( Iterator sigi = signedElementInfos.iterator(); sigi.hasNext(); ) {
                SignedElementInfo signedElementInfo = (SignedElementInfo) sigi.next();
                if (targetElement == signedElementInfo.element) {
                    sigInfo = signedElementInfo;
                    break;
                }
            }

            if (sigInfo == null) {
                // This element was supposed to be signed, but we didn't find it when we
                // processed all the signed elements.
                return Result.policyViolation(new SecurityProcessorException("Element " +
                                                                             targetElement.getLocalName() +
                                                                             " was not signed"));
            }

            if (!sigInfo.validityAchieved) {
                // This element's signature did not validate when we processed all the signed elements.
                return Result.policyViolation(sigInfo.throwable);
            }

            // Now check for encryption
            if (elementSecurity.isEncryption() && !XmlUtil.elementIsEmpty(targetElement)) {
                EncryptedElementInfo encInfo = null;
                for (Iterator cryi = encryptedElementInfos.iterator(); cryi.hasNext();) {
                    EncryptedElementInfo encryptedElementInfo = (EncryptedElementInfo)cryi.next();
                    if (encryptedElementInfo.element == targetElement) {
                        encInfo = encryptedElementInfo;
                        break;
                    }

                    // If this is a signed soap envelope, then only the soap body will have been encrypted
                    try {
                        if (targetElement == document.getDocumentElement() &&
                                encryptedElementInfo.element == XmlUtil.findOnlyOneChildElementByName(targetElement,
                                                                                                      SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                                                                      SoapUtil.BODY_EL_NAME)) {
                            encInfo = encryptedElementInfo;
                            break;
                        }
                    } catch (XmlUtil.MultipleChildElementsException e) {
                        return Result.error(new SecurityProcessorException("Multiple SOAP Body elements in message", e));
                    }
                }

                if (encInfo == null) {
                    // This element was supposed to be encrypted, but we didn't find it when we
                    // processed all the encrypted elements.
                    return Result.policyViolation(new SecurityProcessorException("Element " +
                                                                                 targetElement.getLocalName() +
                                                                                 " was not encrypted"));
                }

                if (encInfo.throwable != null) {
                    // This element was supposed to be encrypted, but the attempt to decrypt it
                    // produced an exception.
                    return Result.policyViolation(encInfo.throwable);
                }
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

        if (envelopeSignedElementInfo != null) {
            return Result.ok(document, envelopeSignedElementInfo.certificateChain);
        }

        return Result.ok(document, null);
    }

    private static class Gathered {
        private final List signedElementInfos;
        private final SignedElementInfo envelopeElementInfo;
        Gathered(List signedElementInfos, SignedElementInfo envelopeElementInfo) {
            this.signedElementInfos = signedElementInfos;
            this.envelopeElementInfo = envelopeElementInfo;
        }
    }
    /**
     * @param document the document that may contain zero or more signed elements
     * @return A Gathered containing the list of signed elements we found and processed, and the signed envelope
     *         element, if any.
     * @throws SecurityProcessorException
     * @throws InvalidSignatureException
     */
    private Gathered gatherSignedElementInfos(Document document) throws SecurityProcessorException, InvalidSignatureException {
        List signedElementInfos = new ArrayList();
        SignedElementInfo envelopeSignedElementInfo = null;
        AdHocIDResolver idResolver = new AdHocIDResolver(document);
        try {
            Element header = XmlUtil.findOnlyOneChildElementByName( document.getDocumentElement(),
                                                                                    SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                                                    SoapUtil.HEADER_EL_NAME );
            if (header == null)
                return new Gathered(signedElementInfos, null);

            Element securityHeader = XmlUtil.findOnlyOneChildElementByName(header,
                                                                           SoapUtil.SECURITY_NAMESPACE2,
                                                                           SoapUtil.SECURITY_EL_NAME);
            if (securityHeader == null)
                securityHeader = XmlUtil.findOnlyOneChildElementByName(header,
                                                                       SoapUtil.SECURITY_NAMESPACE,
                                                                       SoapUtil.SECURITY_EL_NAME);
            if (securityHeader == null)
                return new Gathered(signedElementInfos, null);

            List signatureHeaders = XmlUtil.findChildElementsByName( securityHeader,
                                                                     SoapUtil.DIGSIG_URI,
                                                                     SoapUtil.SIGNATURE_EL_NAME );
            for ( Iterator i = signatureHeaders.iterator(); i.hasNext(); ) {
                Element signature = (Element)i.next();

                Element signedInfo = XmlUtil.findOnlyOneChildElementByName( signature,
                                                                            SoapUtil.DIGSIG_URI,
                                                                            SoapUtil.SIGNED_INFO_EL_NAME );

                Element reference = XmlUtil.findOnlyOneChildElementByName( signedInfo,
                                                                           SoapUtil.DIGSIG_URI,
                                                                           SoapUtil.REFERENCE_EL_NAME );

                String signedElementId = reference.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
                if (signedElementId != null && signedElementId.length() > 0) {
                    String id = signedElementId.startsWith("#") ? signedElementId.substring(1) : signedElementId;
                    Element element = idResolver.resolveID( document, id );
                    if (element == null) {
                        String msg = "Signature header referred to nonexistent element ID " + id;
                        logger.warning( msg );
                        throw new SecurityProcessorException(msg);
                    }

                    X509Certificate[] certificateChain = null;
                    Throwable throwable = null;
                    try {
                        certificateChain = SoapMsgSigner.validateSignature( document, element, signature );
                    } catch ( SignatureNotFoundException e ) {
                        throwable = e;
                    } catch ( InvalidSignatureException e ) {
                        throw e;
                    }

                    SignedElementInfo sei = new SignedElementInfo(id, element, signature, certificateChain, throwable);
                    signedElementInfos.add( sei );
                    if (element == document.getDocumentElement()) {
                        if (envelopeSignedElementInfo != null) {
                            throw new SecurityProcessorException("Envelope was already processed in this request");
                        }
                        envelopeSignedElementInfo = sei;
                    }
                }
            }
        } catch ( XmlUtil.MultipleChildElementsException e ) {
            throw new SecurityProcessorException("Multiple " + e.getName() + " elements found", e);
        }

        return new Gathered(signedElementInfos, envelopeSignedElementInfo);
    }

    /**
     * Check whether the element security properties are supported
     *
     * @param elementSecurity the security element to verify
     * @throws java.security.NoSuchAlgorithmException
     *                                  on unsupported algorithm
     * @throws GeneralSecurityException on security properties invalid
     */
    protected void check(ElementSecurity elementSecurity)
      throws NoSuchAlgorithmException, GeneralSecurityException {
        super.check(elementSecurity);
        // need to check for key null
        if (elementSecurity.isEncryption() && decryptionKey == null) {
            // may be an IllegalState too - em
            throw new KeyException("null decryption key, and decryption requested");
        }
    }

}