/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 11:52:37 AM
 */
package com.l7tech.common.security.xml;

import org.w3c.dom.Document;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * The class security processor handles element processsing for
 * client and server side.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class SecurityProcessor {
    /**
     * the reference prefix for signature
     */
    protected static final String SIGN_REFERENCE = "signref";
    /**
     * the reference prefix for encryption
     */
    protected static final String ENC_REFERENCE = "encref";

    ElementSecurity[] elements = new ElementSecurity[]{};
    Document inputDocument;
    Document outputDocument;

    /**
     * Protected constructor accepting the security elements.
     * The class cannot be directly instantiated. Use one
     * of the factory methods.
     */
    protected SecurityProcessor(ElementSecurity[] elements) {
        if (elements != null) {
            this.elements = elements;
        }
    }

    /**
     * Factory method that creates the <code>SenderXmlSecurityProcessor</code> security processor.
     *
     * @param session  contains sequence generator, key for encrypting elements
     * @param si       the signer info (private key, certificate)
     * @param key      the encryption key
     * @param elements the array of security elements describing the processing
     *                 rules.
     * @return the signer security processor
     */
    public static SecurityProcessor getSigner(Session session, SignerInfo si, Key key, ElementSecurity[] elements) {
        return new SenderXmlSecurityProcessor(si, session, key, elements);
    }

    /**
     * Factory method that creates the <code>ReceiverXmlSecurityProcessor</code> security processor.
     *
     * @param session  contains sequence generator, key for encrypting elements
     * @param key      the decryption key (optional value)
     * @param elements the array of security elements describing the processing
     *                 rules.
     * @return the signer security processor
     */
    public static SecurityProcessor getVerifier(Session session, Key key, ElementSecurity[] elements) {
        return new ReceiverXmlSecurityProcessor(session, key, elements);
    }


    /**
     * Process the document according to the security rules. The input document
     * is left unchanged, that is, first the copy of the document is created and
     * the security is applied agains that document.
     *
     * @param document the input document otp process
     * @return the security processor result {@link SecurityProcessor.Result}
     * @throws GeneralSecurityException   on security error such as unknown
     *                                    algorithm etc. The nature of the error is subclass
     * @throws IOException                on io error such as xml processing
     * @throws SecurityProcessorException thrown on errors detected
     *                                    during element processing such as invalid or missing
     *                                    security properties, XPath error etc.
     */
    public Result process(Document document)
      throws SecurityProcessorException, GeneralSecurityException, IOException {
        if (document == null) {
            throw new IllegalArgumentException();
        }
        Document securedDocument = (Document)document.cloneNode(true);
        return processInPlace(securedDocument);


    }

    /**
     * Process the document according to the security rules. The document is processed
     * in place.
     *
     * @param document the input document otp process
     * @return the security processor result {@link SecurityProcessor.Result}
     * @throws GeneralSecurityException   on security error such as unknown
     *                                    algorithm etc. The nature of the error is subclass
     * @throws IOException                on io error such as xml processing
     * @throws SecurityProcessorException thrown on errors detected
     *                                    during element processing such as invalid or missing
     *                                    security properties, XPath error etc.
     */
    abstract public Result processInPlace(Document document)
      throws SecurityProcessorException, GeneralSecurityException, IOException;

    /**
     * The class represents the result of the security operation
     */
    public static class Result {
        private Document document;
        private X509Certificate[] certificateChain;

        /**
         * create the result instance with the resulting document and
         * certificate
         *
         * @param document the document, result of the processing
         * @param certificateChain the certificate that was associated with the operation
         */
        Result(Document document, X509Certificate[] certificateChain) {
            this.document = document;
            this.certificateChain = certificateChain;
        }

        /** @return the result document */
        public Document getDocument() {
            return document;
        }

        /**
         * The certificate chain that is associated with the operation. In the case
         * of signing this will contain the signing certs, in the verify case
         * it contains the extracted certs
         *
         * @return the certificate chain that was associated with the operaiton
         */
        public X509Certificate[] getCertificateChain() {
            return certificateChain;
        }
    }
    /**
     * Check whether the element security properties are supported
     *
     * @param elementSecurity the security element to verify
     * @throws NoSuchAlgorithmException on unsupported algorithm
     * @throws GeneralSecurityException on security properties invalid
     */
    protected void check(ElementSecurity elementSecurity)
      throws NoSuchAlgorithmException, GeneralSecurityException {
        if (!"AES".equals(elementSecurity.getCipher()))
            throw new NoSuchAlgorithmException("Unable to encrypt request: unsupported cipher: " + elementSecurity.getCipher());
        if (128 != elementSecurity.getKeyLength())
            throw new SecurityException("Unable to encrypt request: unsupported key length: " + elementSecurity.getKeyLength());
    }
}