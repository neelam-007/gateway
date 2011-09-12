package com.l7tech.server.security.cert;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.SignatureException;
import java.math.BigInteger;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.security.cert.TrustedCert;

/**
 * @author Steve Jones
 */
public class TestCertValidationProcessor implements CertValidationProcessor {

    @Override
    public CertificateValidationResult check(X509Certificate[] certificatePath, CertificateValidationType minimumValidationType, CertificateValidationType requestedValidationType, CertValidationProcessor.Facility facility, Audit auditor) throws CertificateException, SignatureException {
        return CertificateValidationResult.OK;
    }

    @Override
    public X509Certificate getCertificateByIssuerDnAndSerial(String issuerDn, BigInteger serial) {
        return null;
    }

    @Override
    public X509Certificate getCertificateBySKI(String base64Ski) {
        return null;
    }

    @Override
    public X509Certificate getCertificateBySubjectDn(String subjectDn) {
        return null;
    }

    @Override
    public RevocationCheckPolicy getDefaultPolicy() {
        return null;
    }

    @Override
    public RevocationCheckPolicy getRevocationCheckPolicy( TrustedCert trustedCert) {
        return null;
    }

    @Override
    public TrustedCert getTrustedCertByOid(long oid) {
        return null;
    }

    @Override
    public TrustedCert getTrustedCertEntry(X509Certificate certificate) {
        return null;
    }
}
