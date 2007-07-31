/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.common.security.CertificateValidationResult;
import static com.l7tech.common.security.CertificateValidationResult.CANT_BUILD_PATH;
import static com.l7tech.common.security.CertificateValidationResult.OK;
import com.l7tech.common.security.CertificateValidationType;
import static com.l7tech.common.security.CertificateValidationType.CERTIFICATE_ONLY;
import com.l7tech.common.security.RevocationCheckPolicy;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.security.*;
import java.security.cert.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class CertValidationProcessorImpl implements CertValidationProcessor, ApplicationListener {
    private static final Logger logger = Logger.getLogger(CertValidationProcessorImpl.class.getName());

    private final X509Certificate caCert;
    private final TrustedCertManager trustedCertManager;
    private final RevocationCheckPolicyManager revocationCheckPolicyManager;
    private final RevocationCheckerFactory revocationCheckerFactory;
    private final ServerConfig serverConfig;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, CertValidationCacheEntry> trustedCertsByDn;
    private final Map<Long, CertValidationCacheEntry> trustedCertsByOid;
    private final Map<Long, RevocationCheckPolicy> revocationPoliciesByOid;

    private Map<String, TrustAnchor> trustAnchorsByDn;
    private CertStore certStore;

    private RevocationCheckPolicy currentDefaultRevocationPolicy;
    private final RevocationCheckPolicy permissiveRcp;

    public CertValidationProcessorImpl(final TrustedCertManager trustedCertManager,
                                       final RevocationCheckPolicyManager revocationCheckPolicyManager,
                                       final CrlCache crlCache,
                                       final X509Certificate caCert,
                                       final ServerConfig serverConfig)
    {
        if (trustedCertManager == null || revocationCheckPolicyManager == null || crlCache == null || caCert == null || serverConfig == null) throw new NullPointerException("A required component is missing");
        this.trustedCertManager = trustedCertManager;
        this.revocationCheckPolicyManager = revocationCheckPolicyManager;
        this.revocationCheckerFactory = new RevocationCheckerFactory(this, crlCache);
        this.caCert = caCert;
        this.permissiveRcp = new RevocationCheckPolicy();
        this.permissiveRcp.setDefaultSuccess(true);
        this.serverConfig = serverConfig;

        Collection<RevocationCheckPolicy> rcps;
        try {
            rcps = revocationCheckPolicyManager.findAll();
        } catch (FindException e) {
            throw new RuntimeException("Unable to find RevocationCheckPolicies", e);
        }

        // Set up initial RevocationCheckPolicy caches
        Map<Long, RevocationCheckPolicy> myRcpsByOid = new HashMap<Long, RevocationCheckPolicy>();
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
        this.revocationPoliciesByOid = myRcpsByOid;

        // Setup initial TrustedCert caches
        Collection<TrustedCert> tces;
        try {
            tces = trustedCertManager.findAll();
        } catch (FindException e) {
            throw new RuntimeException("Unable to find Trusted Certs", e);
        }

        Map<String, CertValidationCacheEntry> myDnCache = new HashMap<String, CertValidationCacheEntry>();
        Map<Long, CertValidationCacheEntry> myOidCache = new HashMap<Long, CertValidationCacheEntry>();

        for (TrustedCert tce : tces) {
            try {
                final CertValidationCacheEntry entry = new CertValidationCacheEntry(tce);
                myDnCache.put(tce.getSubjectDn(), entry);
                myOidCache.put(tce.getOid(), entry);
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "Couldn't load TrustedCert #{0} ({1}): " + ExceptionUtils.getMessage(e), new Object[] { tce.getOid(), tce.getSubjectDn() });
            }
        }

        this.trustedCertsByDn = myDnCache;
        this.trustedCertsByOid = myOidCache;
        initializeCertStoreAndTrustAnchors(tces);
    }

    /**
     * TODO can we pass a chain here in the case of outbound connections to make our job easier?
     */
    public CertificateValidationResult check(final X509Certificate endEntityCertificate,
                                             final CertificateValidationType requestedValType,
                                             final Facility facility,
                                             final Auditor auditor)
            throws CertificateException, SignatureException
    {
        if (endEntityCertificate == null || auditor == null) throw new NullPointerException("a required parameter is missing");
        if (requestedValType == null && facility == null) throw new NullPointerException("Either requestedValType or facility must be provided");

        final CertificateValidationType valType;
        if (requestedValType == null) {
            final String syspropname = "pkixValidation." + facility.name().toLowerCase();
            String stype = serverConfig.getPropertyCached(syspropname);
            if (stype == null) {
                logger.warning(syspropname + " system property unavailable, using default " + CERTIFICATE_ONLY);
                valType = CERTIFICATE_ONLY;
            } else if ("validate".equalsIgnoreCase(stype)) {
                valType = CertificateValidationType.CERTIFICATE_ONLY;
            } else if ("validatepath".equalsIgnoreCase(stype)) {
                valType = CertificateValidationType.PATH_VALIDATION;
            } else if ("revocation".equalsIgnoreCase(stype)) {
                valType = CertificateValidationType.REVOCATION;
            } else {
                throw new IllegalArgumentException(syspropname + " system property contained an invalid cert validation type " + stype);
            }
        } else {
            valType = requestedValType;
        }

        String subjectDn = endEntityCertificate.getSubjectDN().getName();
        String issuerDn = endEntityCertificate.getIssuerDN().getName();

        if (valType == CERTIFICATE_ONLY)
            return checkCertificateOnly(endEntityCertificate, subjectDn, issuerDn, auditor);

        // Time to build a path
        final PKIXBuilderParameters pbp = makePKIXBuilderParameters(endEntityCertificate,
                                                                    valType == CertificateValidationType.REVOCATION,
                                                                    auditor);
        try {
            CertPathBuilderResult builderResult = CertPathBuilder.getInstance("PKIX").build(pbp);
            builderResult.getCertPath();
            return OK;
        } catch (CertPathBuilderException e) {
            auditor.logAndAudit(SystemMessages.CERTVAL_CANT_BUILD_PATH, new String[] { subjectDn, ExceptionUtils.getMessage(e) }, e);
            return CertificateValidationResult.CANT_BUILD_PATH;
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateException("Unable to build Cert Path", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    private CertificateValidationResult checkCertificateOnly(X509Certificate endEntityCertificate,
                                                             String subjectDn,
                                                             String issuerDn,
                                                             Auditor auditor)
            throws SignatureException, CertificateException
    {
        final CertValidationCacheEntry issuerCacheEntry;
        lock.readLock().lock();
        try {
            issuerCacheEntry = trustedCertsByDn.get(issuerDn);
            if (issuerCacheEntry == null) {
                auditor.logAndAudit(SystemMessages.CERTVAL_CANT_FIND_ISSUER, issuerDn, subjectDn);
                return CANT_BUILD_PATH;
            }
        } finally {
            lock.readLock().unlock();
        }

        X509Certificate issuerCert = issuerCacheEntry.cert;
        try {
            // Note, this is not path validation per se; we always verify the signature on the end entity cert, so its
            // CA has to be known.
            CertUtils.cachedVerify(endEntityCertificate, issuerCert.getPublicKey());
            // We do these here because they would normally be done as part of path validation, which we're skipping
            endEntityCertificate.checkValidity();
            issuerCacheEntry.cert.checkValidity();
            auditor.logAndAudit(SystemMessages.CERTVAL_CHECKED);
            return OK;
        } catch (InvalidKeyException e1) {
            throw new CertificateException(MessageFormat.format("Couldn't verify signature of issuer {0} on end entity {1}", issuerDn, subjectDn), e1);
        } catch (NoSuchAlgorithmException e11) {
            throw new RuntimeException(e11); // Can't happen
        } catch (NoSuchProviderException e12) {
            throw new RuntimeException(e12); // Can't happen
        }
    }

    public TrustedCert getTrustedCertByOid(long oid) {
        lock.readLock().lock();
        try {
            return trustedCertsByOid.get(oid).tce;
        } finally {
            lock.readLock().unlock();
        }
    }

    public TrustedCert getTrustedCertBySubjectDn(String subjectDn) {
        lock.readLock().lock();
        try {
            return trustedCertsByDn.get(subjectDn).tce;
        } finally {
            lock.readLock().unlock();
        }
    }

    public TrustedCert getTrustedCertEntry(X509Certificate certificate) {
        lock.readLock().lock();
        try {
            final String subjectDn = certificate.getSubjectDN().getName();
            final CertValidationCacheEntry entry = trustedCertsByDn.get(subjectDn);
            if (entry == null) return null;
            if (CertUtils.certsAreEqual(certificate, entry.cert)) return entry.tce;
            throw new IllegalArgumentException("Cached TrustedCert with DN " + subjectDn + " is different from presented certificate");
        } finally {
            lock.readLock().unlock();
        }
    }

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

    public RevocationCheckPolicy getDefaultPolicy() {
        lock.readLock().lock();
        try {
            return currentDefaultRevocationPolicy;
        } finally {
            lock.readLock().unlock();
        }
    }

    public RevocationCheckPolicy getPermissivePolicy() {
        return permissiveRcp;
    }

    private PKIXBuilderParameters makePKIXBuilderParameters(X509Certificate endEntityCertificate, boolean checkRevocation, Auditor auditor) {
        X509CertSelector sel;
        lock.readLock().lock();
        try {
            sel = new X509CertSelector();
            sel.setCertificate(endEntityCertificate);
            try {
                Set<TrustAnchor> tempAnchors = new HashSet<TrustAnchor>();
                tempAnchors.addAll(trustAnchorsByDn.values());
                PKIXBuilderParameters pbp = new PKIXBuilderParameters(tempAnchors, sel);
                if (checkRevocation)
                    pbp.addCertPathChecker(new RevocationCheckingPKIXCertPathChecker(revocationCheckerFactory, pbp, auditor));
                pbp.addCertStore(certStore);
                pbp.setRevocationEnabled(false); // turn off built in revocation checking
                return pbp;
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e); // Can't happen
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) applicationEvent;
            if (TrustedCert.class.isAssignableFrom(eie.getEntityClass())) {
                try {
                    lock.writeLock().lock();
                    nextOid: for (int i = 0; i < eie.getEntityIds().length; i++) {
                        long oid = eie.getEntityIds()[i];
                        char op = eie.getEntityOperations()[i];
                        switch(op) {
                            case EntityInvalidationEvent.CREATE:
                            case EntityInvalidationEvent.UPDATE:
                                try {
                                    addTrustedCertToCaches(trustedCertManager.findByPrimaryKey(oid));
                                    break;
                                } catch (Exception e) {
                                    logger.log(Level.WARNING, "Couldn't load recently created or updated TrustedCert #" + oid, e);
                                    continue nextOid;
                                }
                            case EntityInvalidationEvent.DELETE:
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
                try {
                    lock.writeLock().lock();
                    nextOid: for (int i = 0; i < eie.getEntityIds().length; i++) {
                        long oid = eie.getEntityIds()[i];
                        char op = eie.getEntityOperations()[i];
                        switch(op) {
                            case EntityInvalidationEvent.CREATE:
                            case EntityInvalidationEvent.UPDATE:
                                try {
                                    addRCPToCaches(revocationCheckPolicyManager.findByPrimaryKey(oid));
                                    break;
                                } catch (Exception e) {
                                    logger.log(Level.WARNING, "Couldn't load recently created or updated RevocationCheckPolicy #" + oid, e);
                                    continue nextOid;
                                }
                            case EntityInvalidationEvent.DELETE:
                                removeRCPFromCaches(oid);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected invalidation operation: '" + op + "'");
                        }
                    }
                    if (currentDefaultRevocationPolicy == null) throw new IllegalStateException("Default revocation policy deleted and not replaced");
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }

    /** Caller must hold write lock (or be the constructor) */
    private void initializeCertStoreAndTrustAnchors(Collection<TrustedCert> tces) {
        Map<String, TrustAnchor> anchors = new HashMap<String, TrustAnchor>();
        Set<X509Certificate> nonAnchors = new HashSet<X509Certificate>();
        anchors.put(caCert.getSubjectDN().getName(), new TrustAnchor(caCert, null));
        for (TrustedCert tce : tces) {
            try {
                if (tce.isTrustAnchor()) {
                    anchors.put(tce.getSubjectDn(), new TrustAnchor(tce.getCertificate(), null));
                } else {
                    nonAnchors.add(tce.getCertificate());
                }
            } catch (CertificateException e) {
                throw new RuntimeException(e); // Can't happen, someone else has already parsed it
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
        revocationCheckerFactory.invalidateRevocationCheckPolicy(policy.getOid());
        if (policy.isDefaultPolicy()) currentDefaultRevocationPolicy = policy; // TODO if the bit has been cleared this won't catch it
        revocationPoliciesByOid.put(policy.getOid(), policy);
    }

    /** Caller must hold write lock */
    private void removeTrustedCertFromCaches(long oid) {
        revocationCheckerFactory.invalidateTrustedCert(oid);
        CertValidationCacheEntry entry = trustedCertsByOid.remove(oid);
        if (entry == null) return;
        trustedCertsByDn.remove(entry.subjectDn);
        try {
            initializeCertStoreAndTrustAnchors(trustedCertManager.findAll());
        } catch (FindException e) {
            throw new RuntimeException(e); // TODO
        }
    }

    /** Caller must hold write lock */
    private void addTrustedCertToCaches(TrustedCert tce) throws CertificateException {
        revocationCheckerFactory.invalidateTrustedCert(tce.getOid());
        CertValidationCacheEntry entry = new CertValidationCacheEntry(tce);
        trustedCertsByDn.put(tce.getSubjectDn(), entry);
        trustedCertsByOid.put(tce.getOid(), entry);
        try {
            initializeCertStoreAndTrustAnchors(trustedCertManager.findAll());
        } catch (FindException e) {
            throw new RuntimeException(e); // TODO
        }
    }
}
