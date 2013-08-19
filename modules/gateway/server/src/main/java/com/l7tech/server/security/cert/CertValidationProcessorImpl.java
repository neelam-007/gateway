package com.l7tech.server.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import sun.security.provider.certpath.AdjacencyList;
import sun.security.provider.certpath.BuildStep;
import sun.security.provider.certpath.SunCertPathBuilderException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.security.types.CertificateValidationResult.OK;
import static com.l7tech.security.types.CertificateValidationType.CERTIFICATE_ONLY;

/**
 * Implementation for CertValidationProcessor
 *
 * @author alex
 */
public class CertValidationProcessorImpl implements CertValidationProcessor, PostStartupApplicationListener, PropertyChangeListener, InitializingBean {
    private static final Logger logger = Logger.getLogger(CertValidationProcessorImpl.class.getName());

    private static final boolean USE_EXCEPTION_DECODER = ConfigFactory.getBooleanProperty( "com.l7tech.server.security.cert.useExceptionDecoder", true );
    private static final String PROP_USE_DEFAULT_ANCHORS = "pkixTrust.useDefaultAnchors";
    private static final String PROP_PERMITTED_CRITICAL_EXTENSIONS = "pkixTrust.permittedCriticalExtensions";

    private final DefaultKey defaultKey;
    private final TrustedCertManager trustedCertManager;
    private final RevocationCheckPolicyManager revocationCheckPolicyManager;
    private final Config config;
    private final RevocationCheckPolicy permissiveRcp;
    private CrlCache crlCache;
    private OCSPCache ocspCache;
    private RevocationCheckerFactory revocationCheckerFactory;
    private final BuilderExceptionDecoder exceptionDecoder;

    // lock and locked items
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, CertValidationCacheEntry> trustedCertsBySKI;
    private final Map<String, List<CertValidationCacheEntry>> trustedCertsByDn; // DN strings in RFC2253 format
    private final Map<IDNSerialKey, CertValidationCacheEntry> trustedCertsByIssuerDnAndSerial;
    private final Map<Goid, CertValidationCacheEntry> trustedCertsByOid;
    private final Map<Goid, RevocationCheckPolicy> revocationPoliciesByOid;
    private Map<String, List<TrustAnchor>> trustAnchorsByDn;  // DN strings in RFC2253 format
    private final Set<String> permittedCriticalExtensions = new HashSet<>();
    private CertStore certStore;
    private boolean cacheIsDirty = false;
    private RevocationCheckPolicy currentDefaultRevocationPolicy;
    // end locked items

    public CertValidationProcessorImpl(final TrustedCertManager trustedCertManager,
                                       final RevocationCheckPolicyManager revocationCheckPolicyManager,
                                       final DefaultKey defaultKey,
                                       final Config config )
    {
        if (trustedCertManager == null || revocationCheckPolicyManager == null || defaultKey == null || config == null) throw new NullPointerException("A required component is missing");
        this.trustedCertManager = trustedCertManager;
        this.revocationCheckPolicyManager = revocationCheckPolicyManager;
        //this.revocationCheckerFactory = new RevocationCheckerFactory(this, crlCache, ocspCache);
        this.defaultKey = defaultKey;
        this.permissiveRcp = new RevocationCheckPolicy();
        this.permissiveRcp.setDefaultSuccess(true);
        this.config = config;

        Map<String, List<CertValidationCacheEntry>> myDnCache = new HashMap<>();
        Map<IDNSerialKey, CertValidationCacheEntry> myIssuerDnCache = new HashMap<>();
        Map<Goid, CertValidationCacheEntry> myOidCache = new HashMap<>();
        Map<String, CertValidationCacheEntry> mySkiCache = new HashMap<>();
        Map<Goid, RevocationCheckPolicy> myRCPByIdCache = new HashMap<>();

        populateCaches(myDnCache, myIssuerDnCache, myOidCache, mySkiCache, myRCPByIdCache);

        this.trustedCertsByDn = myDnCache;
        this.trustedCertsByIssuerDnAndSerial = myIssuerDnCache;
        this.trustedCertsByOid = myOidCache;
        this.trustedCertsBySKI = mySkiCache;
        this.revocationPoliciesByOid = myRCPByIdCache;
        this.exceptionDecoder = getBuilderExceptionDecoder();
    }

    /**
     * 
     */
    @Override
    public CertificateValidationResult check(final X509Certificate[] endEntityCertificatePath,
                                             final CertificateValidationType minValType,
                                             final CertificateValidationType requestedValType,
                                             final Facility facility,
                                             final Audit auditor)
            throws CertificateException, SignatureException
    {
        if (endEntityCertificatePath == null || auditor == null) throw new NullPointerException("a required parameter is missing");
        if (requestedValType == null && facility == null) throw new NullPointerException("Either requestedValType or facility must be provided");
        if (endEntityCertificatePath.length==0 || endEntityCertificatePath[0]==null) throw new IllegalArgumentException("invalid certificate path");

        final X509Certificate endEntityCertificate = endEntityCertificatePath[0];
        final CertificateValidationType valType;
        if (requestedValType == null) {
            final String syspropname = "pkixValidation." + facility.name().toLowerCase();
            String stype = config.getProperty( syspropname );
            if (stype == null) {
                logger.warning(syspropname + " system property unavailable, using default " + CERTIFICATE_ONLY);
                valType = getAcceptedValidationLevel(CERTIFICATE_ONLY, minValType);
            } else if ("validate".equalsIgnoreCase(stype)) {
                valType = getAcceptedValidationLevel(CertificateValidationType.CERTIFICATE_ONLY, minValType);
            } else if ("validatepath".equalsIgnoreCase(stype)) {
                valType = getAcceptedValidationLevel(CertificateValidationType.PATH_VALIDATION, minValType);
            } else if ("revocation".equalsIgnoreCase(stype)) {
                valType = getAcceptedValidationLevel(CertificateValidationType.REVOCATION, minValType);
            } else {
                auditor.logAndAudit(SystemMessages.CERTVAL_INVALID_SETTING, syspropname, stype);
                valType = getAcceptedValidationLevel(CertificateValidationType.REVOCATION, minValType);
            }
        } else {
            valType = getAcceptedValidationLevel(requestedValType, minValType);
        }

        String subjectDnForLoggingOnly = endEntityCertificate.getSubjectDN().getName();

        if (valType == CERTIFICATE_ONLY)
            return checkCertificateOnly(endEntityCertificate, subjectDnForLoggingOnly, auditor);

        // Time to build a path
        final PKIXBuilderParameters pbp;
        try {
            pbp = makePKIXBuilderParameters(endEntityCertificatePath, valType == CertificateValidationType.REVOCATION, auditor);
        } catch (InvalidAlgorithmParameterException e) {
            auditor.logAndAudit(SystemMessages.CERTVAL_CANT_BUILD_PATH, new String[] { subjectDnForLoggingOnly, ExceptionUtils.getMessage(e) }, e.getCause());
            return CertificateValidationResult.CANT_BUILD_PATH;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Can't happen
        }
        try {
            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            builder.build(pbp);
            auditor.logAndAudit(SystemMessages.CERTVAL_CHECKED);            
            return OK;
        } catch (CertPathBuilderException e) {
            if (ExceptionUtils.causedBy(e, CertificateExpiredException.class)) {
                auditor.logAndAudit(SystemMessages.CERTVAL_CERT_EXPIRED, subjectDnForLoggingOnly);
            } else if (ExceptionUtils.causedBy(e, CertificateNotYetValidException.class)) {
                auditor.logAndAudit(SystemMessages.CERTVAL_CERT_NOT_YET_VALID, subjectDnForLoggingOnly);
            } else {
                auditor.logAndAudit(SystemMessages.CERTVAL_CANT_BUILD_PATH, new String[] { subjectDnForLoggingOnly, exceptionDecoder.describe(e) }, e.getCause());
            }
            return CertificateValidationResult.CANT_BUILD_PATH;
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateException("Unable to build Cert Path", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    @Override
    public void afterPropertiesSet() {
        if ( revocationCheckerFactory!=null )
            throw new IllegalStateException("already initialized");
        revocationCheckerFactory = new RevocationCheckerFactory(this, crlCache, ocspCache);
    }

    public void setCrlCache(CrlCache crlCache) {
        this.crlCache = crlCache;
    }

    public void setOcspCache(OCSPCache ocspCache) {
        this.ocspCache = ocspCache;
    }

    private CertificateValidationType getAcceptedValidationLevel(final CertificateValidationType type,
                                                                 final CertificateValidationType minType) {
        CertificateValidationType resultType = type;

        if (minType != null) {
            if ( minType.compareTo(type) > 0) {
                resultType = minType;    
            }
        }

        return resultType;
    }

    /**
     * Check that certificate is valid, any other checking (e.g. cert equals
     * known cert for user, cert is signed by the correct CA, etc) must be
     * performed by the caller as part of their authentication check.
     */
    private CertificateValidationResult checkCertificateOnly(final X509Certificate endEntityCertificate,
                                                             final String subjectDnForLoggingOnly,
                                                             final Audit auditor)
            throws SignatureException, CertificateException
    {
        // note that if the cert is invalid we won't actually get here
        // since this is checked when the credentials are gathered.
        try {
            endEntityCertificate.checkValidity();
        } catch (CertificateExpiredException cee) {
            auditor.logAndAudit(SystemMessages.CERTVAL_CERT_EXPIRED, subjectDnForLoggingOnly);
            throw cee;
        } catch (CertificateNotYetValidException cnyve) {
            auditor.logAndAudit(SystemMessages.CERTVAL_CERT_NOT_YET_VALID, subjectDnForLoggingOnly);
            throw cnyve;
        }

        auditor.logAndAudit(SystemMessages.CERTVAL_CHECKED);
        return CertificateValidationResult.OK;
    }

    @Override
    public TrustedCert getTrustedCertByOid(Goid oid) {
        lock.readLock().lock();
        try {
            TrustedCert trustedCert = null;
            CertValidationCacheEntry certValidationCacheEntry = trustedCertsByOid.get(oid);
            if ( certValidationCacheEntry != null ) {
                trustedCert = certValidationCacheEntry.tce;
            }
            return trustedCert;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get an X.509 certificate by subject distinguished name.
     *
     * <p>This will check all trusted certs and also any JDK trust anchors if
     * they are being trusted.</p>
     *
     * <p>The gateways CA certificate is also a candidate.</p>
     *
     * @param subjectDn The Subject DN
     * @return The certificate or null if not found.
     */
    @Override
    public X509Certificate getCertificateBySubjectDn(String subjectDn) {
        lock.readLock().lock();
        try {
            X509Certificate cert = null;
            final List<CertValidationCacheEntry> certValidationCacheEntry = trustedCertsByDn.get(subjectDn);
            if ( certValidationCacheEntry != null && !certValidationCacheEntry.isEmpty() ) {
                cert = certValidationCacheEntry.get( 0 ).cert;
            } else {
                final List<TrustAnchor> tas = trustAnchorsByDn.get(subjectDn);
                if ( tas != null && !tas.isEmpty() ) {
                    cert = tas.get( 0 ).getTrustedCert();
                }
            }
            return cert;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get an X.509 certificate by issuer distinguished name and serial number.
     *
     * <p>This will check all trusted certs and also any JDK trust anchors if
     * they are being trusted.</p>
     *
     * <p>The gateways CA certificate is also a candidate.</p>
     *
     * @param issuerDn The Issuer DN
     * @param serial The serial number
     * @return The certificate or null if not found.
     */
    @Override
    public X509Certificate getCertificateByIssuerDnAndSerial(String issuerDn, BigInteger serial) {
        lock.readLock().lock();
        try {
            X509Certificate cert = null;
            IDNSerialKey key = new IDNSerialKey(issuerDn, serial);
            CertValidationCacheEntry certValidationCacheEntry = trustedCertsByIssuerDnAndSerial.get(key);
            if ( certValidationCacheEntry != null ) {
                cert = certValidationCacheEntry.cert;
            } else {
                outer:
                for ( final List<TrustAnchor> tas : trustAnchorsByDn.values() ) {
                    for ( final TrustAnchor ta : tas ) {
                        final X509Certificate taCert = ta.getTrustedCert();
                        if ( key.matches(taCert) ) {
                            cert = taCert;
                            break outer;
                        }
                    }
                }
            }
            return cert;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get an X.509 certificate by subject key identifier.
     *
     * <p>This will check all trusted certs and also any JDK trust anchors if
     * they are being trusted.</p>
     *
     * <p>The gateways CA certificate is also a candidate.</p>
     *
     * @param base64SKI The Subject Key Identifier (from cert extension or generated)
     * @return The certificate or null if not found.
     */
    @Override
    public X509Certificate getCertificateBySKI(String base64SKI) {
        lock.readLock().lock();
        try {
            X509Certificate cert = null;
            CertValidationCacheEntry certValidationCacheEntry = trustedCertsBySKI.get(base64SKI);
            if ( certValidationCacheEntry != null ) {
                cert = certValidationCacheEntry.cert;
            } else {
                outer:
                for ( final List<TrustAnchor> tas : trustAnchorsByDn.values() ) {
                    for ( final TrustAnchor ta : tas ) {
                        final X509Certificate taCert = ta.getTrustedCert();
                        if ( base64SKI.equals( CertUtils.getSki(taCert)) ) {
                            cert = ta.getTrustedCert();
                            break outer;
                        }
                    }
                }
            }
            return cert;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public TrustedCert getTrustedCertEntry(X509Certificate certificate) {
        lock.readLock().lock();
        try {
            final String subjectDn = CertUtils.getSubjectDN(certificate);
            final List<CertValidationCacheEntry> entryList = trustedCertsByDn.get(subjectDn);
            if (entryList == null || entryList.isEmpty()) return null;
            for ( final CertValidationCacheEntry entry : entryList ) {
                if (CertUtils.certsAreEqual(certificate, entry.cert)) return entry.tce;
            }
            throw new IllegalArgumentException("Cached TrustedCert with DN " + subjectDn + " is different from presented certificate");
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public RevocationCheckPolicy getRevocationCheckPolicy(TrustedCert trustedCert) {
        switch(trustedCert.getRevocationCheckPolicyType()) {
            case NONE:
                return permissiveRcp;
            case SPECIFIED:
                lock.readLock().lock();
                try {
                    return revocationPoliciesByOid.get(trustedCert.getRevocationCheckPolicyOid());
                } finally {
                    lock.readLock().unlock();
                }
            case USE_DEFAULT:
                lock.readLock().lock();
                try {
                    return currentDefaultRevocationPolicy;
                } finally {
                    lock.readLock().unlock();
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public RevocationCheckPolicy getDefaultPolicy() {
        lock.readLock().lock();
        try {
            return currentDefaultRevocationPolicy;
        } finally {
            lock.readLock().unlock();
        }
    }

    private PKIXBuilderParameters makePKIXBuilderParameters(X509Certificate[] endEntityCertificatePath, boolean checkRevocation, Audit auditor) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        X509CertSelector sel;
        lock.readLock().lock();
        try {
            sel = new X509CertSelector();
            sel.setCertificate(endEntityCertificatePath[0]);
            Set<TrustAnchor> tempAnchors = new HashSet<>();
            tempAnchors.addAll( CollectionUtils.join( trustAnchorsByDn.values() ) );
            PKIXBuilderParameters pbp = new PKIXBuilderParameters(tempAnchors, sel);
            pbp.setRevocationEnabled(false); // We'll do our own
            if (checkRevocation)
                pbp.addCertPathChecker(new RevocationCheckingPKIXCertPathChecker(revocationCheckerFactory, pbp, auditor));
            pbp.addCertPathChecker( new PermitCriticalExtensionPKIXCertPathChecker(permittedCriticalExtensions) );
            pbp.addCertStore(certStore);
            pbp.addCertStore(CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters(Arrays.asList(endEntityCertificatePath))));
            return pbp;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PROP_USE_DEFAULT_ANCHORS.equals(event.getPropertyName())) {
            lock.writeLock().lock();
            try {
                if (logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE,
                            "Updating trust anchors due to cluster property change, use default anchors is now {0}.",
                            event.getNewValue());
                }
                initializeCertStoreAndTrustAnchors(trustedCertManager.findAll(), Boolean.valueOf((String)event.getNewValue()));
            } catch (FindException e) {
                logger.log(Level.WARNING, "Couldn't load TrustedCerts", e);
            } finally {
                lock.writeLock().unlock();
            }
        } else if (PROP_PERMITTED_CRITICAL_EXTENSIONS.equals(event.getPropertyName())) {
            lock.writeLock().lock();
            try {
                if (logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE,
                            "Updating permitted critical extensions due to cluster property change, value is now {0}.",
                            event.getNewValue());
                }
                refreshPermittedCriticalExtensions();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof GoidEntityInvalidationEvent) {
            boolean isDirty = false;
            lock.readLock().lock();
            try {
                isDirty = cacheIsDirty;
            } finally {
                lock.readLock().unlock();
            }

            if ( !isDirty ) {
                // incremental update
                processEntityInvalidationEvent((GoidEntityInvalidationEvent) applicationEvent);
            } else {
                // full reload
                lock.writeLock().lock();
                try {
                    populateCaches(trustedCertsByDn,
                            trustedCertsByIssuerDnAndSerial,
                            trustedCertsByOid,
                            trustedCertsBySKI,
                            revocationPoliciesByOid);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }

    private void populateCaches(Map<String, List<CertValidationCacheEntry>> myDnCache,
                                Map<IDNSerialKey, CertValidationCacheEntry> myIssuerDnCache,
                                Map<Goid, CertValidationCacheEntry> myOidCache,
                                Map<String, CertValidationCacheEntry> mySkiCache,
                                Map<Goid, RevocationCheckPolicy> myRcpsByOid) {
        logger.info("(Re-)Populating certificate validation caches.");

        // clear existing data
        myDnCache.clear();
        myIssuerDnCache.clear();
        myOidCache.clear();
        mySkiCache.clear();
        myRcpsByOid.clear();


        // Set up initial RevocationCheckPolicy caches
        Collection<RevocationCheckPolicy> rcps;
        try {
            rcps = revocationCheckPolicyManager.findAll();
        } catch (FindException e) {
            throw new RuntimeException("Unable to find RevocationCheckPolicies", e);
        }

        RevocationCheckPolicy defaultRcp = null;
        for (RevocationCheckPolicy rcp : rcps) {
            if (rcp.isDefaultPolicy()) {
                if (defaultRcp != null) throw new IllegalStateException("Multiple RevocationCheckPolicies are flagged as default");
                defaultRcp = rcp;
            }
            myRcpsByOid.put(rcp.getGoid(), rcp);
        }
        if (defaultRcp == null) defaultRcp = new RevocationCheckPolicy(); // Always fails
        this.currentDefaultRevocationPolicy = defaultRcp;

        // Setup initial TrustedCert caches
        Collection<TrustedCert> tces;
        try {
            tces = trustedCertManager.findAll();
        } catch (FindException e) {
            throw new RuntimeException("Unable to find Trusted Certs", e);
        }

        for (TrustedCert tce : tces) {
            try {
                final CertValidationCacheEntry entry = new CertValidationCacheEntry(tce);
                addToEntryList( myDnCache, tce.getSubjectDn(), entry );
                myOidCache.put(tce.getGoid(), entry);
                mySkiCache.put(tce.getSki(), entry);
                // last since it may throw on invalid data
                myIssuerDnCache.put(new IDNSerialKey(entry.issuerDn, entry.serial), entry);
            } catch (CertificateException | IllegalArgumentException e) {
                logger.log(Level.WARNING, "Couldn't load TrustedCert #{0} ({1}): " + ExceptionUtils.getMessage(e), new Object[] { tce.getGoid(), tce.getSubjectDn() });
            }
        }

        initializeCertStoreAndTrustAnchors(tces, config.getBooleanProperty( PROP_USE_DEFAULT_ANCHORS, false ));

        refreshPermittedCriticalExtensions();

        cacheIsDirty = false;
    }

    private void processEntityInvalidationEvent(final GoidEntityInvalidationEvent eie) {
        if (TrustedCert.class.isAssignableFrom(eie.getEntityClass())) {
            final Goid[] ids = eie.getEntityIds();
            final char[] ops = eie.getEntityOperations();
            lock.writeLock().lock();
            try {
                nextOid: for (int i = 0; i < ids.length; i++) {
                    Goid oid = ids[i];
                    char op = ops[i];
                    switch(op) {
                        case GoidEntityInvalidationEvent.CREATE:
                        case GoidEntityInvalidationEvent.UPDATE:
                            if (logger.isLoggable(Level.FINE) ) {
                                logger.log(Level.FINE, "Updating cache due to trusted certificate add/update, OID is {0}.", oid);
                            }
                            try {
                                addTrustedCertToCaches(trustedCertManager.findByPrimaryKey(oid));
                                break;
                            } catch (Exception e) {
                                cacheIsDirty = true;
                                logger.log(Level.WARNING, "Couldn't load recently created or updated TrustedCert #" + oid, e);
                                continue nextOid;
                            }
                        case GoidEntityInvalidationEvent.DELETE:
                            if (logger.isLoggable(Level.FINE) ) {
                                logger.log(Level.FINE, "Updating cache due to trusted certificate deletion, OID is {0}.", oid);
                            }
                            removeTrustedCertFromCaches(oid);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected invalidation operation: '" + op + "'");
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        } else if (RevocationCheckPolicy.class.isAssignableFrom(eie.getEntityClass())) {
            final Goid[] ids = eie.getEntityIds();
            final char[] ops = eie.getEntityOperations();
            lock.writeLock().lock();
            try {
                nextOid: for (int i = 0; i < ids.length; i++) {
                    Goid oid = ids[i];
                    char op = ops[i];
                    switch(op) {
                        case GoidEntityInvalidationEvent.CREATE:
                        case GoidEntityInvalidationEvent.UPDATE:
                            if (logger.isLoggable(Level.FINE) ) {
                                logger.log(Level.FINE, "Updating cache due to revocation check policy add/update, OID is {0}.", oid);
                            }
                            try {
                                addRCPToCaches(revocationCheckPolicyManager.findByPrimaryKey(oid));
                                break;
                            } catch (Exception e) {
                                cacheIsDirty = true;
                                logger.log(Level.WARNING, "Couldn't load recently created or updated RevocationCheckPolicy #" + oid, e);
                                continue nextOid;
                            }
                        case GoidEntityInvalidationEvent.DELETE:
                            if (logger.isLoggable(Level.FINE) ) {
                                logger.log(Level.FINE, "Updating cache due to revocation check policy deletion, OID is {0}.", oid);
                            }
                            removeRCPFromCaches(oid);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected invalidation operation: '" + op + "'");
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /** Caller must hold write lock (or be the constructor) */
    private void initializeCertStoreAndTrustAnchors(Collection<TrustedCert> tces, boolean includeDefaults) {
        Map<String, List<TrustAnchor>> anchors = new HashMap<>();
        Set<X509Certificate> nonAnchors = new HashSet<>();

        SignerInfo caInfo = defaultKey.getCaInfo();
        if (caInfo != null)
            addToEntryList( anchors, CertUtils.getSubjectDN( caInfo.getCertificate()), new TrustAnchor(caInfo.getCertificate(), null) );

        if ( includeDefaults ) {
            TrustManagerFactory tmf;
            try {
                final String alg = TrustManagerFactory.getDefaultAlgorithm();
                Provider prov = JceProvider.getInstance().getProviderFor("TrustManagerFactory." + alg);
                tmf = prov == null ? TrustManagerFactory.getInstance(alg) : TrustManagerFactory.getInstance(alg, prov);
                tmf.init((KeyStore)null);
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                logger.log(Level.SEVERE, e.toString(), e);
                throw new RuntimeException(e);
            }
            TrustManager[] defaultTrustManagers = tmf.getTrustManagers();
            X509TrustManager x509tm = null;
            for (TrustManager tm : defaultTrustManagers) {
                if (tm instanceof X509TrustManager) {
                    if (x509tm != null) throw new IllegalStateException("Found two X509TrustManagers");
                    x509tm = (X509TrustManager) tm;
                }
            }
            if (x509tm == null) throw new IllegalStateException("Couldn't find an X509TrustManager");
            X509Certificate[] extraAnchors = x509tm.getAcceptedIssuers();
            for (X509Certificate certificate : extraAnchors) {
                addToEntryList( anchors, CertUtils.getSubjectDN(certificate), new TrustAnchor(certificate, null));
            }
        }

        for (TrustedCert tce : tces) {
            if (tce.isTrustAnchor()) {
                addToEntryList( anchors, tce.getSubjectDn(), new TrustAnchor(tce.getCertificate(), null));
            } else {
                nonAnchors.add(tce.getCertificate());
            }
        }

        this.trustAnchorsByDn = Collections.unmodifiableMap(anchors);
        try {
            this.certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(Collections.unmodifiableSet(nonAnchors)));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e); // Can't happen (we think!)
        }
    }

    /** Caller must hold write lock (or be the constructor) */
    private void refreshPermittedCriticalExtensions() {
        this.permittedCriticalExtensions.clear();
        this.permittedCriticalExtensions.addAll( Arrays.asList( config.getProperty(PROP_PERMITTED_CRITICAL_EXTENSIONS, "").split("\\s") ) );
    }

    /** Caller must hold write lock */
    private void removeRCPFromCaches(Goid oid) {
        revocationCheckerFactory.invalidateRevocationCheckPolicy( oid );
        revocationPoliciesByOid.remove( oid );
        if (currentDefaultRevocationPolicy.getGoid().equals(oid)) {
            currentDefaultRevocationPolicy = null;
            logger.fine("Default revocation policy deleted; hopefully soon we'll be notified that a new one was set as default");
        }
    }

    /** Caller must hold write lock */
    private void addRCPToCaches(RevocationCheckPolicy policy) {
        if ( policy.isDefaultPolicy() ) {
            currentDefaultRevocationPolicy = policy;
        } else if (currentDefaultRevocationPolicy!=null &&
            currentDefaultRevocationPolicy.getGoid() != null &&
            currentDefaultRevocationPolicy.getGoid().equals(policy.getGoid())) {
            currentDefaultRevocationPolicy = null;
        }
        revocationPoliciesByOid.put( policy.getGoid(), policy );
        revocationCheckerFactory.invalidateRevocationCheckPolicy(policy.getGoid());
    }

    /** Caller must hold write lock */
    private void removeTrustedCertFromCaches(Goid oid) {
        revocationCheckerFactory.invalidateTrustedCert(oid);
        CertValidationCacheEntry entry = trustedCertsByOid.remove(oid);
        if (entry == null) return;
        removeFromEntryList( trustedCertsByDn, entry.subjectDn, certValidationCacheEntryMatcher( entry.cert ) );
        trustedCertsBySKI.remove(entry.ski);
        trustedCertsByIssuerDnAndSerial.remove(new IDNSerialKey(entry.issuerDn, entry.serial));
        if(trustAnchorsByDn.containsKey(entry.subjectDn)) {
            final Map<String, List<TrustAnchor>> updatedTrustAnchors = new HashMap<>(trustAnchorsByDn);
            removeFromEntryList( updatedTrustAnchors, entry.subjectDn, trustAnchorMatcher( entry.cert ) );
            trustAnchorsByDn = Collections.unmodifiableMap(updatedTrustAnchors);
        }
    }

    /** Caller must hold write lock */
    private void addTrustedCertToCaches(TrustedCert tce) throws CertificateException {
        // add to trusted cert cache
        CertValidationCacheEntry entry = new CertValidationCacheEntry(tce);
        addToEntryList( trustedCertsByDn, tce.getSubjectDn(), entry, certValidationCacheEntryMatcher(entry.cert) );
        trustedCertsBySKI.put( entry.ski, entry );
        trustedCertsByOid.put( tce.getGoid(), entry );
        trustedCertsByIssuerDnAndSerial.put( new IDNSerialKey( entry.issuerDn, entry.serial ), entry );
        if ( logger.isLoggable( Level.FINE ) ) {
            logger.log(Level.FINE, "Added certificate ''{0}'', to anchors store.", entry.subjectDn);
        }

        // update TrustAnchors and known certs
        Map<String, List<TrustAnchor>> anchors = new HashMap<>(trustAnchorsByDn);
        if ( tce.isTrustAnchor() ) {
            final TrustAnchor old = addToEntryList(anchors, tce.getSubjectDn(), new TrustAnchor(entry.cert, null), trustAnchorMatcher(entry.cert));
            if ( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Added certificate ''{0}'', to anchors store, replaced ''{1}''.",
                        new Object[]{entry.subjectDn, old==null?"<NULL>":old.getTrustedCert().getSubjectDN()});
            }
        } else {
            boolean removed = removeFromEntryList(anchors, tce.getSubjectDn(), trustAnchorMatcher(entry.cert))!=null;
            if ( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Removed certificate ''{0}'', from anchors store ({1}).",
                        new Object[]{entry.subjectDn, removed});
            }
        }
        trustAnchorsByDn = Collections.unmodifiableMap(anchors);

        try {
            Set<Certificate> nonAnchors = new HashSet<>(certStore.getCertificates(null));
            if ( tce.isTrustAnchor() ) {
                boolean removed = nonAnchors.remove(entry.cert);
                if ( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "Removed certificate ''{0}'', from non-anchors store ({1}).",
                            new Object[]{entry.subjectDn, removed});
                }
            } else {
                boolean added = nonAnchors.add(entry.cert);
                if ( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "Added certificate ''{0}'', to non-anchors store ({1}).",
                            new Object[]{entry.subjectDn, added});
                }
            }
            this.certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(Collections.unmodifiableSet(nonAnchors)));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e); // Can't happen (we think!)
        }

        //
        revocationCheckerFactory.invalidateTrustedCert(tce.getGoid());
    }

    private <K,V> void addToEntryList( final Map<K,List<V>> map,
                                       final K key,
                                       final V value ) {
        addToEntryList( map, key, value, null );
    }

    private <K,V> V addToEntryList( final Map<K,List<V>> map,
                                    final K key,
                                    final V value,
                                    @Nullable final Functions.Unary<Boolean,V> matcher ) {
        V replaced = null;
        final List<V> values = map.get( key );
        final List<V> updatedList;
        if ( values == null ) {
            updatedList = Collections.singletonList( value );
        } else if ( matcher == null ) {
            final List<V> valuesCopy = new ArrayList<>( values );
            valuesCopy.add( value );
            updatedList = Collections.unmodifiableList( valuesCopy );
        } else {
            final List<V> valuesCopy = new ArrayList<>( values );
            int replaceIndex = -1;
            for ( int i=0; i<valuesCopy.size(); i++ ) {
                if ( matcher.call( valuesCopy.get( i ) ) ) {
                    replaceIndex = i;
                    break;
                }
            }

            if ( replaceIndex >= 0 ) {
                replaced = valuesCopy.remove( replaceIndex );
                valuesCopy.add( replaceIndex, value );
            } else {
                valuesCopy.add( value );
            }
            updatedList = Collections.unmodifiableList( valuesCopy );
        }

        map.put( key, updatedList );

        return replaced;
    }

    private <K,V> V removeFromEntryList( final Map<K,List<V>> map,
                                         final K key,
                                         final Functions.Unary<Boolean,V> matcher ) {
        V removed = null;
        final List<V> values = map.get( key );
        List<V> updatedList = null;
        if ( values != null ) {
            final List<V> valuesCopy = new ArrayList<>( values );
            int removeIndex = -1;
            for ( int i=0; i<valuesCopy.size(); i++ ) {
                if ( matcher.call( valuesCopy.get( i ) ) ) {
                    removeIndex = i;
                    break;
                }
            }

            if ( removeIndex >= 0 ) {
                removed = valuesCopy.get( removeIndex );
                if ( valuesCopy.size() > 1 ) {
                    valuesCopy.remove( removeIndex );
                    updatedList = Collections.unmodifiableList( valuesCopy );
                }
            } else {
                updatedList = values; // no change
            }
        }

        if ( updatedList != null ) {
            map.put( key, updatedList );
        } else {
            map.remove( key );
        }

        return removed;
    }

    private Functions.Unary<Boolean,TrustAnchor> trustAnchorMatcher( final X509Certificate certificate ) {
        return new Functions.Unary<Boolean,TrustAnchor>() {
            @Override
            public Boolean call( final TrustAnchor trustAnchor ) {
                return CertUtils.certsAreEqual( certificate, trustAnchor.getTrustedCert() );
            }
        };
    }

    private Functions.Unary<Boolean,CertValidationCacheEntry> certValidationCacheEntryMatcher( final X509Certificate certificate ) {
        return new Functions.Unary<Boolean,CertValidationCacheEntry>() {
            @Override
            public Boolean call( final CertValidationCacheEntry certValidationCacheEntry ) {
                return CertUtils.certsAreEqual( certificate, certValidationCacheEntry.cert );
            }
        };
    }

    private static final class IDNSerialKey {
        private final String issuerDn;
        private final BigInteger serial;

        IDNSerialKey(String issuerDn, BigInteger serial) {
            if (issuerDn == null) throw new IllegalArgumentException("issuerDn must not be null");
            if (serial == null) throw new IllegalArgumentException("serial must not be null");
            this.issuerDn = issuerDn;
            this.serial = serial;
        }

        String getIssuerDn() {
            return issuerDn;
        }

        BigInteger getSerial() {
            return serial;
        }

        boolean matches(X509Certificate certificate) {
            boolean match = false;
            String issuerDn = CertUtils.getIssuerDN( certificate );
            BigInteger serial = certificate.getSerialNumber();

            if ( issuerDn != null && serial != null) {
                if ( issuerDn.equals(getIssuerDn()) &&
                     serial.equals(getSerial())) {
                    match = true;
                }
            }

            return match;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IDNSerialKey that = (IDNSerialKey) o;

            if (!issuerDn.equals(that.issuerDn)) return false;
            if (!serial.equals(that.serial)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = issuerDn.hashCode();
            result = 31 * result + serial.hashCode();
            return result;
        }
    }

    /**
     * Certificate path checker that supports whatever extensions it is configured to support.
     */
    private static final class PermitCriticalExtensionPKIXCertPathChecker extends PKIXCertPathChecker {
        private final Set<String> extensions;
        
        private PermitCriticalExtensionPKIXCertPathChecker( final Set<String> extensions ) {
            this.extensions = Collections.unmodifiableSet( new HashSet<>(extensions) );
        }

        @Override
        public void check( final Certificate cert,
                           final Collection<String> unresolvedCriticalExtensions ) throws CertPathValidatorException {
            unresolvedCriticalExtensions.removeAll( extensions );
        }

        @Override
        public void init( final boolean forward ) throws CertPathValidatorException {
        }

        @Override
        public boolean isForwardCheckingSupported() {
            return true;
        }

        @Override
        public Set<String> getSupportedExtensions() {
            return extensions;
        }
    }

    private static BuilderExceptionDecoder getBuilderExceptionDecoder() {
        try {
            return USE_EXCEPTION_DECODER ? new SunBuilderExceptionDecoder() : new DefaultBuilderExceptionDecoder();
        } catch( NoClassDefFoundError e ) {
            return new DefaultBuilderExceptionDecoder();
        }
    }

    private interface BuilderExceptionDecoder {
        String describe( CertPathBuilderException exception );
    }

    private static final class DefaultBuilderExceptionDecoder implements BuilderExceptionDecoder {
        @Override
        public String describe( final CertPathBuilderException exception ) {
            return ExceptionUtils.getMessage( exception );
        }
    }

    private static final class SunBuilderExceptionDecoder implements BuilderExceptionDecoder {
        final BuilderExceptionDecoder fallbackDecoder = new DefaultBuilderExceptionDecoder();

        @SuppressWarnings({ "UseOfSunClasses" })
        @Override
        public String describe( final CertPathBuilderException exception ) {
            String description = fallbackDecoder.describe( exception );

            if ( exception instanceof SunCertPathBuilderException ) {
                final SunCertPathBuilderException sunException = (SunCertPathBuilderException) exception;
                final AdjacencyList list = sunException.getAdjacencyList();
                if ( list != null ) {
                    final List<String> errorsDuringBuild = new ArrayList<>();
                    final Iterator<BuildStep> stepIterator = list.iterator();
                    while ( stepIterator.hasNext() ) {
                        final BuildStep buildStep = stepIterator.next();
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if ( buildStep.getThrowable() != null ) {
                            errorsDuringBuild.add( ExceptionUtils.getMessage( buildStep.getThrowable() ) );
                        }
                    }

                    if ( !errorsDuringBuild.isEmpty() ) {
                        description = description + "; related error(s) " + errorsDuringBuild;
                    }
                }
            }

            return description;
        }
    }
}
