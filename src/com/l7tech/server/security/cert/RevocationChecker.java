package com.l7tech.server.security.cert;

import java.security.cert.X509Certificate;

import com.l7tech.common.security.CertificateValidationResult;

/**
 * Interface for revocation checker implementations.
 *
 * <p>A checker will perform revocation checking for a particular issuer.</p>
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
     * @return the revocation status for the certificate
     */
    CertificateValidationResult getRevocationStatus(X509Certificate certificate);
}
