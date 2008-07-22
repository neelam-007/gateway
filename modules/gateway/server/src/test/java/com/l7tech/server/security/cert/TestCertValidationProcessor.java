package com.l7tech.server.security.cert;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.SignatureException;
import java.math.BigInteger;

import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.security.cert.TrustedCert;

/**
 * @author Steve Jones
 */
public class TestCertValidationProcessor implements CertValidationProcessor {

    public CertificateValidationResult check(X509Certificate[] certificatePath, CertificateValidationType minimumValidationType, CertificateValidationType requestedValidationType, Facility facility, Auditor auditor) throws CertificateException, SignatureException {
        return CertificateValidationResult.OK;
    }

    public X509Certificate getCertificateByIssuerDnAndSerial(String issuerDn, BigInteger serial) {
        return null;
    }

    public X509Certificate getCertificateBySKI(String base64Ski) {
        return null;
    }

    public X509Certificate getCertificateBySubjectDn(String subjectDn) {
        return null;
    }

    public RevocationCheckPolicy getDefaultPolicy() {
        return null;
    }

    public RevocationCheckPolicy getRevocationCheckPolicy( TrustedCert trustedCert) {
        return null;
    }

    public TrustedCert getTrustedCertByOid(long oid) {
        return null;
    }

    public TrustedCert getTrustedCertEntry(X509Certificate certificate) {
        return null;
    }
}
