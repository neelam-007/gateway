package com.l7tech.server.security.cert;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.common.security.CertificateValidationResult;
import com.l7tech.common.security.RevocationCheckPolicy;
import com.l7tech.common.security.RevocationCheckPolicyItem;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.util.ServerCertUtils;

import java.io.IOException;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Factory for building RevocationCheckers.
 *
 * @author Steve Jones
 */
public class RevocationCheckerFactory {
    //- PUBLIC

    /**
     * Create a RevocationCheckerFactory that will obtain data from the given providers
     */
    public RevocationCheckerFactory(CertValidationProcessor processor, CrlCache crlCache) {
        // managers
        this.certValidationProcessor = processor;
        this.crlCache = crlCache;

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
                TrustedCert trustedCert = certValidationProcessor.getTrustedCertEntry(certificate);
                RevocationCheckPolicy policy = trustedCert == null ?
                        certValidationProcessor.getDefaultPolicy() :
                        certValidationProcessor.getRevocationCheckPolicy(trustedCert);
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
                null,
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
                new Functions.Unary<X509Certificate, Long>() {
                    public X509Certificate call(Long oid) {
                        final TrustedCert cert = certValidationProcessor.getTrustedCertByOid(oid);
                        if (cert == null) return null;
                        try {
                            return cert.getCertificate();
                        } catch (CertificateException e) {
                            throw new RuntimeException(e); // Can't happen
                        }
                    }
                },
                keysByTrustedCertOid);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(RevocationCheckerFactory.class.getName());

    private final CertValidationProcessor certValidationProcessor;
    private final CrlCache crlCache;

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
    private void invalidateForProvider(long oid, Functions.Unary<X509Certificate, Long> certGetter, Map<Long,Set<CertKey>> keysForProvider) {
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
        } else if (certGetter != null) { // invalidate for new or unused
            try {
                X509Certificate cert = certGetter.call(oid);
                if (cert != null) {
                    certKeysToPurge.add(new CertKey(cert));
                }
            } catch (Exception fe) {
                logger.log(Level.WARNING, "Error finding trusted certificate '"+oid+"' for invalidation.", fe);
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
     * Load the certificates for the given list of trusted cert oids
     */
    private X509Certificate[] loadCerts(List<Long> tcOids) throws FindException, CertificateException {
        List<X509Certificate> certs = new ArrayList();

        for (Long oid : tcOids) {
            TrustedCert tc = certValidationProcessor.getTrustedCertByOid(oid);
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
                                revocationCheckers.add(new CRLRevocationChecker(null, Pattern.compile(url), issuer, certs, crlCache, certValidationProcessor));
                                break;
                            case CRL_FROM_URL:
                                revocationCheckers.add(new CRLRevocationChecker(url, null, issuer, certs, crlCache, certValidationProcessor));
                                break;
                            case OCSP_FROM_CERTIFICATE:
                                revocationCheckers.add(new OCSPRevocationChecker(null, Pattern.compile(url), issuer, certs, certValidationProcessor));
                                break;
                            case OCSP_FROM_URL:
                                revocationCheckers.add(new OCSPRevocationChecker(url, null, issuer, certs, certValidationProcessor));
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

        public CertificateValidationResult getRevocationStatus(final X509Certificate certificate, Auditor auditor) {
            CertificateValidationResult result = CertificateValidationResult.REVOKED;

            if ( certificate != null ) {
                CertificateValidationResult working = CertificateValidationResult.UNKNOWN;

                checking:
                for ( RevocationChecker checker : revocationCheckers ) {
                    working = checker.getRevocationStatus( certificate, auditor);

                    switch ( working ) {
                        case CANT_BUILD_PATH:
                            // Can't build path is not valid here, so change to revoked
                            working = CertificateValidationResult.REVOKED;
                        case REVOKED:
                            // FALLTHROUGH
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

        protected abstract String[] getUrlsFromCert(X509Certificate certificate, Auditor auditor) throws IOException;

        protected abstract String what();

        protected String getValidUrl(X509Certificate certificate, Auditor auditor) {
            String staticUrl = getUrl();
            if (staticUrl != null) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_USING_STATIC_URL, what(), staticUrl);
                return staticUrl;
            } else {
                Pattern pat = getUrlPattern();
                String[] urlsFromCert;
                final String subjectDn = certificate.getSubjectDN().getName();
                try {
                    urlsFromCert = getUrlsFromCert(certificate, auditor);
                } catch (IOException e) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_CANT_GET_URLS_FROM_CERT, what(), subjectDn);
                    return null;
                }

                if (urlsFromCert == null || urlsFromCert.length == 0) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_NO_URL, what(), subjectDn);
                    return null;
                }

                for (String url : urlsFromCert) {
                    if (pat.matcher(url).matches()) {
                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_URL_MATCH, what(), urlsFromCert[0], subjectDn);
                        return url;
                    }
                }

                auditor.logAndAudit(SystemMessages.CERTVAL_REV_URL_MISMATCH, what(), subjectDn);
                return null;
            }
        }
    }

    /**
     *
     */
    private static final class CRLRevocationChecker extends AbstractRevocationChecker {
        private final CrlCache crlCache;
        private final CertValidationProcessor certRevocationProcessor;

        private CRLRevocationChecker(String url, Pattern regex, boolean allowIssuerSignature, X509Certificate[] trustedIssuers, CrlCache crlCache, CertValidationProcessor processor) {
            super(url, regex, allowIssuerSignature, trustedIssuers);
            this.crlCache = crlCache;
            this.certRevocationProcessor = processor;
        }

        public CertificateValidationResult getRevocationStatus(X509Certificate certificate, Auditor auditor) {
            final String crlUrl = getValidUrl(certificate, auditor);
            if (crlUrl == null) {
                // {@link getCrlUrl()} already audited the reason
                return CertificateValidationResult.UNKNOWN;
            }
            try {
                X509CRL crl = crlCache.getCrl(crlUrl, auditor);
                final String crlIssuerDn = crl.getIssuerDN().getName();
                TrustedCert tc = certRevocationProcessor.getTrustedCertBySubjectDn(crlIssuerDn);
                if (tc == null) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_ISSUER_NOT_FOUND, crlIssuerDn);
                    return CertificateValidationResult.UNKNOWN;
                }
                // TODO compare AKI extension from CRL if present
                final RevocationCheckPolicy rcp = certRevocationProcessor.getRevocationCheckPolicy(tc);
                // TODO lookup the CRL signing cert somehow?
                // TODO verify whether this CRL is authoritative with respect to this cert?
                if (crl.isRevoked(certificate))
                    return CertificateValidationResult.REVOKED;
                else
                    return CertificateValidationResult.OK;
            } catch (CRLException e) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_CRL_INVALID, crlUrl, ExceptionUtils.getMessage(e));
                return CertificateValidationResult.UNKNOWN;
            } catch (IOException e) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_RETRIEVAL_FAILED, crlUrl, ExceptionUtils.getMessage(e));
                return CertificateValidationResult.UNKNOWN;
            }
        }

        protected String what() {
            return "CRL";
        }

        protected String[] getUrlsFromCert(X509Certificate certificate, Auditor auditor) throws IOException {
            return crlCache.getCrlUrlsFromCertificate(certificate, auditor);
        }
    }

    /**
     *
     */
    private static final class OCSPRevocationChecker extends AbstractRevocationChecker {
        private static final String OID_AIA_OCSP = "1.3.6.1.5.5.7.48.1";
        private final CertValidationProcessor certValidationProcessor;

        private OCSPRevocationChecker(final String url,
                                      final Pattern regex,
                                      final boolean allowIssuerSignature,
                                      final X509Certificate[] trustedIssuers,
                                      final CertValidationProcessor certValidationProcessor) {
            super(url, regex, allowIssuerSignature, trustedIssuers);
            this.certValidationProcessor = certValidationProcessor;
        }

        protected String[] getUrlsFromCert(X509Certificate certificate, Auditor auditor) throws IOException {
            try {
                return ServerCertUtils.getAuthorityInformationAccessUris(certificate, OID_AIA_OCSP);
            } catch (CertificateException ce) {
                throw new IOException("Error extracting OCSP urls from certificate.", ce);
            }
        }

        protected String what() {
            return "OCSP";
        }

        public CertificateValidationResult getRevocationStatus(X509Certificate certificate, Auditor auditor) {
            CertificateValidationResult result = CertificateValidationResult.REVOKED;
            String url = getValidUrl(certificate, auditor);
            X509Certificate issuerCertificate = null;

            try {
                String issuerDn = certificate.getIssuerDN().getName();
                TrustedCert tc = certValidationProcessor.getTrustedCertBySubjectDn(issuerDn);
                if (tc == null) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_ISSUER_NOT_FOUND, issuerDn);
                    return CertificateValidationResult.UNKNOWN;
                }
                issuerCertificate = tc.getCertificate();
            } catch (CertificateException ce) {
                auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_ERROR, new String[]{url, ExceptionUtils.getMessage(ce)}, ce);
            }

            if ( issuerCertificate != null ) {
                try {
                    //TODO caching of OCSP responses, reuse of HTTP client, HTTPS.
                    String issuerDn = certificate.getIssuerDN().getName();
                    TrustedCert tc = certValidationProcessor.getTrustedCertBySubjectDn(issuerDn);
                    if (tc == null) {
                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_ISSUER_NOT_FOUND, issuerDn);
                        return CertificateValidationResult.UNKNOWN;
                    }
                    OCSPClient ocsp = new OCSPClient(new CommonsHttpClient(), url, issuerCertificate, getAllowIssuerSignature(), getTrustedIssuers());
                    OCSPClient.OCSPStatus status = ocsp.getRevocationStatus(certificate, true, true);
                    result = status.getResult();
                } catch (OCSPClient.OCSPClientException oce) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_ERROR, new String[]{url, ExceptionUtils.getMessage(oce)}, oce);
                }
            }

            return result;
        }
    }

    private static final class RevokedChecker extends CompositeRevocationChecker {
        private RevokedChecker() {
            super(null, null, null);
        }
        public CertificateValidationResult getRevocationStatus(X509Certificate certificate, Auditor auditor) {
            return CertificateValidationResult.REVOKED;
        }
    }
}
