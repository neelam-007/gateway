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
import java.security.cert.X509Certificate;

/**
 * The class security processor handles element processsing for
 * client and server side.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class SecurityProcessor {
    ElementSecurity[] elements = new ElementSecurity[]{};

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
     * @param si       the signer info (private key, certificate) to user for decorating messages
     * @param recipientCertificate  the certificate of the recipient.  may be null if no encryption will be done.
     * @param elementsToDecorate the array of security elements describing the processing
     *                           rules that will be performed upon outgoing requests.
     * @return the Sender's XML security processor
     */
    public static SecurityProcessor createSenderSecurityProcessor(SignerInfo si,
                                                                  X509Certificate recipientCertificate,
                                                                  ElementSecurity[] elementsToDecorate) {
        return new SenderXmlSecurityProcessor(si, recipientCertificate, elementsToDecorate);
    }

    /**
     * Factory method that creates the <code>ReceiverXmlSecurityProcessor</code> security processor.
     *
     * @param processorResult    the record of elements which were verified to be properly signed/encrypted
     *                           when the request arrived.  This processing must already have been done
     *                           by the caller.
     * @param elementsToCheck    the array of security elements describing the processing
     *                           rules that will be enforced by inspection of the processorResult.
     * @return the Receiver's XML security processor
     */
    public static SecurityProcessor createRecipientSecurityProcessor(WssProcessor.ProcessorResult processorResult,
                                                                     ElementSecurity[] elementsToCheck) {
        return new ReceiverXmlSecurityProcessor(processorResult, elementsToCheck);
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
        public static class Type {
            public static Type OK = new Type(0, "Ok");
            public static Type NOT_APPLICABLE = new Type(1, "Assertion is not applicable to this request");
            public static Type POLICY_VIOLATION = new Type(2, "This request violates the policy. Update policy and retry");
            public static Type ERROR = new Type(3, "This request is erroneous and should not be retried");

            public final int code;
            public final String desc;

            private Type(int code, String desc) {
                this.code = code;
                this.desc = desc;
            }
        }

        private final Document document;
        private final Type type;
        private final X509Certificate[] certificateChain;
        private final Throwable throwable;

        static Result ok(Document document, X509Certificate[] certificateChain) {
            return new Result(document, Type.OK, certificateChain, null);
        }

        static Result error(Throwable throwable) {
            return new Result(null, Type.ERROR, null, throwable );
        }

        static Result policyViolation(Throwable throwable) {
            return new Result(null, Type.POLICY_VIOLATION, null, throwable);
        }

        static Result notApplicable() {
            return new Result(null, Type.NOT_APPLICABLE, null, null);
        }

        /**
         * create the result instance with the resulting document and
         * certificate
         *
         * @param document the document, result of the processing
         * @param type the type of the indicates whether this request satisfied the Xpath precondition and therefore whether the certificateChain should be non-null
         * @param certificateChain the certificate that was associated with the operation, or null if the request did not satisfy the Xpath precondition
         * @param throwable the throwable responsible for this result, if any.
         *
         */
        Result(Document document, Type type, X509Certificate[] certificateChain, Throwable throwable ) {
            this.document = document;
            this.certificateChain = certificateChain;
            this.type = type;
            this.throwable = throwable;
        }

        /** @return the result document */
        public Document getDocument() {
            return document;
        }

        /** @return a {@link Type} instance indicating what type of result this is */
        public Type getType() {
            return type;
        }

        /** @return the Throwable responsible for this result, if any. */
        public Throwable getThrowable() {
            return throwable;
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
}