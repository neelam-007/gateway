/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 12:18:20 PM
 */
package com.l7tech.policy.assertion.xmlsec;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.util.XmlUtil;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * The signer is the <code>SecurityProcessor</code> implementation
 * that encapsulates the message signing and optional encryption.
 * This class is shared by client and the server. It cannot be directly
 * instantiated (it is a package private). Use the <code>SecurityProcessor</code>
 * factory method to obtain an instance.
 * @see {@link SecurityProcessor#getSigner(com.l7tech.common.security.xml.Session,
 *                                         com.l7tech.common.security.xml.SignerInfo,
 *                                         java.security.Key,
 *                                         com.l7tech.policy.assertion.xmlsec.ElementSecurity[])}
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
class Signer extends SecurityProcessor {
    static Logger logger = Logger.getLogger(Signer.class.getName());
    private SignerInfo signerInfo;
    private Session session;
    private Key encryptionKey;

    /**
     * Create the new instance with the signer information, session, optional
     * encryption key and the security elements.
     *
     * @param si       the signer information (private key, and certificate)
     * @param session  the sesion
     * @param key      the optional encryption key. May be null, that means
     *                 no encryption. The <code>KeyException</code> is trown
     *                 if the encryption is requested.
     * @param elements the security elements
     */
    Signer(SignerInfo si, Session session, Key key, ElementSecurity[] elements) {
        super(elements);
        this.signerInfo = si;
        this.session = session;
        this.encryptionKey = key;
        if (si == null || session == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Process the document according to the security rules. The rules are applied
     * directly on the document.
     *
     * @param document the document to sign
     * @return the security processor result {@link SecurityProcessor.Result}
     * @throws SecurityProcessorException
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public Result processInPlace(Document document)
      throws SecurityProcessorException, GeneralSecurityException, IOException {
        if (document == null) {
            throw new IllegalArgumentException();
        }
        try {
            long sessId = session.getId();
            long nextSequence = session.nextSequenceNumber();
            // document copy, clone document
            SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(document, sessId, nextSequence);
            int encReferenceIdSuffix = 1;
            int signReferenceIdSuffix = 1;
            boolean envelopeProcessed = false;
            SoapMsgSigner dsigHelper = new SoapMsgSigner();

            for (int i = 0; i < elements.length && !envelopeProcessed; i++) {
                ElementSecurity elementSecurity = elements[i];
                // XPath precondition match?
                XpathExpression xpath = elementSecurity.getPreconditionXPath();
                if (xpath != null) {
                    List nodes = XpathEvaluator.newEvaluator(document, xpath.getNamespaces()).select(xpath.getExpression());
                    if (nodes.isEmpty()) {
                        logger.fine("The XPath precondition result is empty '" + xpath.getExpression() + "' skipping");
                        continue;
                    }
                }

                Element element = null;
                xpath = elementSecurity.getxPath();
                if (xpath != null) {
                    List nodes = XpathEvaluator.newEvaluator(document, xpath.getNamespaces()).select(xpath.getExpression());
                    if (nodes.isEmpty()) {
                        final String message = "The XPath result is empty '" + xpath.getExpression() + "'";
                        String logmessage = message +"\nMessage is\n"+XmlUtil.documentToString(document);
                        logger.warning(logmessage);
                        throw new SecurityProcessorException(message);
                    }
                    element = (Element)nodes.get(0);
                    if (element.equals(document.getDocumentElement())) {
                        envelopeProcessed = true; //signal to ignore everything else. Should scream if more eleemnts exist?
                    }
                } else {
                    element = document.getDocumentElement();
                    envelopeProcessed = true; //signal to ignore everything else. Should scream if more lements exist?
                }
                if (elementSecurity.isEncryption()) {
                    check(elementSecurity);
                    // we do above check (ported from original impl), however the key has already been produced.
                    // it will meake more sense once different keys will be supported
                    final String referenceId = ENC_REFERENCE + encReferenceIdSuffix;
                    byte[] keyreq = encryptionKey.getEncoded();
                    XmlMangler.encryptXml(element, keyreq, Long.toString(sessId), referenceId);
                    ++encReferenceIdSuffix;
                    logger.fine("encrypted element for XPath" + xpath.getExpression());
                }
                // dsig
                final String referenceId = SIGN_REFERENCE + signReferenceIdSuffix;
                dsigHelper.signElement(document, element, referenceId, signerInfo.getPrivate(), signerInfo.getCertificate());
                ++signReferenceIdSuffix;
                logger.fine("signed element for XPath " + xpath.getExpression());
            }
            return new Result(document, signerInfo.getCertificate());
        } catch (SignatureStructureException e) {
            SignatureException se = new  SignatureException("Signing error");
            se.initCause(e);
            throw se;
        } catch (XSignatureException e) {
            SignatureException se = new  SignatureException("Signing error");
            se.initCause(e);
            throw se;
        } catch (JaxenException e) {
            throw new SecurityProcessorException("XPath error", e);
        }
    }

    /**
     * Check whether the element security properties are supported
     *
     * @param elementSecurity the security element to verify
     * @throws java.security.NoSuchAlgorithmException
     *                           on unsupported algorithm
     * @throws GeneralSecurityException on security properties invalid
     */
    protected void check(ElementSecurity elementSecurity)
      throws NoSuchAlgorithmException, GeneralSecurityException {
        super.check(elementSecurity);
        // need to check for key null
        if (elementSecurity.isEncryption() && encryptionKey == null) {
            // may be an IllegalState too - em
            throw new KeyException("null encryption key, and encryption requested");
        }
    }
}