/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 2:40:10 PM
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.*;
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
import java.util.List;
import java.util.logging.Logger;

class Verifier extends SecurityProcessor {
    static Logger logger = Logger.getLogger(Verifier.class.getName());

    private Key decryptionKey;
    private Session session;

    /**
     * Create the new instance with the signer information, session, optional
     * encryption key and the security elements.
     *
     * @param session  the sesion
     * @param key      the optional encryption key. May be null, that means
     *                 no encryption. The <code>KeyException</code> is trown
     *                 if the encryption is requested.
     * @param elements the security elements
     */
    Verifier(Session session, Key key, ElementSecurity[] elements) {
        super(elements);
        if (session == null) {
            throw new IllegalArgumentException();
        }
        this.session = session;
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
            X509Certificate documentCertificate = null;
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
                            envelopeProcessed = true; //signal to ignore everything else. Should scream if more elemnts exist?
                        }
                    } else {
                        element = document.getDocumentElement();
                        envelopeProcessed = true; //signal to ignore everything else. Should scream if more elements exist?
                    }
                // verifiy element signature
                SoapMsgSigner dsigHelper = new SoapMsgSigner();

                // verify that this cert is signed with the root cert of this ssg
                documentCertificate = dsigHelper.validateSignature(document, element);
                logger.fine("signature of response message verified");


                if (elementSecurity.isEncryption()) { //element security is required
                    check(elementSecurity);
                    XmlMangler.decryptXml(document, decryptionKey);
                }
                logger.fine("response message element decrypted");
            }
            // clean empty security element and header if necessary
            SoapUtil.cleanEmptySecurityElement(document);
            SoapUtil.cleanEmptyHeaderElement(document);

            return new Result(document, documentCertificate);
        } catch (JaxenException e) {
            throw new SecurityProcessorException("XPath error", e);
        } catch (SignatureNotFoundException e) {
            SignatureException se = new  SignatureException("Signature not found");
            se.initCause(e);
            throw se;
        } catch (InvalidSignatureException e) {
            SignatureException se = new  SignatureException("Invalid signature");
            se.initCause(e);
            throw se;
        } catch (ParserConfigurationException e) {
            throw new SecurityProcessorException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new SecurityProcessorException(e.getMessage(), e);
        } catch (XMLSecurityElementNotFoundException e) {
            throw new SecurityProcessorException(e.getMessage(), e);
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
        if (elementSecurity.isEncryption() && decryptionKey == null) {
            // may be an IllegalState too - em
            throw new KeyException("null decryption key, and decryption requested");
        }
    }
}