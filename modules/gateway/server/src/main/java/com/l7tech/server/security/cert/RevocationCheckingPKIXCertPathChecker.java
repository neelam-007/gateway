package com.l7tech.server.security.cert;

import com.l7tech.server.audit.Auditor;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.common.io.CertUtils;

import java.security.cert.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PKIXCertPathChecker that performs revocation checking.
 *
 * <p>A RevocationCheckerFactory is used to obtain a revocation check that is
 * applicable for the certificate being validated.</p>
 *
 * <p>When creating a revocation path checker the caller must provide the
 * {@link TrustAnchor TrustAnchors} for use when determining if the first
 * certificate in the chain is trusted.</p>
 *
 * @see PKIXParameters#getTrustAnchors()
 * @author Steve Jones
 */
public class RevocationCheckingPKIXCertPathChecker extends PKIXCertPathChecker {
    
    //- PUBLIC

    /**
     * Create a path checker with the given anchor.
     *
     * @param factory The factory for RevocationCheckers
     * @param params The parameters provide access to {@link java.security.cert.TrustAnchor TrustAnchors}
     * @param auditor
     */
    public RevocationCheckingPKIXCertPathChecker(final RevocationCheckerFactory factory, final PKIXParameters params, Auditor auditor) {
        this.revocationCheckerFactory = factory;
        this.trustAnchors = params.getTrustAnchors();
        this.auditor = auditor;
        this.lastFailedX509Certificate = new X509Certificate[1];
    }

    /**
     * Check a certificate for revocation.
     *
     * @param certificate The certificate to check
     * @param unresolvedCritExts ignored
     * @throws CertPathValidatorException if the certificate is revoked (or status cannot be determined)
     */
    @Override
    public void check(final Certificate certificate, final Collection<String> unresolvedCritExts) throws CertPathValidatorException {
        if ( !(certificate instanceof X509Certificate))
            throw new CertPathValidatorException("Only X.509 certificates are supported.");

        final X509Certificate x509Certificate = (X509Certificate) certificate;

        /**
         * Cached failure for 2nd pass of Suns path builder
         */
        if (lastFailedX509Certificate[0] != null &&
                CertUtils.certsAreEqual(x509Certificate, lastFailedX509Certificate[0])) {
            throw new CertPathValidatorException("Revocation check failed for certificate '"+x509Certificate.getSubjectDN()+"'.");                
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Performing revocation check for certificate ''{0}''.", x509Certificate.getSubjectDN());

        // Get the issuer certificate
        X509Certificate issuerCertificate = prevX509Certificate;
        if (issuerCertificate == null) {
            for (TrustAnchor anchor : trustAnchors) {
                X509Certificate trustAnchorCertificate = anchor.getTrustedCert();
                if (CertUtils.isEqualDN(CertUtils.getSubjectDN(trustAnchorCertificate),
                                        CertUtils.getIssuerDN(x509Certificate)) ) {
                    if (CertVerifier.isVerified(x509Certificate, trustAnchorCertificate)) {
                        issuerCertificate = trustAnchorCertificate;
                        break;
                    }
                }
            }

            if (issuerCertificate == null)
                throw new CertPathValidatorException("Certificate not trusted '"+x509Certificate.getSubjectDN()+"' (no anchor)");
        }

        // Get the revocation checker for the issuer
        RevocationChecker revocationChecker = revocationCheckerFactory.getRevocationChecker(issuerCertificate);
        CertificateValidationResult status = revocationChecker.getRevocationStatus(x509Certificate, issuerCertificate, auditor, CertificateValidationResult.UNKNOWN);
        if (!status.equals(CertificateValidationResult.OK)) {
            lastFailedX509Certificate[0] = x509Certificate;
            throw new CertPathValidatorException("Revocation check failed for certificate '"+x509Certificate.getSubjectDN()+"'.");    
        }

        // keep this for later
        prevX509Certificate = x509Certificate;
    }

    /**
     * Get the supported extensions for this path checker.
     *
     * @return An empty set.
     */
    @Override
    public Set<String> getSupportedExtensions() {
        return Collections.emptySet();
    }

    /**
     * Initialize this path checker.
     *
     * @param forward Must be false (forward checking not supported)
     * @throws CertPathValidatorException If you try to use forward checking
     */
    @Override
    public void init(final boolean forward) throws CertPathValidatorException {
        if ( forward )
            throw new CertPathValidatorException("Forward checking not supported");

        prevX509Certificate = null;
        //lastFailedX509Certificate = null;
    }

    /**
     * Does this path checker support forward checking.
     *
     * @return false
     */
    @Override
    public boolean isForwardCheckingSupported() {
        return false;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(RevocationCheckingPKIXCertPathChecker.class.getName());

    private final RevocationCheckerFactory revocationCheckerFactory;
    private final Set<TrustAnchor> trustAnchors;
    private final Auditor auditor;

    private X509Certificate prevX509Certificate;

    // this is to allow cloned checkers to share the last failure to prevent
    // superfluous revocation checks due to the path builder backtracking
    // see sun.security.provider.certpath.SunCertPathBuilder:163
    // this is undesireable due to performance impact and duplication of
    // audit details
    private final X509Certificate[] lastFailedX509Certificate;
}
