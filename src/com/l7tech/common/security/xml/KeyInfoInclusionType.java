/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.xml;

import java.util.EnumSet;

/**
 * @author alex
*/
public enum KeyInfoInclusionType {
    /**
     * No certificate information should be included
     */
    NONE,

    /**
     * //KeyInfo/X509Data/X509Certificate (the whole cert)
     */
    CERT,

    /**
     * //KeyInfo/SecurityTokenReference/KeyIdentifier[@valueType="...#ThumbprintSHA1] (the cert's thumbprint)
     */
    STR_THUMBPRINT,

    /**
     * //KeyInfo/SecurityTokenReference/KeyIdentifier[@valueType="...#X509SubjectKeyIdentifier"] (the cert's SKI)
     */
    STR_SKI,

    /**
     * //KeyInfo/X509Data/X509IssuerSerial (issuer DN &amp; serial number)
     */
    ISSUER_SERIAL;

    public static final EnumSet<KeyInfoInclusionType> CERT_ONLY = EnumSet.of(CERT);
    public static final EnumSet<KeyInfoInclusionType> CERT_OR_SKI = EnumSet.of(CERT, STR_SKI);
}
