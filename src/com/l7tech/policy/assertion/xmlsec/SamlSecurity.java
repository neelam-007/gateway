package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * The <code>SamlSecurity</code> assertion is used to specify that a request must include a SAML holder-of-key
 * assertion signed by a recognized token server.
 */
public class SamlSecurity extends CredentialSourceAssertion {
    public static final int CONFIRMATION_METHOD_HOLDER_OF_KEY = 0;
    public static final int CONFIRMATION_METHOD_SENDER_VOUCHES = 1;
    public static final int CONFIRMATION_METHOD_WHATEVER = 2;

    /**
     * Default constructor (sets confirmation method type as whatever)
     */
    public SamlSecurity() {
        this.confirmationMethodType = CONFIRMATION_METHOD_WHATEVER;
    }

    /**
     * Constructor that lets you spec the confirmation method
     * @param confirmationMethodType CONFIRMATION_METHOD_HOLDER_OF_KEY or CONFIRMATION_METHOD_SENDER_VOUCHES
     *                               or CONFIRMATION_METHOD_WHATEVER
     */
    public SamlSecurity(int confirmationMethodType) {
        if (confirmationMethodType < CONFIRMATION_METHOD_HOLDER_OF_KEY ||
            confirmationMethodType > CONFIRMATION_METHOD_WHATEVER) {
            throw new IllegalArgumentException("confirmationMethodType value of " + confirmationMethodType +
                                               " is invalid");
        }
        this.confirmationMethodType = confirmationMethodType;
    }

    public int getConfirmationMethodType() {
        return confirmationMethodType;
    }

    public void setConfirmationMethodType(int confirmationMethodType) {
        this.confirmationMethodType = confirmationMethodType;
    }

    private int confirmationMethodType;
}
