/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

/**
 * 
 */
public interface ResponseWssConfig extends SecurityHeaderAddressable {
    public String getKeyReference();
    public void setKeyReference(String keyReference);

    boolean isProtectTokens();

    /**
     * If true, the token on which the signature is based will be included in the signature if it is
     * present in the message.  This is similar to the semantics of the sp:ProtectTokens assertion in WS-SecurityPolicy.
     *
     * @param protectTokens true to sign the BST (or other signing token); false to leave it unsigned.
     */
    void setProtectTokens(boolean protectTokens);
}
