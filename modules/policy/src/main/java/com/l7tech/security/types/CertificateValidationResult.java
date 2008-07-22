/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.security.types;

/**
 * @author alex
*/
public enum CertificateValidationResult {
    /**
     * The system was unable to build a CertPath to a Trust Anchor
     */
    CANT_BUILD_PATH,

    /**
     * Known to be revoked
     */
    REVOKED,

    /**
     * Valid according to the requested policy
     */
    OK,

    /**
     * Path validation succeeded, but revocation status could not be determined
     */
    UNKNOWN,
    ;
}
