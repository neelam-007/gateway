/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.types.CertificateValidationResult;
import static com.l7tech.security.types.CertificateValidationResult.OK;
import com.l7tech.security.types.CertificateValidationType;
import static com.l7tech.security.types.CertificateValidationType.CERTIFICATE_ONLY;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

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

/**
 * Implementation for CertValidationProcessor
 *
 * @author alex
 */
public class CertValidationProcessorImpl implements CertValidationProcessor, ApplicationListener, PropertyChangeListener, InitializingBean {
    private static final Logger logger = Logger.getLogger(CertValidationProcessorImpl.class.getName());

    private static final String PROP_USE_DEFAULT_ANCHORS = "pkixTrust.useDefaultAnchors";

    private final DefaultKey defaultKey;
    private final TrustedCertManager trustedCertManager;
    private final RevocationCheckPolicyManager revocationCheckPolicyManager;
    private final ServerConfig serverConfig;
    private final RevocationCheckPolicy permissiveRcp;
    private CrlCache crlCache;
    private OCSPCache ocspCache;
    private RevocationCheckerFactory revocationCheckerFactory;

    // lock and locked items
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, CertValidationCacheEntry> trustedCertsBySKI;
    private final Map<String, CertValidationCacheEntry> trustedCertsByDn; // DN strings in RFC2253 format
    private final Map<IDNSerialKey, CertValidationCacheEntry> trustedCertsByIssuerDnAndSerial;
    private final Map<Long, CertValidationCacheEntry> trustedCertsByOid;
    private final Map<Long, RevocationCheckPolicy> revocationPoliciesByOid;
    private Map<String, TrustAnchor> trustAnchorsByDn;  // DN strings in RFC2253 format
    private CertStore certStore;
    private boolean cacheIsDirty = false;
    private RevocationCheckPolicy currentDefaultRevocationPolicy;
    // end locked items

    public CertValidationProcessorImpl(final TrustedCertManager trustedCertManager,
                                       final RevocationCheckPolicyManager revocationCheckPolicyManager,
                                       final DefaultKey defaultKey,
                                       final ServerConfig serverConfig)
    {
        if (trustedCertManager == null || revocationCheckPolicyManager == null || defaultKey == null || serverConfig == null) throw new NullPointerException("A required component is missing");
        this.trustedCertManager = trustedCertManager;
        this.revocationCheckPolicyManager = revocationCheckPolicyManager;
        //this.revocationCheckerFactory = new RevocationCheckerFactory(this, crlCache, ocspCache);
        this.defaultKey = defaultKey;
        this.permissiveRcp = new RevocationCheckPolicy();
        this.permissiveRcp.setDefaultSuccess(true);
        this.serverConfig = serverConfig;

        Map<String, CertValidationCacheEntry> myDnCache = new HashMap<String, CertValidationCacheEntry>();
        Map<IDNSerialKey, CertValidationCacheEntry> myIssuerDnCache = new HashMap<IDNSerialKey, CertValidationCacheEntry>();
        Map<Long, CertValidationCacheEntry> myOidCache = new HashMap<Long, CertValidationCacheEntry>();
        Map<String, CertValidationCacheEntry> mySkiCache = new HashMap<String, CertValidationCacheEntry>();
        Map<Long, RevocationCheckPolicy> myRCPByIdCache = new HashMap<Long, RevocationCheckPolicy>();

        populateCaches(myDnCache, myIssuerDnCache, myOidCache, mySkiCache, myRCPByIdCache);

        this.trustedCertsByDn = myDnCache;
        this.trustedCertsByIssuerDnAndSerial = myIssuerDnCache;
        this.trustedCertsByOid = myOidCache;
        this.trustedCertsBySKI = mySkiCache;
        this.revocationPoliciesByOid = myRCPByIdCache;
    }

    /**
     * 
     */
    @Override
    public CertificateValidationResult check(final X509Certificate[] endEntityCertificatePath,
                                             final CertificateValidationType minValType,
                                             final CertificateValidationType requestedValType,
                                             final Facility facility,
                                             final Auditor auditor)
            throws CertificateException, SignatureException
    {
        if (endEntityCertificatePath == null || auditor == null) throw new NullPointerException("a required parameter is missing");
        if (requestedValType == null && facility == null) throw new NullPointerException("Either requestedValType or facility must be provided");
        if (endEntityCertificatePath.length==0 || endEntityCertificatePath[0]==null) throw new IllegalArgumentException("invalid certificate path");

        final X509Certificate endEntityCertificate = endEntityCertificatePath[0];
        final CertificateValidationType valType;
        if (requestedValType == null) {
            final String syspropname = "pkixValidation." + facility.name().toLowerCase();
            String stype = serverConfig.getPropertyCached(syspropname);
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
        final PKIXBuilderParameters pbp = makePKIXBuilderParameters(endEntityCertificatePath,
                                                                    valType == CertificateValidationType.REVOCATION,
                                                                    auditor);
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
                auditor.logAndAudit(SystemMessages.CERTVAL_CANT_BUILD_PATH, new String[] { subjectDnForLoggingOnly, ExceptionUtils.getMessage(e) }, e.getCause());
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
                                                             final Auditor auditor)
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
    public TrustedCert getTrustedCertByOid(long oid) {
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
            CertValidationCacheEntry certValidationCacheEntry = trustedCertsByDn.get(subjectDn);
            if ( certValidationCacheEntry != null ) {
                cert = certValidationCacheEntry.cert;
            } else {
                TrustAnchor ta = trustAnchorsByDn.get(subjectDn);
                if ( ta != null ) {
                    cert = ta.getTrustedCert();
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
                for ( TrustAnchor ta : trustAnchorsByDn.values() ) {
                    X509Certificate taCert = ta.getTrustedCert();
                    if (key.matches(taCert)) {
                        cert = taCert;
                        break;
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
                for ( TrustAnchor ta : trustAnchorsByDn.values() ) {
                    X509Certificate taCert = ta.getTrustedCert();
                    if ( base64SKI.equals( CertUtils.getSki(taCert)) ) {
                        cert = ta.getTrustedCert();
                        break;
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
            final CertValidationCacheEntry entry = trustedCertsByDn.get(subjectDn);
            if (entry == null) return null;
            if (CertUtils.certsAreEqual(certificate, entry.cert)) return entry.tce;
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

    private PKIXBuilderParameters makePKIXBuilderParameters(X509Certificate[] endEntityCertificatePath, boolean checkRevocation, Auditor auditor) {
        X509CertSelector sel;
        lock.readLock().lock();
        try {
            sel = new X509CertSelector();
            sel.setCertificate(endEntityCertificatePath[0]);
            try {
                Set<TrustAnchor> tempAnchors = new HashSet<TrustAnchor>();
                tempAnchors.addAll(trustAnchorsByDn.values());
                PKIXBuilderParameters pbp = new PKIXBuilderParameters(tempAnchors, sel);
                pbp.setRevocationEnabled(false); // We'll do our own
                if (checkRevocation)
                    pbp.addCertPathChecker(new RevocationCheckingPKIXCertPathChecker(revocationCheckerFactory, pbp, auditor));
                pbp.addCertStore(certStore);
                pbp.addCertStore(CertStore.getInstance("Collection",
                        new CollectionCertStoreParameters(Arrays.asList(endEntityCertificatePath))));
                return pbp;
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // Can't happen
            }
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
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityInvalidationEvent) {
            boolean isDirty = false;
            lock.readLock().lock();
            try {
                isDirty = cacheIsDirty;
            } finally {
                lock.readLock().unlock();
            }

            if ( !isDirty ) {
                // incremental update
                processEntityInvalidationEvent((EntityInvalidationEvent) applicationEvent);
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

    private void populateCaches(Map<String, CertValidationCacheEntry> myDnCache,
                                Map<IDNSerialKey, CertValidationCacheEntry> myIssuerDnCache,
                                Map<Long, CertValidationCacheEntry> myOidCache,
                                Map<String, CertValidationCacheEntry> mySkiCache,
                                Map<Long, RevocationCheckPolicy> myRcpsByOid) {
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
            myRcpsByOid.put(rcp.getOid(), rcp);
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
                myDnCache.put(tce.getSubjectDn(), entry);
                myOidCache.put(tce.getOid(), entry);
                mySkiCache.put(tce.getSki(), entry);
                // last since it may throw on invalid data
                myIssuerDnCache.put(new IDNSerialKey(entry.issuerDn, entry.serial), entry);
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "Couldn't load TrustedCert #{0} ({1}): " + ExceptionUtils.getMessage(e), new Object[] { tce.getOid(), tce.getSubjectDn() });
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Couldn't load TrustedCert #{0} ({1}): " + ExceptionUtils.getMessage(e), new Object[] { tce.getOid(), tce.getSubjectDn() });
            }
        }

        initializeCertStoreAndTrustAnchors(tces, Boolean.valueOf(serverConfig.getProperty(PROP_USE_DEFAULT_ANCHORS)));

        cacheIsDirty = false;
    }

    private void processEntityInvalidationEvent(final EntityInvalidationEvent eie) {
        if (TrustedCert.class.isAssignableFrom(eie.getEntityClass())) {
            final long[] ids = eie.getEntityIds();
            final char[] ops = eie.getEntityOperations();
            lock.writeLock().lock();
            try {
                nextOid: for (int i = 0; i < ids.length; i++) {
                    long oid = ids[i];
                    char op = ops[i];
                    switch(op) {
                        case EntityInvalidationEvent.CREATE:
                        case EntityInvalidationEvent.UPDATE:
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
                        case EntityInvalidationEvent.DELETE:
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
            final long[] ids = eie.getEntityIds();
            final char[] ops = eie.getEntityOperations();
            lock.writeLock().lock();
            try {
                nextOid: for (int i = 0; i < ids.length; i++) {
                    long oid = ids[i];
                    char op = ops[i];
                    switch(op) {
                        case EntityInvalidationEvent.CREATE:
                        case EntityInvalidationEvent.UPDATE:
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
                        case EntityInvalidationEvent.DELETE:
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
        Map<String, TrustAnchor> anchors = new HashMap<String, TrustAnchor>();
        Set<X509Certificate> nonAnchors = new HashSet<X509Certificate>();

        SignerInfo caInfo = defaultKey.getCaInfo();
        if (caInfo != null)
            anchors.put(CertUtils.getSubjectDN( caInfo.getCertificate()), new TrustAnchor(caInfo.getCertificate(), null));

        if ( includeDefaults ) {
            TrustManagerFactory tmf;
            try {
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore)null);
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, e.toString(), e);
                throw new RuntimeException(e);
            } catch (KeyStoreException e) {
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
                anchors.put(CertUtils.getSubjectDN(certificate), new TrustAnchor(certificate, null));
            }
        }

        for (TrustedCert tce : tces) {
            if (tce.isTrustAnchor()) {
                anchors.put(tce.getSubjectDn(), new TrustAnchor(tce.getCertificate(), null));
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

    /** Caller must hold write lock */
    private void removeRCPFromCaches(long oid) {
        revocationCheckerFactory.invalidateRevocationCheckPolicy(oid);
        revocationPoliciesByOid.remove(oid);
        if (currentDefaultRevocationPolicy.getOid() == oid) {
            currentDefaultRevocationPolicy = null;
            logger.fine("Default revocation policy deleted; hopefully soon we'll be notified that a new one was set as default");
        }
    }

    /** Caller must hold write lock */
    private void addRCPToCaches(RevocationCheckPolicy policy) {
        if ( policy.isDefaultPolicy() ) {
            currentDefaultRevocationPolicy = policy;
        } else if (currentDefaultRevocationPolicy!=null && currentDefaultRevocationPolicy.getOid()==policy.getOid()) {
            currentDefaultRevocationPolicy = null;
        }
        revocationPoliciesByOid.put(policy.getOid(), policy);
        revocationCheckerFactory.invalidateRevocationCheckPolicy(policy.getOid());
    }

    /** Caller must hold write lock */
    private void removeTrustedCertFromCaches(long oid) {
        revocationCheckerFactory.invalidateTrustedCert(oid);
        CertValidationCacheEntry entry = trustedCertsByOid.remove(oid);
        if (entry == null) return;
        trustedCertsByDn.remove(entry.subjectDn);
        trustedCertsBySKI.remove(entry.ski);
        trustedCertsByIssuerDnAndSerial.remove(new IDNSerialKey(entry.issuerDn, entry.serial));
        if(trustAnchorsByDn.containsKey(entry.subjectDn)) {
            Map<String, TrustAnchor> updatedTrustAnchors = new HashMap<String, TrustAnchor>(trustAnchorsByDn);
            updatedTrustAnchors.remove(entry.subjectDn);
            trustAnchorsByDn = Collections.unmodifiableMap(updatedTrustAnchors);
        }
    }

    /** Caller must hold write lock */
    private void addTrustedCertToCaches(TrustedCert tce) throws CertificateException {
        // add to trusted cert cache
        CertValidationCacheEntry entry = new CertValidationCacheEntry(tce);
        trustedCertsByDn.put(tce.getSubjectDn(), entry);
        trustedCertsBySKI.put(entry.ski, entry);
        trustedCertsByOid.put(tce.getOid(), entry);
        trustedCertsByIssuerDnAndSerial.put(new IDNSerialKey(entry.issuerDn, entry.serial), entry);
        if ( logger.isLoggable(Level.FINE) ) {
            logger.log(Level.FINE, "Added certificate ''{0}'', to anchors store.", entry.subjectDn);
        }

        // update TrustAnchors and known certs
        Map<String, TrustAnchor> anchors = new HashMap<String, TrustAnchor>(trustAnchorsByDn);
        if ( tce.isTrustAnchor() ) {
            TrustAnchor old = anchors.put(tce.getSubjectDn(), new TrustAnchor(entry.cert, null));
            if ( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Added certificate ''{0}'', to anchors store, replaced ''{1}''.",
                        new Object[]{entry.subjectDn, old==null?"<NULL>":old.getTrustedCert().getSubjectDN()});
            }
        } else {
            boolean removed = anchors.remove(tce.getSubjectDn())!=null;
            if ( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Removed certificate ''{0}'', from anchors store ({1}).",
                        new Object[]{entry.subjectDn, removed});
            }
        }
        trustAnchorsByDn = Collections.unmodifiableMap(anchors);

        try {
            Set<Certificate> nonAnchors = new HashSet<Certificate>(certStore.getCertificates(null));
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
        revocationCheckerFactory.invalidateTrustedCert(tce.getOid());
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
}
