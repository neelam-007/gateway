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
}
