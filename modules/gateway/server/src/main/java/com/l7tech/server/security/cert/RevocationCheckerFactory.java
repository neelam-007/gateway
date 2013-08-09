package com.l7tech.server.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.whirlycott.cache.Cache;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
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
    public RevocationCheckerFactory(CertValidationProcessor processor, CrlCache crlCache, OCSPCache ocspCache) {
        // managers
        this.certValidationProcessor = processor;
        this.crlCache = crlCache;
        this.ocspCache = ocspCache;

        // initialize maps and map lock
        // use Whirlycache for checkers by cert as this could be large
        this.mapLock = new ReentrantReadWriteLock();
        this.keysByTrustedCertOid = new HashMap<Goid,Set<CertKey>>();
        this.keysByRevocationCheckPolicyOid = new HashMap<Goid,Set<CertKey>>();
        this.checkersByCert =
                WhirlycacheFactory.createCache("RevocationPolicyCache", 1000, 66, WhirlycacheFactory.POLICY_LRU);
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
                checker = (CompositeRevocationChecker) checkersByCert.retrieve(key);
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
                    checkersByCert.store(key, checker);

                    if (trustedCert != null)
                        addChecker(keysByTrustedCertOid, trustedCert.getGoid(), checker);

                    if (policy != null && !RevocationCheckPolicy.DEFAULT_GOID.equals(policy.getGoid()))
                        addChecker(keysByRevocationCheckPolicyOid, policy.getGoid(), checker);
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
    public void invalidateRevocationCheckPolicy(Goid revocationCheckPolicyOid) {
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
    public void invalidateTrustedCert(Goid trustedCertOid) {
        invalidateForProvider(
                trustedCertOid,
                new Functions.Unary<X509Certificate, Goid>() {
                    @Override
                    public X509Certificate call(Goid oid) {
                        final TrustedCert cert = certValidationProcessor.getTrustedCertByOid(oid);
                        return cert == null ? null : cert.getCertificate();
                    }
                },
                keysByTrustedCertOid);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(RevocationCheckerFactory.class.getName());

    private final CertValidationProcessor certValidationProcessor;
    private final CrlCache crlCache;
    private final OCSPCache ocspCache;

    private final ReadWriteLock mapLock;
    private final Cache checkersByCert;
    private final Map<Goid,Set<CertKey>> keysByTrustedCertOid;
    private final Map<Goid,Set<CertKey>> keysByRevocationCheckPolicyOid;

    /**
     * Add info to the map, caller is responsible for getting a write lock
     */
    private void addChecker(Map<Goid,Set<CertKey>> keysByoid, Goid goid, CompositeRevocationChecker checker) {
        Set<CertKey> keySet = keysByoid.get(goid);

        if (keySet == null) {
            keySet = new HashSet<CertKey>();
            keysByoid.put(goid, keySet);
        }

        keySet.add(checker.key);
    }

    /**
     * Invalidate any cached checker for the given oid / provider
     *
     * For trusted certs this will also invalidate by certificate key in case
     * we already have a cached revocation checker for the certificate .
     */
    private void invalidateForProvider(Goid oid, Functions.Unary<X509Certificate, Goid> certGetter, Map<Goid,Set<CertKey>> keysForProvider) {
        Set<Goid> providerMappingsToPurge = new HashSet<Goid>();
        Set<CertKey> certKeysToPurge = new HashSet<CertKey>();

        // find checker for trusted cert
        Set<CertKey> keys = null;
        mapLock.readLock().lock();
        try {
            keys = keysForProvider.get(oid);
        } finally {
            mapLock.readLock().unlock();
        }

        if (keys != null) { // invalidate for update / delete
            providerMappingsToPurge.add(oid);
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

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Clearing cached revocation checkers for certificate keys {0}.", certKeysToPurge);
        }

        // purge
        mapLock.writeLock().lock();
        try {
            keysForProvider.keySet().removeAll(providerMappingsToPurge);
            for (CertKey key : certKeysToPurge) {
                checkersByCert.remove(key);
            }
        } finally {
            mapLock.writeLock().unlock();
        }

    }

    /**
     * Load the certificates for the given list of trusted cert oids
     */
    private X509Certificate[] loadCerts(List<Goid> tcOids) throws FindException, CertificateException {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();

        for (Goid oid : tcOids) {
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
        CertificateValidationResult resultForNetworkFailure = CertificateValidationResult.REVOKED;
        List<RevocationChecker> revocationCheckers = new ArrayList<RevocationChecker>();

        if (rcp != null) {
            if (rcp.isDefaultSuccess())
                resultForUnknown =  CertificateValidationResult.OK;

            if ( rcp.isContinueOnServerUnavailable() ){
                resultForNetworkFailure = CertificateValidationResult.UNKNOWN;
            }

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
                                revocationCheckers.add(new OCSPRevocationChecker(null, Pattern.compile(url), issuer, certs, ocspCache, certValidationProcessor));
                                break;
                            case OCSP_FROM_URL:
                                revocationCheckers.add(new OCSPRevocationChecker(url, null, issuer, certs, ocspCache, certValidationProcessor));
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

        return new CompositeRevocationChecker(key, resultForUnknown, resultForNetworkFailure, revocationCheckers);
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

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CertKey && CertUtils.certsAreEqual( certificate, ( (CertKey) obj ).certificate );
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "CertKey[dn='"+certificate.getSubjectDN().toString()+"']";
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
        private final CertificateValidationResult resultForNetworkFailure;
        private final List<RevocationChecker> revocationCheckers;

        private CompositeRevocationChecker(CertKey key,
                                           CertificateValidationResult resultForUnknown,
                                           CertificateValidationResult resultForNetworkFailure,
                                           List<RevocationChecker> revocationCheckers) {
            this.key = key;
            this.resultForUnknown = resultForUnknown;
            this.resultForNetworkFailure = resultForNetworkFailure;
            this.revocationCheckers = Collections.unmodifiableList(revocationCheckers);
        }

        @Override
        public CertificateValidationResult getRevocationStatus(final X509Certificate certificate, X509Certificate issuer, Audit auditor, CertificateValidationResult onNetworkFailure) {
            CertificateValidationResult result = CertificateValidationResult.REVOKED;

            if ( certificate != null ) {
                CertificateValidationResult working = CertificateValidationResult.UNKNOWN;

                checking:
                for ( RevocationChecker checker : revocationCheckers ) {
                    working = checker.getRevocationStatus(certificate, issuer, auditor, resultForNetworkFailure);

                    switch ( working ) {
                        case CANT_BUILD_PATH:
                            // Can't build path is not valid here, so change to revoked
                            working = CertificateValidationResult.REVOKED;
                            // FALLTHROUGH
                        case REVOKED:
                            // FALLTHROUGH
                        case OK:
                            break checking;
                    }
                }

                if ( CertificateValidationResult.UNKNOWN.equals(working) ) {
                    result = resultForUnknown;    
                } else {
                    result = working;
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
        private final CertValidationProcessor certValidationProcessor;

        protected AbstractRevocationChecker(final String url,
                                            final Pattern regex,
                                            final boolean allowIssuerSignature,
                                            final X509Certificate[] trustedIssuers,
                                            final CertValidationProcessor certValidationProcessor) {
            this.url = url;
            this.regex = regex;
            this.allowIssuerSignature = allowIssuerSignature;
            this.trustedIssuers = trustedIssuers;
            this.certValidationProcessor = certValidationProcessor;
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

        public CertValidationProcessor getCertValidationProcessor() {
            return certValidationProcessor;
        }

        /**
         * Extract the (CRL or OCSP) URL from the given certificate.
         *
         * @param certificate The certificate (not null)
         * @param auditor The auditor to use (not null)
         * @return The URLs from the certificate.
         * @throws IOException if an error occurs
         */
        protected abstract String[] getUrlsFromCert(X509Certificate certificate, Audit auditor) throws IOException;

        /**
         * Get a descriptive name for this type of revocation checker (e.g. "CRL" or "OCSP") 
         */
        protected abstract String what();

        /**
         * Attempt to locate a authorized signing certificate.
         *
         * @param issuer The issuer certificate
         * @param signerCerts A list of candidate certificates (may be empty)
         * @param auditor The auditor to use
         * @param checker A checker for additional "issuer" permitted certificates
         * @return The authorized certificate, or null if not authorized
         */
        protected X509Certificate getAuthorizedSigner(final X509Certificate issuer,
                                                      final X509Certificate[] signerCerts,
                                                      final Audit auditor,
                                                      final Functions.Unary<Boolean,X509Certificate> checker) {
            X509Certificate signer = null;

            if ( signerCerts.length == 0 ) {
                if ( allowIssuerSignature ) {
                    signer = issuer;
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_SIGNER_IS_ISSUER, what(), signer.getSubjectDN().toString());
                }
            } else {
                for ( X509Certificate signerCert : signerCerts ) {
                    // check ok for signing
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Checking signer certificate ''{0}''.",
                                signerCert.getSubjectDN());
                    }

                    // is this the issuer?
                    if ( issuer.getSerialNumber().equals(signerCert.getSerialNumber()) &&
                         CertUtils.getIssuerDN(issuer).equals(CertUtils.getIssuerDN(signerCert))) {
                        if ( allowIssuerSignature ) {
                            signer = issuer;
                            auditor.logAndAudit(SystemMessages.CERTVAL_REV_SIGNER_IS_ISSUER, what(), signer.getSubjectDN().toString());
                            break;
                        }
                    }

                    // is this an allowed cert?
                   for ( X509Certificate trustedSigner : trustedIssuers ) {
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Checking against trusted signer certificate ''{0}''.",
                                    trustedSigner.getSubjectDN());
                        }

                        if ( trustedSigner.getSerialNumber().equals(signerCert.getSerialNumber()) &&
                             CertUtils.getIssuerDN(trustedSigner).equals(CertUtils.getIssuerDN(signerCert))) {
                            signer = trustedSigner;
                            auditor.logAndAudit(SystemMessages.CERTVAL_REV_SIGNER_IS_TRUSTED, what(), signer.getSubjectDN().toString());                            
                            break;
                        }
                    }

                    if (allowIssuerSignature && checker != null && checker.call(signerCert)) {
                        signer = signerCert;
                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_SIGNER_IS_ISSUER_DELE, what(), signer.getSubjectDN().toString());
                        break;
                    }
                }
            }

            return signer;
        }

        /**
         * Get the URL to use for a revocation check (perhaps from the given certificate).
         *
         * @param certificate The certificate whose revocation status is of interest
         * @param auditor The auditor to use
         * @return The url or null on error (in which case a message is audited)
         */
        protected String getValidUrl(X509Certificate certificate, Audit auditor) {
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
                        try {
                            URI uri = new URI(url);
                            if (!uri.isAbsolute()) {
                                throw new URISyntaxException(url, "Relative URI");
                            }
                        } catch (URISyntaxException use) {
                            auditor.logAndAudit(SystemMessages.CERTVAL_REV_URL_INVALID, what(), url);
                            continue; // could be other valid URLs
                        }

                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_URL_MATCH, what(), url, subjectDn);
                        return url;
                    }
                }

                auditor.logAndAudit(SystemMessages.CERTVAL_REV_URL_MISMATCH, what(), subjectDn);
                return null;
            }
        }
    }

    /**
     * RevocationChecker that uses CRLs.
     *
     * This revocation checker will return UNKNOWN for the status if the
     * Certificate being checked is not covered by the CRL.
     *
     * This revocation checker will return UNKNOWN for the status if the CRL
     * URL is from the Certificate being checked and there is no URL that
     * matches the defined URL pattern.
     *
     * If a CRL cannot be retrieved or is invalid then REVOKED is returned
     * (fail closed)
     */
    private static final class CRLRevocationChecker extends AbstractRevocationChecker {
        private final CrlCache crlCache;

        private CRLRevocationChecker(String url, Pattern regex, boolean allowIssuerSignature, X509Certificate[] trustedIssuers, CrlCache crlCache, CertValidationProcessor processor) {
            super(url, regex, allowIssuerSignature, trustedIssuers, processor);
            this.crlCache = crlCache;
        }

        @Override
        public CertificateValidationResult getRevocationStatus(X509Certificate certificate, X509Certificate issuerCertificate, Audit auditor, CertificateValidationResult onNetworkFailure) {
            final String crlUrl = getValidUrl(certificate, auditor);
            if (crlUrl == null) {
                // {@link getCrlUrl()} already audited the reason
                return CertificateValidationResult.UNKNOWN;
            }
            try {
                X509CRL crl;
                try {
                    //attempt to grab the CRL
                    crl = crlCache.getCrl(crlUrl, auditor);
                }
                catch (IOException ioe) {
                    //we have experienced a network failure when trying to retrieve CRL from a server, we need to determine
                    //if should revoke at this point, or let it continue.  This decision is decided by "continue processing"
                    //checkbox.                      
                    return onNetworkFailure;
                }

                final String crlIssuerDn = CertUtils.getDN( crl.getIssuerX500Principal() );

                // Check certificate scope
                // Extend this check if we want to add support for the Issuing Distribution Point extension.
                if ( !crl.getIssuerX500Principal().equals(certificate.getIssuerX500Principal()) ) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_CRL_SCOPE, certificate.getSubjectDN().toString(), crlUrl);
                    return CertificateValidationResult.UNKNOWN;
                }

                CertValidationProcessor certValidationProcessor =  getCertValidationProcessor();
                X509Certificate crlCertificate;
                // Check CRL signature using Authority Key Identifier extension to id signer cert (if present)
                AuthorityKeyIdentifierStructure akis = CertUtils.getAKIStructure(crl);
                if (akis != null) {
                    String keyIdentifier = CertUtils.getAKIKeyIdentifier(akis);
                    if ( keyIdentifier != null) {
                        crlCertificate = certValidationProcessor.getCertificateBySKI(keyIdentifier);
                        if (crlCertificate == null) {
                            auditor.logAndAudit(SystemMessages.CERTVAL_REV_ISSUER_NOT_FOUND, what(), "SKI:"+keyIdentifier);
                            return CertificateValidationResult.REVOKED;
                        }
                    } else {
                        String certIssuerDn = CertUtils.getAKIAuthorityCertIssuer(akis);
                        BigInteger certSerial = CertUtils.getAKIAuthorityCertSerialNumber(akis);

                        if (certIssuerDn == null || certSerial==null) {
                            throw new CRLException("CRL Authority Key Identifier must have both a serial number and issuer name.");
                        }

                        crlCertificate = certValidationProcessor.getCertificateByIssuerDnAndSerial(certIssuerDn, certSerial);
                        if (crlCertificate == null) {
                            auditor.logAndAudit(SystemMessages.CERTVAL_REV_ISSUER_NOT_FOUND, what(),
                                    "IssuerDN:"+certIssuerDn+";Serial:"+certSerial);
                            return CertificateValidationResult.REVOKED;
                        }
                    }
                } else {
                    crlCertificate = certValidationProcessor.getCertificateBySubjectDn(crlIssuerDn);
                    if (crlCertificate == null) {
                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_ISSUER_NOT_FOUND, what(), crlIssuerDn);
                        return CertificateValidationResult.REVOKED;
                    }
                }

                // verify the signature
                X509Certificate signer = getAuthorizedSigner(issuerCertificate, new X509Certificate[]{crlCertificate}, auditor, null);
                if ( signer != null ) {
                    try {
                        //Note that this is cached in the CRL object
                        KeyUsageChecker.requireActivity(KeyUsageActivity.verifyCrl, signer);
                        crl.verify(signer.getPublicKey());
                    }
                    catch (Exception e) {
                        logger.log(Level.WARNING, "Verification of CRL signature failed '"+crlIssuerDn+"'.", e);
                        return CertificateValidationResult.REVOKED;
                    }
                } else {
                    throw new CRLException("No authorized signer found for CRL.");
                }

                if (crl.isRevoked(certificate)) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_REVOKED, certificate.getSubjectDN().toString());
                    return CertificateValidationResult.REVOKED;
                } else {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_NOT_REVOKED, certificate.getSubjectDN().toString());
                    return CertificateValidationResult.OK;
                }
            } catch (GeneralSecurityException e) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_CRL_INVALID, crlUrl, ExceptionUtils.getMessage(e));
                return CertificateValidationResult.REVOKED;
            } catch (IOException e) {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_RETRIEVAL_FAILED, new String[]{what(), crlUrl, ExceptionUtils.getMessage(e)}, e);
                return CertificateValidationResult.REVOKED;
            }
        }

        @Override
        protected String what() {
            return "CRL";
        }

        @Override
        protected String[] getUrlsFromCert(X509Certificate certificate, Audit auditor) throws IOException {
            return crlCache.getCrlUrlsFromCertificate(certificate, auditor);
        }
    }

    /**
     * Revocation checker that uses OCSP.
     *
     * This revocation checker will return UNKNOWN for the status if the
     * Certificate being checked is not covered by the OCSP responder.
     *
     * This revocation checker will return UNKNOWN for the status if the OCSP
     * URL is from the Certificate being checked and there is no URL that
     * matches the defined URL pattern.
     *
     * If the OCSP responder is unreachable or there is an error response then
     * REVOKED is returned (fail closed)
     */
    private static final class OCSPRevocationChecker extends AbstractRevocationChecker {
        private static final String OID_AIA_OCSP = "1.3.6.1.5.5.7.48.1";

        private final OCSPCache ocspCache;

        private OCSPRevocationChecker(final String url,
                                      final Pattern regex,
                                      final boolean allowIssuerSignature,
                                      final X509Certificate[] trustedIssuers,
                                      final OCSPCache ocspCache,
                                      final CertValidationProcessor certValidationProcessor) {
            super(url, regex, allowIssuerSignature, trustedIssuers, certValidationProcessor);
            this.ocspCache = ocspCache;
        }

        @Override
        protected String[] getUrlsFromCert(X509Certificate certificate, Audit auditor) throws IOException {
            try {
                return CertUtils.getAuthorityInformationAccessUris(certificate, OID_AIA_OCSP);
            } catch (CertificateException ce) {
                throw new IOException("Error extracting OCSP urls from certificate.", ce);
            }
        }

        @Override
        protected String what() {
            return "OCSP";
        }

        @Override
        public CertificateValidationResult getRevocationStatus(final X509Certificate certificate,
                                                               final X509Certificate issuerCertificate,
                                                               final Audit auditor,
                                                               final CertificateValidationResult onNetworkFailure) {
            CertificateValidationResult result = CertificateValidationResult.REVOKED;
            final String url = getValidUrl(certificate, auditor);
            if (url == null) {
                // {@link getCrlUrl()} already audited the reason
                return CertificateValidationResult.UNKNOWN;
            }

            try {
                OCSPClient.OCSPCertificateAuthorizer authorizer = new OCSPClient.OCSPCertificateAuthorizer(){
                    /**
                     * OCSP authorizer that gets the authorized signer from the superclass.
                     *
                     * This will pass in a callback to check for a "delegated" OCSP responder certificate.
                     */
                    @Override
                    public X509Certificate getAuthorizedSigner(final OCSPClient ocsp, final X509Certificate[] certificates) {
                        return OCSPRevocationChecker.this.getAuthorizedSigner(issuerCertificate, certificates, auditor, new Functions.Unary<Boolean,X509Certificate>(){
                            /**
                             * Callback that checks if the given certificate is allowed by the issuer and
                             * performs a revocation check on the certificate if needed.
                             */
                            @Override
                            public Boolean call(final X509Certificate x509Certificate) {
                                Boolean isPermittedSigner = Boolean.FALSE;
                                try {
                                    if (ocsp.isPermittedByIssuer(x509Certificate)) {
                                        if (ocsp.shouldCheckRevocation(x509Certificate)) {
                                            try {
                                                /**
                                                 * Check for revocation of the OCSP signer certificate.
                                                 */
                                                CertificateValidationResult cvr = getCertValidationProcessor().check(
                                                        new X509Certificate[]{x509Certificate},
                                                        null,
                                                        CertificateValidationType.REVOCATION,
                                                        null,
                                                        auditor);
                                                if (cvr == CertificateValidationResult.OK) {
                                                    isPermittedSigner = Boolean.TRUE;
                                                } else {
                                                    auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_SIGNER_CERT_REVOKED, url, x509Certificate.getSubjectDN().toString());
                                                    debugCertificate(x509Certificate);
                                                }
                                            } catch (GeneralSecurityException gse) {
                                                auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_SIGNER_CERT_REVOKED, url, x509Certificate.getSubjectDN().toString());
                                                debugCertificate(x509Certificate);
                                            }
                                        } else {
                                            isPermittedSigner = Boolean.TRUE;
                                        }
                                    }
                                } catch (OCSPClient.OCSPClientException oce) {
                                    logger.log(Level.WARNING, "Error checking if certificate '" + x509Certificate.getSubjectDN() +
                                            "' is permitted to sign OCSP responses.", oce);
                                }

                                return isPermittedSigner;
                            }
                        });
                    }
                };
                OCSPClient.OCSPStatus status = ocspCache.getOCSPStatus(url, certificate, issuerCertificate, authorizer, auditor);
                result = status.getResult();

                if (result == CertificateValidationResult.OK) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_NOT_REVOKED, certificate.getSubjectDN().toString());
                } else if (result == CertificateValidationResult.REVOKED) {
                    auditor.logAndAudit(SystemMessages.CERTVAL_REV_REVOKED, certificate.getSubjectDN().toString());
                }
            } catch (OCSPCache.OCSPClientRecursionException ocre) {
                auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_RECURSION, url);                
            } catch (OCSPClient.OCSPClientStatusException ocse) {
                auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_BAD_RESPONSE_STATUS, url, ExceptionUtils.getMessage(ocse));
            } catch (OCSPClient.OCSPClientException oce) {
                //noinspection ThrowableResultOfMethodCallIgnored
                auditor.logAndAudit(SystemMessages.CERTVAL_OCSP_ERROR, new String[]{url, ExceptionUtils.getMessage(oce)}, ExceptionUtils.getDebugException(oce));
                result = onNetworkFailure;
            }

            return result;
        }

        private void debugCertificate(X509Certificate certificate) {
            if (certificate != null) {
                if (logger.isLoggable(Level.FINE)) {
                    try {
                        logger.log(Level.FINE, "OCSP Responder rejected certificate\n{0}", CertUtils.encodeAsPEM(certificate));
                    } catch (IOException ioe) {
                        logger.log(Level.FINE, "OCSP Responder rejected certificate Subject DN ''{0}''.", certificate.getSubjectX500Principal());
                    } catch (CertificateEncodingException cee) {
                        logger.log(Level.FINE, "OCSP Responder rejected certificate Subject DN ''{0}''.", certificate.getSubjectX500Principal());
                    }
                }
            }
        }
    }

    private static final class RevokedChecker extends CompositeRevocationChecker {
        private RevokedChecker() {
            super(null, null, null, null);
        }
        @Override
        public CertificateValidationResult getRevocationStatus( final X509Certificate certificate, final X509Certificate issuer, final Audit auditor, final CertificateValidationResult onNetworkFailure ) {
            return CertificateValidationResult.REVOKED;
        }
    }
}
