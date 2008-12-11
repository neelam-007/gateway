/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import java.util.EnumSet;

/**
 * Used to specify how KeyInfo elements should be generated, namely {@link #NONE not at all}, {@link #CERT containing
 * the whole certificate}, or using a SecurityTokenReference containing either a {@link #STR_SKI SKI}, an
 * {@link #STR_THUMBPRINT SHA-1 Thumbprint} or an {@link #ISSUER_SERIAL X509IssuerSerial} structure.
*/
public enum KeyInfoInclusionType {
    /**
     * No certificate information should be included
     */
    NONE,

    /**
     * //KeyInfo/X509Data/X509Certificate
     * ...or sometimes...
     * //KeyInfo/SecurityTokenReference/Reference[@URI="#someBst"]
     */
    CERT,

    /**
     * //KeyInfo/SecurityTokenReference/KeyIdentifier[@valueType="...#ThumbprintSHA1] (the cert's thumbprint)
     */
    STR_THUMBPRINT,

    /**
     * //KeyInfo/SecurityTokenReference/KeyIdentifier[@valueType="...#X509SubjectKeyIdentifier"] (the cert's SKI)
     * ...or sometimes...
     * //KeyInfo/SecurityTokenReference/X509Data/X509SKI (the cert's SKI)
     */
    STR_SKI,

    /**
     * //KeyInfo/SecurityTokenReference/X509Data/X509IssuerSerial (issuer DN &amp; serial number)
     * ...or sometimes...
     * //KeyInfo/X509Data/X509IssuerSerial (issuer DN &amp; serial number)
     */
    ISSUER_SERIAL;

    public static final EnumSet<KeyInfoInclusionType> CERT_ONLY = EnumSet.of(CERT);
    public static final EnumSet<KeyInfoInclusionType> CERT_OR_SKI = EnumSet.of(CERT, STR_SKI);
    public static final EnumSet<KeyInfoInclusionType> ANY = EnumSet.of(ISSUER_SERIAL, STR_SKI, STR_THUMBPRINT, CERT);
}
