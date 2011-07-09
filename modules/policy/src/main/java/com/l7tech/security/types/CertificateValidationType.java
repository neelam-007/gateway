package com.l7tech.security.types;

import javax.xml.bind.annotation.XmlEnumValue;

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
    @XmlEnumValue( "Validate" ) CERTIFICATE_ONLY,

    /**
     * In addition to checking the cert's validity and signature, we will build a path to a Trust Anchor
     */
    @XmlEnumValue( "Validate Certificate Path" ) PATH_VALIDATION,

    /**
     * In addition to checking the cert's validity and signature and building a path to a Trust Anchor, we will
     * check it for revocation.
     */
    @XmlEnumValue( "Revocation Checking" ) REVOCATION,
    ;
}
