/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Calendar;

/**
 * Represents a saml:Assertion XML security token.
 */
public interface SamlSecurityToken extends SigningSecurityToken {
    ConfirmationMethod HOLDER_OF_KEY = new ConfirmationMethod("Holder-of-key");
    ConfirmationMethod SENDER_VOUCHES = new ConfirmationMethod("Sender-vouches");

    X509Certificate getSubjectCertificate();
    X509Certificate getIssuerCertificate();

    boolean isHolderOfKey();

    boolean isSenderVouches();

    /**
     * @return the actual Confirmation Method used by this assertion, or null if it didn't have one.
     */
    ConfirmationMethod getConfirmationMethod();

    String getAssertionId();

    boolean isSigned();

    /** Check signature of this saml assertion.  May only be called if isSigned() returns true. */
    void verifyIssuerSignature() throws SignatureException;

    /** @return the name identifier format, or null if there wasn't one. {@see SamlConstants} */
    String getNameIdentifierFormat();

    /** @return the name qualifier, or null if there wasn't one. {@see SamlConstants} */
    String getNameQualifier();

    /** @return the name identifier value, or null if there wasn't one. {@see SamlConstants} */
    String getNameIdentifierValue();

    /** @return the authentication method, or null if there wasn't one. {@see SamlConstants} */
    String getAuthenticationMethod();

    Calendar getExpires();

    /**
     * Check if this assertion has either already expired, or is set to expire within the next preexpireSec seconds.
     *
     * @param preexpireSec number of seconds into the future for which the assertion should remain valid.
     * @return true if this assertion has not yet expired, and will not expire for at least another preexpireSec
     *         seconds.
     */
    boolean isExpiringSoon(int preexpireSec);

    X509Certificate getSigningCertificate();

    public static final class ConfirmationMethod {
        private final String name;
        private ConfirmationMethod(String name) { this.name = name; }
        public String toString() { return name; }
    }
}
