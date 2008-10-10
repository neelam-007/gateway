package com.l7tech.security.token;

/**
 * Represents a SignatureConfirmation element.
 */
public interface SignatureConfirmation extends ParsedElement {
    /**
     * @return the still-base64-encoded SignatureConfirmation value of the SignatureValue being confirmed.
     */
    String getConfirmationValue();
}
