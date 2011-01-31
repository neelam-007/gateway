/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

/**
 * 
 */
public interface WssDecorationConfig extends SecurityHeaderAddressable {

    /**
     * Get the type of key reference to use.
     *
     * @return The key reference type.
     * @see com.l7tech.security.xml.KeyReference
     */
    String getKeyReference();

    /**
     * Set the type of key reference to use.
     *
     * @param keyReference The reference to use.
     * @see com.l7tech.security.xml.KeyReference
     */
    void setKeyReference(String keyReference);
    
    /**
     * Should the signing token be covered by the signature.
     *
     * @return True if the signing token should be protected.
     */
    boolean isProtectTokens();

    /**
     * If true, the token on which the signature is based will be included in the signature if it is
     * present in the message.  This is similar to the semantics of the sp:ProtectTokens assertion in WS-SecurityPolicy.
     *
     * @param protectTokens true to sign the BST (or other signing token); false to leave it unsigned.
     */
    void setProtectTokens(boolean protectTokens);

    /**
     * @return true if the value of {@link #isProtectTokens()} is to be used at runtime; false if it is to be ignored.
     */
    boolean isUsingProtectTokens();

    /**
     * Get the digest algorithm to use for the SignatureMethod and DigestMethod.
     *
     * @return the preferred digest algorithm name (ie, "SHA-384").  May be null.
     */
    String getDigestAlgorithmName();

    /**
     * Set the digest algorithm to use for the SignatureMethod and DigestMethod.
     *
     * @param digestAlgorithmName the digest algorithm name (ie, "SHA-1" or "SHA-512"), or null to allow the WSS decorator to choose.
     */
    void setDigestAlgorithmName(String digestAlgorithmName);
}
