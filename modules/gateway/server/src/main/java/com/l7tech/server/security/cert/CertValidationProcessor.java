package com.l7tech.server.security.cert;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Interface for Certificate Validation Processors.
 *
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
     * Check the validity of the given certificate.
     *
     * @param certificatePath The certificate to validate, possibly with path (must not be null) 
     * @param minimumValidationType The minimum permitted type of validation to
     *        be performed, or null for any
     * @param requestedValidationType the type of validation to be performed,
     *        or null to use the system-wide default for the provided facility
     * @param facility the type of system component asking for the validation
     *        to be performed.  Must be supplied if validationType is null.
     * @param auditor The auditor to use (must not be null)
     */
    CertificateValidationResult check(X509Certificate[] certificatePath,
                                      CertificateValidationType minimumValidationType,
                                      CertificateValidationType requestedValidationType,
                                      Facility facility,
                                      Audit auditor)
            throws CertificateException;

    /**
     * Get a known certificate by subject distinguished name.
     *
     * @param subjectDn The subject DN
     * @return The certificate or null if not known
     */
    X509Certificate getCertificateBySubjectDn(String subjectDn);

    /**
     * Get a known certificate by issuer distinguished name and serial.
     *
     * @param issuerDn The issuers DN
     * @param serial The certficates serial number
     * @return The certificate or null if not known
     */
    X509Certificate getCertificateByIssuerDnAndSerial(String issuerDn, BigInteger serial);

    /**
     * Get a known certificate by subject key identifier.
     *
     * @param base64Ski The BASE64 encoded SKI
     * @return The certificate or null if not known
     */
    X509Certificate getCertificateBySKI(String base64Ski);

    /**
     * Get a Trusted Certificate by OID.
     *
     * @param oid The key for the certificate
     * @return The trusted certificate or null
     */
    TrustedCert getTrustedCertByOid(Goid oid);

    /**
     * Get a Trusted Certificate by certificate.
     *
     * @param certificate The certificate of the trusted certificate entry
     * @return The trusted certificate or null
     */
    TrustedCert getTrustedCertEntry(X509Certificate certificate);

    /**
     * Get a Revocation Check Policy by trusted certificate.
     *
     * <p>This may fetch an explictly referenced policy, the default or a
     * policy that disables revocation checking for the trusted certificate
     * entry.</p>
     *
     * @param trustedCert The trusted certificate
     * @return The policy or null
     */
    RevocationCheckPolicy getRevocationCheckPolicy(TrustedCert trustedCert);

    /**
     * Get the default Revocation Check Policy.
     *
     * @return The policy or null
     */
    RevocationCheckPolicy getDefaultPolicy();
}
