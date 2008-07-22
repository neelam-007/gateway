package com.l7tech.server.security.cert;

import com.l7tech.server.audit.Auditor;
import com.l7tech.security.types.CertificateValidationResult;

import java.security.cert.X509Certificate;

/**
 * Interface for revocation checker implementations.
 *
 * <p>A checker will perform revocation checking for one or more issuers.</p>
 *
 * @author Steve Jones
 */
public interface RevocationChecker {

    /**
     * Check if the given certificate has been revoked.
     *
     * <p>Revocation checkers are not expected to return
     * {@link CertificateValidationResult#CANT_BUILD_PATH CANT_BUILD_PATH},
     * since they don't perform any path building.</p>
     *
     * @param certificate The certificate to check.
     * @param issuer The certificate for the issuer of the certificate to check.
     * @param auditor The auditor to use for any audit messages
     * @return the revocation status for the certificate
     */
    CertificateValidationResult getRevocationStatus(X509Certificate certificate, X509Certificate issuer, Auditor auditor, CertificateValidationResult resultOnNetworkFailure);
}
