/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.token;

import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Calendar;

/**
 * Represents a saml:Assertion XML security token.
 */
public interface SamlSecurityToken extends X509SigningSecurityToken {
    ConfirmationMethod HOLDER_OF_KEY = new ConfirmationMethod("Holder-of-key");
    ConfirmationMethod SENDER_VOUCHES = new ConfirmationMethod("Sender-vouches");
    ConfirmationMethod BEARER_TOKEN = new ConfirmationMethod("Bearer token");
    int VERSION_1_1 = 1;
    int VERSION_2_0 = 2;

    int getVersionId();

    X509Certificate getSubjectCertificate();

    boolean isHolderOfKey();

    boolean isSenderVouches();

    /** @return true if this assertion uses bearer token subject confirmation, or has no subject confirmation at all */
    boolean isBearerToken();

    /**
     * @return the actual Confirmation Method used by this assertion, or null if it didn't have one.
     */
    ConfirmationMethod getConfirmationMethod();

    String getAssertionId();

    /**
     * Get a unique identifier for this assertion.
     *
     * @return The unique identifier for this assertion
     */
    String getUniqueId();

    boolean hasEmbeddedIssuerSignature();

    /**
     * Check embededded signature of this saml assertion.  May only be called if {@link #hasEmbeddedIssuerSignature()}  returns true.
     * */
    void verifyEmbeddedIssuerSignature() throws SignatureException;

    /** @return the name identifier format, or null if there wasn't one. {@link com.l7tech.security.saml.SamlConstants SamlConstants} */
    String getNameIdentifierFormat();

    /** @return the name qualifier, or null if there wasn't one. {@link com.l7tech.security.saml.SamlConstants SamlConstants} */
    String getNameQualifier();

    /** @return the name identifier value, or null if there wasn't one. {@link com.l7tech.security.saml.SamlConstants SamlConstants} */
    String getNameIdentifierValue();

    /** @return the authentication method, or null if there wasn't one. {@link com.l7tech.security.saml.SamlConstants SamlConstants} */
    String getAuthenticationMethod();

    boolean isOneTimeUse();

    /** @return the IssueInstant, or null if there wasn't one. */
    Calendar getIssueInstant();

    /** @return the NotBefore date in the Conditions, or null if there wasn't one. */
    Calendar getStarts();

    /** @return the NotOnOrAfter date in the Conditions, or null if there wasn't one. */
    Calendar getExpires();

    /**
     * Check if this assertion has either already expired, or is set to expire within the next preexpireSec seconds.
     *
     * @param preexpireSec number of seconds into the future for which the assertion should remain valid.
     * @return true if this assertion has not yet expired, and will not expire for at least another preexpireSec
     *         seconds.
     */
    boolean isExpiringSoon(int preexpireSec);

    public static final class ConfirmationMethod {
        private final String name;
        private ConfirmationMethod(String name) { this.name = name; }
        public String toString() { return name; }
    }
}
