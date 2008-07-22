/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.types;

/**
 * CertificateValidationType enumeration.
 *
 * <p>Items are ordered from least to most secure.</p>
 *
 * @author alex
*/
public enum CertificateValidationType {
    /**
     * The cerficate's validity and signature will be checked
     */
    CERTIFICATE_ONLY,

    /**
     * In addition to checking the cert's validity and signature, we will build a path to a Trust Anchor
     */
    PATH_VALIDATION,

    /**
     * In addition to checking the cert's validity and signature and building a path to a Trust Anchor, we will
     * check it for revocation.
     */
    REVOCATION,
    ;
}
