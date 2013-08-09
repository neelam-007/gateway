package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

/**
 * Provides higher-level services related to TrustedCert instances than simple cached CRUD.
 * This includes services such as checking certificate trust, and filtered cache lookups.
 */
public interface TrustedCertServices {
    /**
     * Checks whether the certificate at the top of the specified chain is trusted for outbound SSL connections.
     * <p>
     * This will be true if either the specific certificate has the {@link com.l7tech.security.cert.TrustedCert#isTrustedForSsl()}
     * option set, or the signing cert that comes next in the chain has the {@link com.l7tech.security.cert.TrustedCert#isTrustedForSigningServerCerts()}
     * option set.
     * <p>
     * @param serverCertChain the certificate chain
     * @param trustedCertOids a list of OIDs of trusted certificates to trust, or null to use the list from the database.
     * @throws java.security.cert.CertificateException if there is a problem decoding a certificate
     */
    void checkSslTrust(X509Certificate[] serverCertChain, Set<Goid> trustedCertOids) throws CertificateException;

    /**
     * Perform a cached lookup of trusted certs by subject DN, filtered to include only TrustedCert instances
     * matching the specified criteria.
     *
     * @param subjectDn  the subject DN to look up.  Required.
     * @param omitExpired  if true, certificates that are before their NotBefore or after their NotAfter will be omitted from the returned list.
     * @param requiredTrustFlags  a Set of TrustedFor flags to filter by flag, or null to perform no filtering by flag.
     *                            If a Set is specified, only TrustedCert instances that have all the specified flags enabled
     *                            will be returned.  If an empty Set is specified, no TrustedCert instances will be returned.
     * @param requiredOids a Set of TrustedCert OIDs that are acceptable, or null to perform no filtering by OID.
     *                     If a Set is specified, only TrustedCert instances with an OID in the set will be returned.
     *                     If an empty set is specified, no TrustedCert instances will be returned.
     * @return a Collection of read-only TrustedCert instances that match the specified criteria.  May be empty, but never null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem reading TrustedCert instances from the database.
     */
    Collection<TrustedCert> getCertsBySubjectDnFiltered(String subjectDn, boolean omitExpired, Set<TrustedCert.TrustedFor> requiredTrustFlags, Set<Goid> requiredOids) throws FindException;

    /**
     * Perform a cached lookup of all trusted certs that are trusted for the specified activities.
     *
     * @param requiredTrustFlags  a Set of TrustedFor flags to filter by flag.  Required.
     *                            If a Set is specified, only TrustedCert instances that have all the specified flags enabled
     *                            will be returned.  If an empty Set is specified, no TrustedCert instances will be returned.
     * @return a Collection of read-only TrustedCert instances that match the specified criteria.  May be empty, but never null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem reading TrustedCert instances from the database.
     */
    Collection<TrustedCert> getAllCertsByTrustFlags(Set<TrustedCert.TrustedFor> requiredTrustFlags) throws FindException;
}
