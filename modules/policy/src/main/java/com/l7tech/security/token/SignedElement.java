/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.token;

/**
 * @author mike
 */
public interface SignedElement extends ParsedElement {
    /**
     * @return either a X509SecurityToken or a DerivedKeyToken
     */
    SigningSecurityToken getSigningSecurityToken();
}
