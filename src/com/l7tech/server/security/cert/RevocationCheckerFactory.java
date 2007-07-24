package com.l7tech.server.security.cert;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.l7tech.common.security.CertificateValidationResult;
import com.l7tech.common.security.RevocationCheckPolicy;
import com.l7tech.common.security.RevocationCheckPolicyItem;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.objectmodel.FindException;

/**
 * Factory for building RevocationCheckers.
 *
 * @author Steve Jones
 */
public class RevocationCheckerFactory {

    //- PUBLIC

    /**
     * Create a RevocationCheckerFactory that will obtain data from the given providers
     *
     * @param revocationCheckPolicyProvider Provider for {@link RevocationCheckPolicy RevocationCheckPolicies}
     * @param trustedCertProvider Provider for {@link TrustedCert TrustedCerts}
     */
    public RevocationCheckerFactory(final RevocationCheckPolicyProvider revocationCheckPolicyProvider,
                                    final TrustedCertProvider trustedCertProvider) {
        // managers
        this.revocationCheckPolicyProvider = revocationCheckPolicyProvider;
        this.trustedCertProvider = trustedCertProvider;

        // initialize maps and map lock
        this.mapLock = new ReentrantReadWriteLock();
        this.checkersByCert = new HashMap();
        this.keysByTrustedCertOid = new HashMap();
        this.keysByRevocationCheckPolicyOid = new HashMap();
    }

    /**
     * Get the revocation checker for the given certificate.
     *
     * @param certificate The certificate of the issuer (CA) this checker will check.
     */
    public RevocationChecker getRevocationChecker(final X509Certificate certificate) {
        CompositeRevocationChecker checker = null;

        try {
            CertKey key = new CertKey(certificate);

            mapLock.readLock().lock();
            try {
                checker = checkersByCert.get(key);
            } finally {
                mapLock.readLock().unlock();
            }

            if (checker == null) {
                TrustedCert trustedCert = getTrustedCert(certificate);
                RevocationCheckPolicy policy = getRevocationCheckPolicy(trustedCert);
                checker = buildChecker(key, policy);

                mapLock.writeLock().lock();
                try {
                    checkersByCert.put(key, checker);

                    if (trustedCert != null)
                        addChecker(keysByTrustedCertOid, trustedCert.getOid(), checker);

                    if (policy != null && policy.getOid()!=RevocationCheckPolicy.DEFAULT_OID)
                        addChecker(keysByRevocationCheckPolicyOid, policy.getOid(), checker);
                } finally {
                    mapLock.writeLock().unlock();                    
                }
            }
        } catch (CertificateException cee) {
            Object subject = certificate == null ? "none" : certificate.getSubjectDN();
            logger.log(Level.WARNING, "Unable to create revocation checker for certificate '"+subject+"'.", cee);
        } catch (FindException fe) {
            Object subject = certificate == null ? "none" : certificate.getSubjectDN();
            logger.log(Level.WARNING, "Unable to create revocation checker for certificate '"+subject+"'.", fe);
        }

        if (checker == null) {
            checker = new RevokedChecker();            
        }

        return checker;
    }

    /**
     * Notify the RevocationCheckerFactory of a RevocationCheckPolicy change.
     *
     * <p>This will cause cached data to be invalidated for the policy.</p>
     *
     * @param revocationCheckPolicyOid The key for the changed policy
     */
    public void invalidateRevocationCheckPolicy(long revocationCheckPolicyOid) {
        invalidateForProvider(
                revocationCheckPolicyOid,
                revocationCheckPolicyProvider,
                keysByRevocationCheckPolicyOid);
    }

    /**
     * Notify the RevocationCheckerFactory of a TrustedCert change.
     *
     * <p>This will cause cached data to be invalidated for the trusted cert.</p>
     *
     * <p>Note that this must be called when a trusted cert is added.</p>
     *
     * @param trustedCertOid The key for the changed trusted certificate
     */
    public void invalidateTrustedCert(long trustedCertOid) {
        invalidateForProvider(
                trustedCertOid,
                trustedCertProvider,
                keysByTrustedCertOid);
    }

    public interface Provider<T> {
        T findByPrimaryKey(long key) throws FindException, CertificateException;
    }

    public interface RevocationCheckPolicyProvider extends Provider<RevocationCheckPolicy> {
        RevocationCheckPolicy findByPrimaryKey(long key) throws FindException;
        RevocationCheckPolicy getDefaultPolicy() throws FindException;
    }

    public interface TrustedCertProvider extends Provider<TrustedCert> {
        TrustedCert getCachedCertBySubjectDn(String subjectDn) throws FindException, CertificateException;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(RevocationCheckerFactory.class.getName());

    private final RevocationCheckPolicyProvider revocationCheckPolicyProvider;
    private final TrustedCertProvider trustedCertProvider;

    private final ReadWriteLock mapLock;
    private Map<CertKey,CompositeRevocationChecker> checkersByCert;
    private Map<Long,Set<CertKey>> keysByTrustedCertOid;
    private Map<Long,Set<CertKey>> keysByRevocationCheckPolicyOid;

    /**
     * Add info to the map, caller is responsible for getting a write lock
     */
    private void addChecker(Map<Long,Set<CertKey>> keysByoid, long oid, CompositeRevocationChecker checker) {
        Long longOid = Long.valueOf(oid);
        Set<CertKey> keySet = keysByoid.get(longOid);

        if (keySet == null) {
            keySet = new HashSet();
            keysByoid.put(longOid, keySet);
        }

        keySet.add(checker.key);
    }

    /**
     * Invalidate any cached checker for the given oid / provider
     *
     * For trusted certs this will also invalidate by certificate key in case
     * we already have a cached revocation checker for the certificate .
     */
    private void invalidateForProvider(long oid, Provider<?> provider, Map<Long,Set<CertKey>> keysForProvider) {
        List<Long> providerMappingsToPurge = new ArrayList();
        List<CertKey> certKeysToPurge = new ArrayList();

        // find checker for trusted cert
        Set<CertKey> keys = null;
        mapLock.readLock().lock();
        try {
            keys = keysForProvider.get(Long.valueOf(oid));
        } finally {
            mapLock.readLock().unlock();
        }

        if (keys != null) { // invalidate for update / delete
            providerMappingsToPurge.add(Long.valueOf(oid));
            certKeysToPurge.addAll(keys);
        } else if ( provider instanceof TrustedCertProvider) { // invalidate for new or unused
            try {
                TrustedCertProvider tcProvider = (TrustedCertProvider) provider;
                TrustedCert trustedCert = tcProvider.findByPrimaryKey(oid);
                if (trustedCert != null) {
                    certKeysToPurge.add(new CertKey(trustedCert.getCertificate()));
                }
            } catch (FindException fe) {
                logger.log(Level.WARNING, "Error finding trusted certificate '"+oid+"' for invalidation.", fe);
            } catch (CertificateException ce) {
                logger.log(Level.WARNING, "Error getting trusted certificate '"+oid+"' for invalidation.", ce);
            }
        }

        // purge
        mapLock.writeLock().lock();
        try {
            keysForProvider.keySet().removeAll(providerMappingsToPurge);
            checkersByCert.keySet().removeAll(certKeysToPurge);
        } finally {
            mapLock.writeLock().unlock();
        }

    }

    /**
     * Get the TrustedCert entry for the given certificate.
     *
     * @param certificate
     * @return The TrustedCert entry or null if there is none
     * @throws FindException On DB error
     * @throws CertificateException On error with cert
     */
    private TrustedCert getTrustedCert(final X509Certificate certificate) throws FindException, CertificateException {
        return trustedCertProvider.getCachedCertBySubjectDn(certificate.getIssuerDN().toString());
    }

    /**
     * Get the RevocationCheckPolicy for the given trusted cert (or the default/none)
     *
     * @param trustedCert which may be null
     * @return the RevocationCheckPolicy to use (may be null)
     * @throws FindException On DB error
     */
    private RevocationCheckPolicy getRevocationCheckPolicy(final TrustedCert trustedCert) throws FindException {
        //TODO implement when the hibernate mapping is in place
        //revocationCheckPolicyProvider.findByPrimaryKey(trustedCert.get???);
        //revocationCheckPolicyProvider.getDefaultPolicy()

        // Policy to use when NONE
        //RevocationCheckPolicy noCheckingPolicy = new RevocationCheckPolicy();
        //noCheckingPolicy.setDefaultSuccess(true);

        throw new FindException("Not implemented");
    }

    /**
     * Load the certificates for the given list of trusted cert oids
     */
    private X509Certificate[] loadCerts(List<Long> tcOids) throws FindException, CertificateException {
        List<X509Certificate> certs = new ArrayList();

        for ( Long oid : tcOids ) {
            TrustedCert tc = trustedCertProvider.findByPrimaryKey(oid);
            if (tc != null) {
                certs.add(tc.getCertificate());
            }
        }

        return certs.toArray(new X509Certificate[certs.size()]);
    }

    /**
     * Build the revocation checkers for the given policy.
     */
    private CompositeRevocationChecker buildChecker(final CertKey key,
                                                    final RevocationCheckPolicy rcp) throws FindException, CertificateException {
        CertificateValidationResult resultForUnknown = CertificateValidationResult.REVOKED;
        List<RevocationChecker> revocationCheckers = new ArrayList();

        if (rcp != null) {
            if (rcp.isDefaultSuccess())
                resultForUnknown =  CertificateValidationResult.OK;

            for (RevocationCheckPolicyItem item : rcp.getRevocationCheckItems()) {
                String url = item.getUrl();
                boolean issuer = item.isAllowIssuerSignature();
                X509Certificate[] certs = loadCerts(item.getTrustedSigners());

                RevocationCheckPolicyItem.Type type = item.getType();
                if (type != null) {
                    try {
                        switch (type) {
                            case CRL_FROM_CERTIFICATE:
                                revocationCheckers.add(new CRLRevocationChecker(null, Pattern.compile(url), issuer, certs));
                                break;
                            case CRL_FROM_URL:
                                revocationCheckers.add(new CRLRevocationChecker(url, null, issuer, certs));
                                break;
                            case OCSP_FROM_CERTIFICATE:
                                revocationCheckers.add(new OCSPRevocationChecker(null, Pattern.compile(url), issuer, certs));
                                break;
                            case OCSP_FROM_URL:
                                revocationCheckers.add(new OCSPRevocationChecker(url, null, issuer, certs));
                                break;
                            default:
                                logger.log(Level.WARNING, "Ignoring unknown revocation checking type ''{0}''.", type.name());
                        }
                    } catch (PatternSyntaxException pse) {
                        logger.log(Level.WARNING, "Ignoring revocation checker with invalid URL pattern ''{0}''.", url);
                    }
                }
            }
        }

        return new CompositeRevocationChecker(key, resultForUnknown, revocationCheckers);
    }

    /**
     * Map key based on a certificate
     */
    private static final class CertKey {
        private final X509Certificate certificate;
        private final int hashCode;

        private CertKey(X509Certificate certificate) throws CertificateEncodingException {
            this.certificate = certificate;
            this.hashCode =  Arrays.hashCode(certificate.getEncoded());
        }

        public boolean equals(Object obj) {
            return CertUtils.certsAreEqual(certificate, ((CertKey)obj).certificate);
        }

        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Revocation checker that is composed of a list of checkers.
     *
     * This is the runtime for a RevocationCheckPolicy
     */
    private static class CompositeRevocationChecker implements RevocationChecker {
        private final CertKey key;
        private final CertificateValidationResult resultForUnknown;
        private final List<RevocationChecker> revocationCheckers;

        private CompositeRevocationChecker(CertKey key,
                                           CertificateValidationResult resultForUnknown,
                                           List<RevocationChecker> revocationCheckers) {
            this.key = key;
            this.resultForUnknown = resultForUnknown;
            this.revocationCheckers = Collections.unmodifiableList(revocationCheckers);
        }

        public CertificateValidationResult getRevocationStatus(final X509Certificate certificate) {
            CertificateValidationResult result = CertificateValidationResult.REVOKED;

            if ( certificate != null ) {
                CertificateValidationResult working = CertificateValidationResult.UNKNOWN;

                checking:
                for ( RevocationChecker checker : revocationCheckers ) {
                    working = checker.getRevocationStatus( certificate );

                    switch ( working ) {
                        case CANT_BUILD_PATH:
                            // Can't build path is not valid here, so change to revoked
                            working = CertificateValidationResult.REVOKED;
                        case REVOKED:
                        case OK:
                            break checking;
                    }
                }

                if ( CertificateValidationResult.UNKNOWN.equals(working) ) {
                    result = resultForUnknown;    
                }
            }

            return result;
        }
    }

    /**
     * Base class for Url/Url pattern based revocation checkers.
     */
    private static abstract class AbstractRevocationChecker implements RevocationChecker {
        private final String url;
        private final Pattern regex;
        private final boolean allowIssuerSignature;
        private final X509Certificate[] trustedIssuers;

        protected AbstractRevocationChecker(String url, Pattern regex, boolean allowIssuerSignature, X509Certificate[] trustedIssuers) {
            this.url = url;
            this.regex = regex;
            this.allowIssuerSignature = allowIssuerSignature;
            this.trustedIssuers = trustedIssuers;
        }

        protected String getUrl() {
            return url;
        }

        protected Pattern getUrlPattern() {
            return regex;
        }

        protected boolean getAllowIssuerSignature() {
            return allowIssuerSignature;
        }

        protected X509Certificate[] getTrustedIssuers() {
            return trustedIssuers;
        }
    }

    /**
     *
     */
    private static final class CRLRevocationChecker extends AbstractRevocationChecker {
        private CRLRevocationChecker(String url, Pattern regex, boolean allowIssuerSignature, X509Certificate[] trustedIssuers) {
            super(url, regex, allowIssuerSignature, trustedIssuers);
        }

        public CertificateValidationResult getRevocationStatus(X509Certificate certificate) {
            return CertificateValidationResult.REVOKED;
        }
    }

    /**
     *
     */
    private static final class OCSPRevocationChecker extends AbstractRevocationChecker {
        private OCSPRevocationChecker(String url, Pattern regex, boolean allowIssuerSignature, X509Certificate[] trustedIssuers) {
            super(url, regex, allowIssuerSignature, trustedIssuers);
        }

        public CertificateValidationResult getRevocationStatus(X509Certificate certificate) {
            return CertificateValidationResult.REVOKED;
        }
    }

    private static final class RevokedChecker extends CompositeRevocationChecker {
        private RevokedChecker() {
            super(null, null, null);
        }
        public CertificateValidationResult getRevocationStatus(X509Certificate certificate) {
            return CertificateValidationResult.REVOKED;
        }
    }
}
