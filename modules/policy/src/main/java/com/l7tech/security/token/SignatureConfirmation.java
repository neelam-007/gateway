package com.l7tech.security.token;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;


/**
 * WSS Processor's result for SignatureConfirmation.
 */
public interface SignatureConfirmation {

    enum Status {
        /**
         * Success state, no SignatureConfirmation elements found and not expected
         */
        NO_CONFIRMATION,

        /**
         * SignatureConfirmations successfully validated, results available through getConfirmedValues()
         */
        CONFIRMED,
        /**
         * SignatureConfirmation validation failed, errors recorded and available through getErrors()
         */
        INVALID
    }

    /**
     * Records SignatureConfirmation elements discovered by the WSS Processor.
     */
    void addConfirmationElement(Element e, boolean strict);

    public Map<String, ParsedElement> getConfirmationElements();

    /**
     * @return the Element that contained the provided @param signatureConfirmation
     */
    Element getElement(String signatureConfirmation);

    /**
     * Records the signing tokens which signed the validated signature confirmations.
     */
    public void addSignedElement(String signatureConfirmation, Set<SignedElement> newSigned);

    /**
     * @return true if a value-less SignatureConfirmation was found in the message, false otherwise
     */
    public boolean hasNullValue();

    /**
     * Records a SignatureConfirmation processing error.
     */
    void addError(String error);

    /**
     * @return a list of SignatureConfirmation processing errors
     */
    List<String> getErrors();

    /**
     * @return a list of successfully validated SignatureConfirmation values and their signing tokens
     */
    Map<String,List<SignedElement>> getConfirmedValues();

    /**
     * @return SignatureConfirmation validation status
     * @see {@link com.l7tech.security.token.SignatureConfirmation.Status}
     */
    Status getStatus();
}
