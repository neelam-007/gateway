/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.token;

import org.w3c.dom.Element;

/**
 * @author mike
 */
public interface SignedElement extends ParsedElement {

    /**
     * Get the signing security token for this element.
     *
     * @return either a X509SecurityToken or a DerivedKeyToken
     */
    SigningSecurityToken getSigningSecurityToken();

    /**
     * Get the DOM Element of the Signature for this element.
     *
     * @return the signature Element.
     */
    Element getSignatureElement();

    /**
     * Get the signing algorithm used with this signed element.
     *
     * @return the signature algorithm identifier
     */
    String getSignatureAlgorithmId();

    /**
     * Get the digest algorithms used for this signed element.
     *
     * @return the digest algorithm identifiers
     */
    String[] getDigestAlgorithmIds();
}
