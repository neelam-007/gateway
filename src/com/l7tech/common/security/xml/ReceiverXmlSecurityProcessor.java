/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 2:40:10 PM
 */
package com.l7tech.common.security.xml;

import com.l7tech.common.util.CommonLogger;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.ibm.xml.dsig.util.AdHocIDResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.jaxen.dom.DOMXPath;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

class ReceiverXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = CommonLogger.getSystemLogger();

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
                String pattern = "//[xenc:EncryptedData/xenc:KeyInfo/xenc:KeyName='"+keyName+"']";
                encryptedElementXpath = new DOMXPath(pattern);
                encryptedElementXpath.addNamespace("xenc", SoapUtil.XMLENC_NS);
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
     * @throws java.security.GeneralSecurityException
     *                                    on security error such as unknown
     *                                    algorithm etc. The nature of the error is subclass
     * @throws java.io.IOException        on io error such as xml processing
     * @throws SecurityProcessorException thrown on errors detected
     *                                    during element processing such as invalid or missing security
     *                                    properties, XPath error etc.
     */
    public SecurityProcessor.Result processInPlace_pseudo( Document document )
            throws SecurityProcessorException, GeneralSecurityException, IOException {
        /*
        for each node in cipherDocument.signedElements:
            if checkSignature:
                successfulSigList += ( node )
            else:
                failedSigList += (node, throwable)
        */
        SoapMsgSigner.normalizeDoc( document );
        AdHocIDResolver idResolver = new AdHocIDResolver(document);           
        List signedElementInfos = new ArrayList();
        try {
            Element header = XmlUtil.findOnlyOneChildElementByName( document.getDocumentElement(),
                                                                                    SOAPConstants.URI_NS_SOAP_ENVELOPE,
                                                                                    SoapUtil.HEADER_EL_NAME );
            List signatureHeaders = XmlUtil.findChildElementsByName( header,
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
                        return Result.error( new SecurityProcessorException(msg) );
                    }

                    X509Certificate[] certificateChain = null;
                    Throwable throwable = null;
                    try {
                        certificateChain = SoapMsgSigner.validateSignature( document, element, signature );
                    } catch ( SignatureNotFoundException e ) {
                        throwable = e;
                    } catch ( InvalidSignatureException e ) {
                        return Result.error( e );
                    }

                    SignedElementInfo sei = new SignedElementInfo(id, element, signature, certificateChain, throwable);
                    signedElementInfos.add( sei );
                }
            }
        } catch ( XmlUtil.MultipleChildElementsException e ) {
            return Result.error(new SecurityProcessorException("Multiple " + e.getName() + " elements found", e));
        }


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
                        try {
                            XmlMangler.decryptElement( encryptedElement, decryptionKey );
                        } catch ( ParserConfigurationException e ) {
                            return Result.error(e);
                        } catch ( SAXException e ) {
                            return Result.error(e);
                        } catch ( XMLSecurityElementNotFoundException e ) {
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
        for ( int i = 0; i < elements.length; i++ ) {
            ElementSecurity elementSecurity = elements[i];
            XpathExpression preconditionXpath = elementSecurity.getPreconditionXpath();

            if ( preconditionXpath == null ) {
                // TODO
            } else {
                try {
                    XpathEvaluator preconditionEval = XpathEvaluator.newEvaluator(document,preconditionXpath.getNamespaces());
                    List preconditionMatches = preconditionEval.select( preconditionXpath.getExpression() );
                    if ( preconditionMatches == null || preconditionMatches.size() == 0 )
                        continue;

                    XpathExpression elementXpath = elementSecurity.getElementXpath();

                    // precondition satisfied
                    XpathEvaluator elementEval = XpathEvaluator.newEvaluator(document, elementXpath.getNamespaces());
                    List elementMatches = elementEval.select(elementXpath.getExpression());
                    if ( elementMatches == null || elementMatches.size() == 0 ) {
                        return Result.error(new SecurityProcessorException("Precondition matched but element not found"));
                    }
                    // TODO: What if multiple actionable elements are found?
                    if (elementMatches.size() > 1)
                        logger.warning("Multiple elements match receiver XML xpath; ignoring all but the first one " +
                                       "(xpath = " + elementXpath.getExpression());
                    Element targetElement = (Element) elementMatches.get(0);

                    SignedElementInfo info = null;
                    for ( Iterator sigi = signedElementInfos.iterator(); sigi.hasNext(); ) {
                        SignedElementInfo signedElementInfo = (SignedElementInfo) sigi.next();
                        if (targetElement == signedElementInfo.element) {
                            info = signedElementInfo;
                            break;
                        }
                    }

                    if (info == null) {
                        // This element was supposed to be signed, but we didn't find it when we
                        // processed all the signed elements.
                        return Result.policyViolation(new SecurityProcessorException("Element " + targetElement.getLocalName() + " was not signed"));
                    }

                    if (!info.validityAchieved) {
                        
                    }


                } catch ( JaxenException e ) {
                    return Result.error(e);
                }
            }
        }

        /*
        if successfulSigList.contains(Envelope):
            return new Result(document, preconditionMatched, documentCertificates);

            return new Result( cert )
        */
        return null;
    }

    /**
     * Process the document according to the security rules.
     *
     * @param document the input document to process
     * @return the security processor result {@link SecurityProcessor.Result}
     * @throws java.io.IOException        on io error such as xml processing
     */
    public Result processInPlace(Document document) throws IOException {
        boolean envelopeProcessed = false;

        try {
            X509Certificate[] documentCertificates = null;
            boolean preconditionMatched = false;
            List deferred = new LinkedList(); // defer processing of operations involving encryption
            for (int i = elements.length - 1; i >= 0 && !envelopeProcessed; i--) {
                ElementSecurity elementSecurity = elements[i];

                if (elementSecurity.isEncryption()) {
                    deferred.add(elementSecurity);
                    continue;
                }

                envelopeProcessed = ElementSecurity.isEnvelope(elementSecurity);
                if ( envelopeProcessed ) preconditionMatched = true;

                // XPath precondition match?
                XpathExpression preconditionXpath = elementSecurity.getPreconditionXpath();
                if (preconditionXpath != null) {
                    List nodes = XpathEvaluator.newEvaluator(document, preconditionXpath.getNamespaces()).select(preconditionXpath.getExpression());
                    if (nodes.isEmpty()) {
                        logger.fine("The XPath precondition result is empty '" + preconditionXpath.getExpression() + "' skipping");
                        continue;
                    } else {
                        preconditionMatched = true;
                    }
                }

                Element messagePartElement = null;
                preconditionXpath = elementSecurity.getElementXpath();
                if (preconditionXpath != null) {
                    List nodes = XpathEvaluator.newEvaluator(document, preconditionXpath.getNamespaces()).select(preconditionXpath.getExpression());
                    if (nodes.isEmpty()) {
                        final String message = "The XPath result is empty '" + preconditionXpath.getExpression() + "'";
                        String logmessage = message + "\nMessage is\n" + XmlUtil.documentToString(document);
                        logger.warning(logmessage);
                        throw new SecurityProcessorException(message);
                    }
                    messagePartElement = (Element)nodes.get(0);
                } else {
                    messagePartElement = document.getDocumentElement();
                    envelopeProcessed = true; //signal to ignore everything else. Should scream if more elements exist?
                }
                // verifiy element signature

                // verify that this cert is signed with the root cert of this ssg
                documentCertificates = SoapMsgSigner.validateSignature(document, messagePartElement);
                logger.fine("signature of response message verified");

                if (elementSecurity.isEncryption()) { //element security is required
                    if (messagePartElement.hasChildNodes()) {
                        check(elementSecurity);
                        XmlMangler.decryptElement(messagePartElement, decryptionKey);
                    } else {
                        logger.warning("Encrypt requested XPath '" + preconditionXpath.getExpression() + "'" + " but no child nodes exist, skipping encryption");
                    }
                }
                logger.fine("response message element decrypted");
            }

            // Now do the deferred ones that had encryption
            for (Iterator i = deferred.iterator(); i.hasNext();) {
                ElementSecurity elementSecurity = (ElementSecurity)i.next();

                envelopeProcessed = ElementSecurity.isEnvelope(elementSecurity);
                if ( envelopeProcessed ) preconditionMatched = true;

                // XPath precondition match?
                XpathExpression preconditionXpath = elementSecurity.getPreconditionXpath();
                if (preconditionXpath != null) {
                    List nodes = XpathEvaluator.newEvaluator(document, preconditionXpath.getNamespaces()).select(preconditionXpath.getExpression());
                    if (nodes.isEmpty()) {
                        if ( elementSecurity.isEncryption() ) {
                            // FALLTHROUGH: Speculatively decrypt the part to attempt to match the operation
                            logger.fine("Operation did not match using XPath '" + preconditionXpath.getExpression() +
                                        "'; will decrypt and retry");
                        } else {
                            logger.fine("Operation did not match using XPath '" + preconditionXpath.getExpression() +
                                        "'; skipping");
                            continue;
                        }
                    } else {
                        preconditionMatched = true;
                    }
                }

                Element messagePartElement = null;
                XpathExpression elementXpath = elementSecurity.getElementXpath();
                if (elementXpath != null) {
                    List nodes = XpathEvaluator.newEvaluator(document,
                                                             elementXpath.getNamespaces()).select(elementXpath.getExpression());
                    if (nodes.isEmpty())
                        continue; // Request wasn't encrypted; it's just a non-matching request

                    messagePartElement = (Element)nodes.get(0);
                } else {
                    messagePartElement = document.getDocumentElement();
                    envelopeProcessed = true; //signal to ignore everything else. Should scream if more elements exist?
                }

                SignatureNotFoundException signatureNotFoundException = null;
                XMLSecurityElementNotFoundException xmlSecurityElementNotFoundException = null;

                // verify element signature
                try {
                    // verify that this cert is signed with the root cert of this ssg
                    documentCertificates = SoapMsgSigner.validateSignature(document, messagePartElement);
                    logger.fine("signature of response message verified");
                } catch ( SignatureNotFoundException e ) {
                    logger.fine("signature of response not found");
                    signatureNotFoundException = e;
                }

                if (elementSecurity.isEncryption()) { //element security is required
                    if (messagePartElement.hasChildNodes()) {
                        check(elementSecurity);
                        if (envelopeProcessed) {
                            // Find the Body
                            try {
                                Element bodyElement = XmlUtil.findOnlyOneChildElementByName( messagePartElement,
                                                                                             messagePartElement.getNamespaceURI(),
                                                                                             SoapUtil.BODY_EL_NAME );
                                if ( bodyElement == null ) {
                                    String msg = "No SOAP:Body element found";
                                    logger.warning(msg);
                                    throw new SecurityProcessorException(msg);
                                }

                                try {
                                    // Bug 838: Save exception in case this really is the correct operation; otherwise ignore it
                                    XmlMangler.decryptElement(bodyElement, decryptionKey);
                                } catch ( XMLSecurityElementNotFoundException e ) {
                                    xmlSecurityElementNotFoundException = e;
                                }
                            } catch ( XmlUtil.MultipleChildElementsException e ) {
                                String msg = "Message contained multiple SOAP:Body elements";
                                logger.warning(msg);
                                throw new SecurityProcessorException( msg );
                            }
                        } else {
                            try {
                                // Bug 838: Save exception in case this really is the correct operation; otherwise ignore it
                                XmlMangler.decryptElement(messagePartElement, decryptionKey);
                            } catch ( XMLSecurityElementNotFoundException e ) {
                                xmlSecurityElementNotFoundException = e;
                            }
                        }
                    } else {
                        logger.warning("Encrypt requested XPath '" + elementXpath.getExpression() + "'" + " but no child nodes exist, skipping encryption");
                    }

                    if ( envelopeProcessed ) {
                        preconditionMatched = true;
                    } else if ( preconditionXpath == null ) {
                        // It's probably the Body or somesuch
                        preconditionMatched = true;
                    } else if (!preconditionMatched ) {
                        // Retry the precondition check now that we've decrypted
                        List nodes = XpathEvaluator.newEvaluator(document, preconditionXpath.getNamespaces()).select(preconditionXpath.getExpression());
                        if (!nodes.isEmpty())
                            preconditionMatched = true;
                    }
                }

                if (!preconditionMatched) {
                    String msg = "Operation did not match, even after decryption.  Ignoring Security element for message.";
                    logger.info( msg );
                    continue;
                }

                // Bug 838: Precondition must have matched; make sure these two exceptions get propagated
                if ( signatureNotFoundException != null )
                    return Result.policyViolation(signatureNotFoundException);
                if ( xmlSecurityElementNotFoundException != null )
                    return Result.policyViolation(xmlSecurityElementNotFoundException);

                logger.fine("response message element decrypted");
            }

            return preconditionMatched ?
                    Result.ok(document, documentCertificates) :
                    Result.notApplicable();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return Result.error( e );
        }
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