/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

/**
 * @author mike
 */
public interface SignedElement extends ParsedElement {
    /**
     * @return either a X509SecurityToken or a DerivedKeyToken
     */
    SecurityToken getSigningSecurityToken();
}
