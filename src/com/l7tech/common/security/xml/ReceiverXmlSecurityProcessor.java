/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 2:40:10 PM
 */
package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

class ReceiverXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = Logger.getLogger(ReceiverXmlSecurityProcessor.class.getName());

    private Key decryptionKey;

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
    ReceiverXmlSecurityProcessor(Session session, Key key, ElementSecurity[] elements) {
        super(elements);
        this.decryptionKey = key;
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
    public Result processInPlace(Document document)
      throws SecurityProcessorException, GeneralSecurityException, IOException {
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
                    if (nodes.isEmpty()) {
                        if (!preconditionMatched) continue; // Request wasn't encrypted; it's just a non-matching request
                        final String message = "The XPath result is empty '" + elementXpath.getExpression() + "'";
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

                SignatureNotFoundException signatureNotFoundException = null;
                XMLSecurityElementNotFoundException xmlSecurityElementNotFoundException = null;

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
                if ( signatureNotFoundException != null ) throw signatureNotFoundException;
                if ( xmlSecurityElementNotFoundException != null ) throw xmlSecurityElementNotFoundException;

                logger.fine("response message element decrypted");
            }

            return new Result(document, preconditionMatched, documentCertificates);
        } catch (JaxenException e) {
            throw new SecurityProcessorException("XPath error", e);
        } catch (SignatureNotFoundException e) {
            SignatureException se = new SignatureException("Signature not found");
            se.initCause(e);
            throw se;
        } catch (InvalidSignatureException e) {
            SignatureException se = new SignatureException("Invalid signature");
            se.initCause(e);
            throw se;
        } catch (ParserConfigurationException e) {
            throw new SecurityProcessorException("Xml parser configuration error", e);
        } catch (SAXException e) {
            throw new SecurityProcessorException("Xml parser error", e);
        } catch (XMLSecurityElementNotFoundException e) {
            throw new SecurityProcessorException("Request does not contain security element", e);
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