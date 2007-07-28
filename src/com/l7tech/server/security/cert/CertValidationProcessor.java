/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.security.CertificateValidationResult;
import com.l7tech.common.security.CertificateValidationType;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.RevocationCheckPolicy;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.SignatureException;

/**
 * @author alex
 */
public interface CertValidationProcessor {
    /**
     * Note that the Facility names need to match the {@link com.l7tech.server.ServerConfig} property names
     * (except lower-cased) 
     */
    public static enum Facility {
        IDENTITY, ROUTING, OTHER
    }

    /**
     * @param validationType the type of validation to be performed, or null to use the system-wide default for the
     *        provided facility
     * @param facility the type of system component asking for the validation to be performed.  Must be supplied if
     *        validationType is null.
     */
    CertificateValidationResult check(X509Certificate cert,
                                      CertificateValidationType validationType,
                                      Facility facility,
                                      Auditor auditor)
            throws CertificateException, SignatureException;

    TrustedCert getTrustedCertByOid(long oid);

    TrustedCert getTrustedCertBySubjectDn(String subjectDn);

    TrustedCert getTrustedCertEntry(X509Certificate certificate);

    RevocationCheckPolicy getRevocationCheckPolicy(TrustedCert trustedCert);

    RevocationCheckPolicy getDefaultPolicy();

    RevocationCheckPolicy getPermissivePolicy();
}
