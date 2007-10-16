/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.saml;

/**
 * @author alex
*/
public enum KeyInfoInclusionType {
    /**
     * No certificate information should be included
     */
    NONE,

    /**
     * //KeyInfo/X509Data/X509Certificate (the whole subject cert)
     */
    CERT,

    /**
     * //KeyInfo/SecurityTokenReference/KeyIdentifier[valueType="...#ThumbprintSHA1] (the cert's thumbprint)
     */
    STR_THUMBPRINT,

    /**
     * //KeyInfo/SecurityReference/KeyIdentifier[format="...#X509SubjectKeyIdentifier"] (the cert's SKI)
     */
    STR_SKI
}
