package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
 * @see {@link SecurityProcessor#getSigner(com.l7tech.common.security.xml.Session,
  *      com.l7tech.common.security.xml.SignerInfo,
  *      java.security.Key,
  *      com.l7tech.common.security.xml.ElementSecurity[])}
 */
class SenderXmlSecurityProcessor extends SecurityProcessor {
    static Logger logger = Logger.getLogger(SenderXmlSecurityProcessor.class.getName());
    private SignerInfo signerInfo;
    private Session session;
    private Key encryptionKey;

    /**
     * Create the new instance with the signer information, session, optional
     * encryption key and the security elements.
     *
     * @param si       the signer information (private key, and certificate)
     * @param session  the sesion, this may be <b>null</b> in case simple sessionless
     *                 signing is requested
     * @param key      the optional encryption key. May be null, that means
     *                 no encryption. The <code>KeyException</code> is trown
     *                 if the encryption is requested.
     * @param elements the security elements
     */
    SenderXmlSecurityProcessor(SignerInfo si, Session session, Key key, ElementSecurity[] elements) {
        super(elements);
        this.signerInfo = si;
        this.session = session;
        this.encryptionKey = key;
        if (si == null) {
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
     * @throws GeneralSecurityException if unsupported key length or algorithm, or miscellaneous DOM trouble
     * @throws IOException
     */
    public Result processInPlace(Document document)
      throws SecurityProcessorException, GeneralSecurityException, IOException {
        if (document == null) {
            throw new IllegalArgumentException();
        }
        try {
            int encReferenceIdSuffix = 1;
            int signReferenceIdSuffix = 1;
            boolean preconditionMatched = false;

            // Process operations with encryption first
            List deferred = new LinkedList(); // defer processing of sign-only ElementSecurity
            for (int i = 0; i < elements.length; i++) {
                ElementSecurity elementSecurity = elements[i];

                if (!elementSecurity.isEncryption()) {
                    // Defer this operation until all crypto guys are done
                    deferred.add(elementSecurity);
                    continue;
                }

                preconditionMatched = preconditionMatches(elementSecurity, document);
                if (!preconditionMatched)
                    continue; // skip: there was a precondition and it failed

                FoundElement foundElement = findXpathElement(document, elementSecurity);
                Element element = foundElement.found;
                XpathExpression elementXpath = foundElement.expression;

                // Do encryption, since it is now known that isEncryption() is true for this ElementSecurity
                if (element.hasChildNodes()) {
                    check(elementSecurity);
                    Element encElement = element;
                    if (element == document.getDocumentElement()) {
                        encElement = SoapUtil.getBody(document);
                        if (encElement == null) {
                            logger.severe("Could not retrieve SOAP Body from the document \n" + XmlUtil.documentToString(document));
                            throw new IOException("Could not retrieve SOAP Body from the document");
                        }
                    }
                    // we do above check to verify if the parameters are valid and everything is ready for encryption
                    final String referenceId = ENC_REFERENCE + encReferenceIdSuffix;
                    byte[] keyreq = encryptionKey.getEncoded();
                    long sessId = session.getId();
                    XmlMangler.encryptXml(encElement, keyreq, Long.toString(sessId), referenceId);
                    ++encReferenceIdSuffix;
                    logger.fine("encrypted element for XPath" + elementXpath.getExpression());
                } else {
                    logger.warning("Encrypt requested XPath '" + elementXpath.getExpression() + "'" + " but no child nodes exist, skipping encryption");
                }

                signReferenceIdSuffix = doSignElement(signReferenceIdSuffix, document, element, elementXpath);
            }

            // Then, go back and do the signing-only operations
            for (Iterator i = deferred.iterator(); i.hasNext();) {
                ElementSecurity elementSecurity = (ElementSecurity)i.next();

                //envelopeProcessed = ElementSecurity.isEnvelope(elementSecurity); // todo: what is this for?
                preconditionMatched = preconditionMatches(elementSecurity, document);
                if (!preconditionMatched)
                    continue; // skip: there was a precondition and it failed

                FoundElement foundElement = findXpathElement(document, elementSecurity);
                Element element = foundElement.found;
                XpathExpression elementXpath = foundElement.expression;

                signReferenceIdSuffix = doSignElement(signReferenceIdSuffix, document, element, elementXpath);
            }

            return preconditionMatched
                        ? Result.ok(document, signerInfo.getCertificateChain())
                        : Result.notApplicable();
            
        } catch (SignatureStructureException e) {
            SignatureException se = new SignatureException("Signing error");
            se.initCause(e);
            throw se;
        } catch (XSignatureException e) {
            SignatureException se = new SignatureException("Signing error");
            se.initCause(e);
            throw se;
        } catch (JaxenException e) {
            throw new SecurityProcessorException("XPath error", e);
        } catch ( SoapUtil.MessageNotSoapException e ) {
            throw new SecurityProcessorException("Can't encrypt or sign a non-SOAP message", e);
        }
    }

    private int doSignElement(int signId, Document document, Element element, XpathExpression elementXpath)
            throws SignatureStructureException, XSignatureException, SoapUtil.MessageNotSoapException {
        // dsig
        final String referenceId = SIGN_REFERENCE + signId;
        SoapMsgSigner.signElement(document, element, referenceId, signerInfo.getPrivate(), signerInfo.getCertificateChain());
        ++signId;
        logger.fine("signed element for XPath " + elementXpath.getExpression());
        return signId;
    }

    private static final class FoundElement {
        private FoundElement(XpathExpression expression, Element found) {
            this.expression = expression;
            this.found = found;
        }
        private final XpathExpression expression;
        private final Element found;
    }

    private FoundElement findXpathElement(Document document, ElementSecurity elementSecurity) throws JaxenException, IOException, SecurityProcessorException {
        XpathExpression elementXpath = elementSecurity.getElementXpath();
        if (elementXpath != null) {
            List nodes = XpathEvaluator.newEvaluator(document, elementXpath.getNamespaces()).select(elementXpath.getExpression());
            if (nodes.isEmpty()) {
                final String message = "The XPath result is empty '" + elementXpath.getExpression() + "'";
                String logmessage = message + "\nMessage is\n" + XmlUtil.documentToString(document);
                logger.warning(logmessage);
                throw new SecurityProcessorException(message);
            }
            Object o = nodes.get(0);
            if ( o instanceof Element )
                return new FoundElement(elementXpath, (Element)o);

            final String message = "The XPath query resulted in something other than a single element '" + elementXpath.getExpression() + "'";
            logger.warning(message);
            throw new SecurityProcessorException(message);
        } else {
            return new FoundElement(elementXpath, document.getDocumentElement());
        }
    }

    /** @return TRUE if the xpath precondition matched; FALSE if it didn't; null if there was no precondition. */
    private static boolean preconditionMatches(ElementSecurity elementSecurity, Document document) throws JaxenException {
        XpathExpression xpath = elementSecurity.getPreconditionXpath();
        if (xpath == null)
            return true;
        List nodes = XpathEvaluator.newEvaluator(document, xpath.getNamespaces()).select(xpath.getExpression());
        if (nodes.isEmpty())
            logger.fine("The XPath precondition result is empty '" + xpath.getExpression() + "' skipping");
        return !nodes.isEmpty();
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
        if (elementSecurity.isEncryption()) {
            if (encryptionKey == null) {
                throw new KeyException("null encryption key, and encryption requested");
            }
            if (session == null) {
                throw new GeneralSecurityException("Could not encrypt response because session was not provided by requestor.");
            }
        }
    }
}