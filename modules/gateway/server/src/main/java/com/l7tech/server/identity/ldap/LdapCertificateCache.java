package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.Lifecycle;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LDAP certificate caching and indexing support.
 */
class LdapCertificateCache implements Lifecycle {

    //- PUBLIC

    @Override
    public void start() {
        scheduleTask( rebuildTask, true, ldapRuntimeConfig.getRebuildTimerLength() );
        scheduleTask( cleanupTask, false, ldapRuntimeConfig.getCleanupTimerLength() );
    }

    @Override
    public void stop() {
        cancelTasks(rebuildTask, cleanupTask);
    }

    //- PACKAGE

    LdapCertificateCache( final LdapIdentityProviderConfig config,
                          final IdentityProviderConfigManager configManager,
                          final LdapRuntimeConfig ldapRuntimeConfig,
                          final LdapUtils.LdapTemplate ldapTemplate,
                          final Set<String> distinctCertAttributeNames,
                          final String ownerDescription,
                          final boolean enableIndex ) {
        this.config = config;
        this.configManager = configManager;
        this.ldapRuntimeConfig = ldapRuntimeConfig;
        this.ldapTemplate = ldapTemplate;
        this.distinctCertAttributeNames = distinctCertAttributeNames;
        this.ownerDescription = ownerDescription;

        if ( enableIndex ) {
            rebuildTask = new RebuildTask(this, ldapRuntimeConfig, ownerDescription);
        }

        cleanupTask = new CleanupTask(this, ownerDescription);
    }

    X509Certificate findCertBySki( final String ski ) {
        X509Certificate lookedupCert = null;

        // look for presence of cert in index
        CertIndex index = certIndexRef.get();
        CertCacheKey certCacheKey = index.getCertCacheKeyBySki( ski );

        if ( certCacheKey != null ) {
            final Pair<String, X509Certificate> result = getCertificateByKey(certCacheKey);
            lookedupCert = result == null ? null : result.right;
        }

        return lookedupCert;
    }

    X509Certificate findCertByIssuerAndSerial( final X500Principal issuerDN, final BigInteger certSerial ) {
        X509Certificate lookedupCert = null;

        // look for presence of cert in index
        CertIndex index = certIndexRef.get();
        CertCacheKey certCacheKey = index.getCertCacheKeyByIssuerAndSerial( issuerDN, certSerial );

        if ( certCacheKey != null ) {
            final Pair<String, X509Certificate> result = getCertificateByKey(certCacheKey);
            lookedupCert = result == null ? null : result.right;
        }

        return lookedupCert;
    }

    X509Certificate findCertByThumbprintSHA1( final String thumbprintSHA1 ) throws FindException {
        X509Certificate lookedupCert = null;

        // look for presence of cert in index
        CertIndex index = certIndexRef.get();
        CertCacheKey certCacheKey = index.getCertCacheKeyByThumbprintSHA1( thumbprintSHA1 );

        if ( certCacheKey != null ) {
            final Pair<String, X509Certificate> result = getCertificateByKey(certCacheKey);
            lookedupCert = result == null ? null : result.right;
        } 

        return lookedupCert;
    }

    X509Certificate findCertBySubjectDn( final X500Principal subjectDn ) {
        X509Certificate lookedupCert = null;

        // look for presence of cert in index
        CertIndex index = certIndexRef.get();
        CertCacheKey certCacheKey = index.getCertCacheKeyByCanonicalSubjectDn( CertUtils.getDN(subjectDn) );

        if ( certCacheKey != null ) {
            final Pair<String, X509Certificate> result = getCertificateByKey(certCacheKey);
            lookedupCert = result == null ? null : result.right;
        }

        return lookedupCert;
    }

    String findUserDnByCert( final X509Certificate cert ) throws FindException, CertificateEncodingException {
        String userDn = null;

        CertIndex index = certIndexRef.get();
        CertCacheKey certCacheKey = index.getCertCacheKeyByThumbprintSHA1(CertUtils.getThumbprintSHA1(cert));
        if (certCacheKey != null) {
            final Pair<String, X509Certificate> result = getCertificateByKey(certCacheKey);
            userDn = result == null ? null : result.left;
        }

        return userDn;
    }

    void clearIndex( final X500Principal issuer, final BigInteger serial, final String ski ) {
        final CertIndex index = certIndexRef.get();
        index.removeIndex( issuer, serial, ski );
    }

    void cacheAndIndexCertificates( final String dn, final X509Certificate[] certificates ) throws CertificateException {
        final CertIndex index = certIndexRef.get();

        final Map<CertCacheKey, CertCacheEntry> newCertCacheEntries = new HashMap<CertCacheKey, CertCacheEntry>();
        for ( X509Certificate cert : certificates ) {
            CertCacheKey certCacheKey = index.addCertificateToIndexes( dn, cert );
            if ( certCacheKey != null ) {
                newCertCacheEntries.put( certCacheKey, new CertCacheEntry(dn, cert) );
            }
        }

        for ( CertCacheKey certCacheKey : newCertCacheEntries.keySet() ) {
            logger.fine("Caching cert for " + certCacheKey);
        }

        cacheLock.writeLock().lock();
        try {
            certCache.putAll(newCertCacheEntries);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(LdapCertificateCache.class.getName());

    /**
     * Certificate index by issuer/serial and SKI
     */
    private final AtomicReference<CertIndex> certIndexRef = new AtomicReference<CertIndex>(new CertIndex());

    /**
     * This is a map to cache user certificates by the LDAP entry DN
     * Key: a string, lowercased, the DN of the LDAP entry to which the cert belong. This DN comes from the index certIndex
     * Value: the X509Certificate
     */
    private final HashMap<CertCacheKey, CertCacheEntry> certCache = new HashMap<CertCacheKey, CertCacheEntry>();

    /**
     * a lock for accessing the index above
     */
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();


    private final IdentityProviderConfigManager configManager;
    private final LdapIdentityProviderConfig config;
    private final LdapRuntimeConfig ldapRuntimeConfig;
    private final LdapUtils.LdapTemplate ldapTemplate;
    private final Set<String> distinctCertAttributeNames;
    private final ManagedTimer timer = new ManagedTimer("LDAP Certificate Cache Timer");
    private final String ownerDescription;
    private ManagedTimerTask rebuildTask;
    private ManagedTimerTask cleanupTask;

    private boolean isStale() {
        if ( configManager == null ) {
            logger.warning("Config Manager is null.");
        } else {
            try {
                IdentityProviderConfig config = configManager.findByPrimaryKey(this.config.getGoid());
                return config == null || config.getVersion()!=this.config.getVersion();
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error checking identity configuration for " + config, ExceptionUtils.getDebugException( e ) );
            }
        }
        return false; // don't assume it was removed if there are errors getting to it
    }

    private void scheduleTask( final ManagedTimerTask task, final boolean immediate,  final long period ) {
        if ( task != null ) {
            timer.schedule(task, immediate ? 0 : 5000, period);
        }
    }

    private void cancelTasks(ManagedTimerTask ... tasks) {
        for (ManagedTimerTask task : tasks) {
            if ( task != null ) {
                task.cancel();
            }
        }
    }

    private void rescheduleIndexRebuildTask() {
        cancelTasks(rebuildTask);
        rebuildTask = new RebuildTask(this, ldapRuntimeConfig, ownerDescription);
        scheduleTask(rebuildTask, false, ldapRuntimeConfig.getRebuildTimerLength());
    }

    private void cleanupCertCache() {
        ArrayList<CertCacheKey> todelete = new ArrayList<CertCacheKey>();
        long now = System.currentTimeMillis();
        cacheLock.readLock().lock();
        try {
            Set<CertCacheKey> keys = certCache.keySet();
            for (CertCacheKey key : keys) {
                CertCacheEntry cce = certCache.get(key);
                if ((now - cce.entryCreation) > ldapRuntimeConfig.getCachedCertEntryLife()) {
                    todelete.add(key);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        if (todelete.size() > 0) {
            cacheLock.writeLock().lock();
            try {
                for (CertCacheKey key : todelete) {
                    logger.fine("Removing certificate from cache '" + key + "'.");
                    certCache.remove(key);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }

    private Pair<String, X509Certificate> getCertificateByKey( final CertCacheKey certCacheKey ) {
        final AtomicReference<Pair<String, X509Certificate>> outputHolder = new AtomicReference<Pair<String, X509Certificate>>();

        // try to find cert in cert cache
        cacheLock.readLock().lock();
        try {
            CertCacheEntry cce = certCache.get(certCacheKey);
            if (cce != null) outputHolder.set(new Pair<String, X509Certificate>(cce.userDn, cce.cert));
        } finally {
            cacheLock.readLock().unlock();
        }

        if (outputHolder.get() != null) {
            logger.fine("Cert found in cache '"+certCacheKey+"'.");
        } else {
            // load the cert from ldap
            try {
                final UserMappingConfig[] mappings = config.getUserMappings();
                ldapTemplate.attributes( certCacheKey.getKey(), new LdapUtils.LdapListener(){
                    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
                    @Override
                    void attributes( final String dn, final Attributes attributes ) throws NamingException {
                        for ( UserMappingConfig mapping : mappings ) {
                            String userCertAttrName = mapping.getUserCertAttrName();
                            if (userCertAttrName == null || userCertAttrName.trim().isEmpty()) {
                                logger.fine("No user certificate attribute has been configured for user mapping " + mapping.getObjClass());
                            } else {
                                Object certificateObj = LdapUtils.extractOneAttributeValue(attributes, userCertAttrName.trim());
                                if (certificateObj instanceof byte[]) {
                                    logger.fine("Found a certificate in directory for " + certCacheKey.getKey());
                                    try {
                                        X509Certificate certificate = CertUtils.decodeCert((byte[])certificateObj);
                                        if ( certCacheKey.getValue().equals(CertUtils.getThumbprintSHA1(certificate)) ) {
                                            outputHolder.set(new Pair<String, X509Certificate>(dn, certificate));
                                            break;
                                        }
                                    } catch ( CertificateException ce ) {
                                        throw (NamingException) new NamingException(ExceptionUtils.getMessage(ce)).initCause( ce );
                                    }
                                }
                            }
                        }
                    }
                } );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error looking up certificate in directory " + certCacheKey, e);
            }

            if (outputHolder.get() == null) {
                logger.fine("Certificate is in the index but not in directory (" + certCacheKey + ")");
            } else {
                // add the cert to the cert cache and return
                logger.fine("Caching cert for " + certCacheKey);
                cacheLock.writeLock().lock();
                try {
                    certCache.put(certCacheKey, new CertCacheEntry(outputHolder.get().left, outputHolder.get().right));
                } finally {
                    cacheLock.writeLock().unlock();
                }
            }
        }

        return outputHolder.get();
    }

    private void doRebuildCertIndex() {
        try {
            doWithLDAPContextClassLoader( new Functions.NullaryVoidThrows<NamingException>() {
                @Override
                public void call() throws NamingException {
                    rebuildCertIndex();
                }
            } );
        } catch (NamingException e) {
            logger.log( Level.WARNING, "Error while recreating ldap user certificate index for LDAP Provider '" + config.getName() + "': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private void rebuildCertIndex() {
        logger.fine("Re-creating ldap user certificate index for " + config.getName());
        final CertIndex index = new CertIndex(
                new HashMap<Pair<String, String>, CertCacheKey>(),
                new HashMap<String, CertCacheKey>(),
                new HashMap<String, CertCacheKey>(),
                new HashMap<String, CertCacheKey>(),
                false);

        try {
            String filter = null;
            if ( config.getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX ) {
                LdapSearchFilter searchFilter = new LdapSearchFilter();
                boolean multiple = distinctCertAttributeNames.size() > 1;
                if (multiple) searchFilter.or();
                for ( String attr : distinctCertAttributeNames ) {
                    searchFilter.attrPresent( attr );
                }
                if (multiple) searchFilter.end();
                filter = searchFilter.isEmpty() ? null : searchFilter.buildFilter();
            } else if ( config.getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX_CUSTOM ) {
                filter = config.getUserCertificateIndexSearchFilter();
            }

            if ( filter != null ) {
                logger.fine( "LDAP user certificate search filter is '"+filter+"'." );

                SearchControls sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                sc.setReturningAttributes(distinctCertAttributeNames.toArray(new String[distinctCertAttributeNames.size()]));

                ldapTemplate.search( filter, 0, distinctCertAttributeNames, new LdapUtils.LdapListener(){
                    @Override
                    void attributes( final String dn, final Attributes attributes ) throws NamingException {
                        for ( String certAttributeName : distinctCertAttributeNames ) {
                            final Object certificateObj = LdapUtils.extractOneAttributeValue(attributes, certAttributeName);
                            if (certificateObj instanceof byte[]) {
                                try {
                                    final X509Certificate cert = CertUtils.decodeCert((byte[])certificateObj);
                                    index.addCertificateToIndexes( dn, cert );
                                } catch ( CertificateException e ) {
                                    logger.log( Level.WARNING, "Could not process certificate for '"+dn+"': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                }
                            }
                        }
                    }
                } );

                certIndexRef.set( index.immutable() );

                logger.fine("LDAP user certificate index rebuilt '"+index.describe()+"'.");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while recreating ldap user certificate index for LDAP Provider '" + config.getName() + "': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Invoke the given callback with the context classloader that loads SSL socket factories.
     */
    private void doWithLDAPContextClassLoader( final Functions.NullaryVoidThrows<NamingException> callback ) throws NamingException {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader() );
            callback.call();
        } finally {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }

    private static final class CertCacheKey extends Pair<String,String> {
        public CertCacheKey( final String dn, final String thumbprint) {
            super(dn,thumbprint);
        }
    }

    private static final class CertCacheEntry {
        private final String userDn;
        private final X509Certificate cert;
        private final long entryCreation;

        CertCacheEntry(String userDn, X509Certificate cert) {
            this.entryCreation = System.currentTimeMillis();
            this.userDn = userDn;
            this.cert = cert;
        }
    }

    private static class CertIndex {
        /**
         * This is a map which indexes the DN of LDAP entries for the entry's certificate serial and issuer dn.
         * Key: a string, lowercased, that is a Pair of IssuerDn and serial num.
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<Pair<String, String>, CertCacheKey> certIndexByIssuerSerial;

        /**
         * This is a map which indexes the DN of LDAP entries for the entry's certificate SKI.
         * Key: a string, BASE64 SKI.
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<String, CertCacheKey> certIndexBySki;

        /**
         * This is a map which indexes the DN of LDAP entries for the entry's ThumbprintSHA1.
         * Key: a string, BASE64 SKI.
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<String, CertCacheKey> certIndexByThumbprintSHA1;

        /**
         * This is a map which indexes the DN of LDAP entries for the entry's certificate Subject DN in RFC 2253 "CANONICAL" string format.
         * Key: a string, Subject DN in X500Principal.CANONICAL format
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<String, CertCacheKey> certIndexByCanonicalSubjectDn;

        private final boolean immutable;

        protected CertIndex() {
            this.certIndexByIssuerSerial = new ConcurrentHashMap<Pair<String, String>, CertCacheKey>();
            this.certIndexBySki = new ConcurrentHashMap<String, CertCacheKey>();
            this.certIndexByThumbprintSHA1 = new ConcurrentHashMap<String, CertCacheKey>();
            this.certIndexByCanonicalSubjectDn = new ConcurrentHashMap<String, CertCacheKey>();
            this.immutable = false;
        }

        protected CertIndex( final Map<Pair<String, String>, CertCacheKey> certIndexByIssuerSerial,
                             final Map<String, CertCacheKey> certIndexBySki,
                             final Map<String, CertCacheKey> certIndexByThumbprintSHA1,
                             final Map<String, CertCacheKey> certIndexByCanonicalSubjectDn,
                             final boolean immutable ) {
            this.certIndexByIssuerSerial = certIndexByIssuerSerial;
            this.certIndexBySki = certIndexBySki;
            this.certIndexByThumbprintSHA1 = certIndexByThumbprintSHA1;
            this.certIndexByCanonicalSubjectDn = certIndexByCanonicalSubjectDn;
            this.immutable = immutable;
        }

        protected CertCacheKey getCertCacheKeyByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
            Pair<String, String> key = makeIndexKey(issuer, serial);
            return certIndexByIssuerSerial.get(key);
        }

        protected CertCacheKey getCertCacheKeyBySki( final String ski ) {
            return certIndexBySki.get(ski);
        }

        protected CertCacheKey getCertCacheKeyByThumbprintSHA1( final String thumbprintSHA1 ) {
            return certIndexByThumbprintSHA1.get(thumbprintSHA1);
        }

        protected CertCacheKey getCertCacheKeyByCanonicalSubjectDn( final String canonicalSubjectDn ) {
            return certIndexByCanonicalSubjectDn.get(canonicalSubjectDn);
        }

        protected CertCacheKey addCertificateToIndexes( final String dn,
                                                        final X509Certificate cert ) throws CertificateException {
            CertCacheKey certCacheKey = null;

            if ( !immutable ) {
                final String thumbprintSHA1 = CertUtils.getThumbprintSHA1(cert);
                certCacheKey = new CertCacheKey( dn, thumbprintSHA1 );

                if (cert.getSerialNumber() != null && cert.getIssuerX500Principal() != null) {
                    X500Principal issuer = cert.getIssuerX500Principal();
                    BigInteger serial = cert.getSerialNumber();

                    Pair<String, String> key = makeIndexKey(issuer, serial);
                    logger.finer("Indexing certificate by issuer/serial " + key + " for " + dn);
                    certIndexByIssuerSerial.put(key, certCacheKey);
                }
                String ski = CertUtils.getSki( cert );
                if ( ski != null ) {
                    logger.finer("Indexing certificate by ski " + ski + " for " + dn);
                    certIndexBySki.put(ski, certCacheKey);
                }

                logger.finer("Indexing certificate by thumbprintSHA1 " + thumbprintSHA1 + " for " + dn);
                certIndexByThumbprintSHA1.put(thumbprintSHA1, certCacheKey);

                String canonicalSubjectDn = CertUtils.getSubjectDN(cert);
                logger.finer("Indexing certificate by canonicalSubjectDn " + canonicalSubjectDn + " for " + dn);
                certIndexByCanonicalSubjectDn.put(canonicalSubjectDn, certCacheKey);
            }

            return certCacheKey;
        }

        protected void removeIndex( final X500Principal issuer, final BigInteger serial, final String ski ) {
            if ( !immutable ) {
                if ( issuer != null && serial != null ) {
                    certIndexByIssuerSerial.remove(makeIndexKey(issuer, serial));
                }

                if ( ski != null ) {
                    certIndexBySki.remove(ski);
                }
            }
        }

        protected CertIndex immutable() {
            return immutable ? this : new CertIndex(
                    Collections.unmodifiableMap(certIndexByIssuerSerial),
                    Collections.unmodifiableMap(certIndexBySki),
                    Collections.unmodifiableMap(certIndexByThumbprintSHA1),
                    Collections.unmodifiableMap(certIndexByCanonicalSubjectDn),
                    true);
        }

        protected String describe() {
            StringBuilder description = new StringBuilder();
            description.append("Issuer/Serial index size: ");
            description.append(certIndexByIssuerSerial.size());
            description.append(", SKI index size: ");
            description.append(certIndexBySki.size());
            description.append(", ThumbprintSHA1 index size: ");
            description.append(certIndexByThumbprintSHA1.size());
            return description.toString();
        }

        private Pair<String,String> makeIndexKey(X500Principal issuerDN, BigInteger certSerial) {
            return new Pair<String, String>(issuerDN.getName(X500Principal.RFC2253), certSerial.toString());
        }
    }

    private static class CleanupTask extends ManagedTimerTask {
        private final LdapCertificateCache ldapCertificateCache;
        private final String ownerDescription;

        CleanupTask( final LdapCertificateCache ldapCertificateCache,
                     final String ownerDecription ) {
            this.ldapCertificateCache = ldapCertificateCache;
            this.ownerDescription = ownerDecription;
        }

        @Override
        protected void doRun() {
            //when the referant Identity provider goes away, this timer should stop
            if ( ldapCertificateCache.isStale() ) {
                cancel();
            } else {
                ldapCertificateCache.cleanupCertCache();
            }
        }

        @Override
        public boolean cancel() {
            logger.info("Cancelling cert cache cleanup task for '" + ownerDescription + "'.");
            return super.cancel();
        }
    }

    /**
     * Task to rebulid the certificate index (if required)
     */
    private static class RebuildTask extends ManagedTimerTask {
        private final LdapCertificateCache ldapCertificateCache;
        private final LdapRuntimeConfig ldapRuntimeConfig;
        private final String ownerDescription;
        private long currentIndexInterval;

        RebuildTask( final LdapCertificateCache ldapCertificateCache,
                     final LdapRuntimeConfig ldapRuntimeConfig,
                     final String ownerDescription ) {
            this.ldapCertificateCache = ldapCertificateCache;
            this.ldapRuntimeConfig = ldapRuntimeConfig;
            this.ownerDescription = ownerDescription;
            this.currentIndexInterval = ldapRuntimeConfig.getRebuildTimerLength();
        }

        @Override
        protected void doRun() {
            // When the referant Identity provider goes away, this timer should stop
            if ( ldapCertificateCache.isStale() ) {
                cancel();
            } else {
                long newIndexInterval = ldapRuntimeConfig.getRebuildTimerLength();

                if (currentIndexInterval != newIndexInterval) {
                    logger.info( MessageFormat.format(
                            "Certificate index rebuild interval has changed (old value = {0}, new value = {1}). Rescheduling this task with new interval.",
                            currentIndexInterval,
                            newIndexInterval));
                    currentIndexInterval = newIndexInterval;
                    ldapCertificateCache.rescheduleIndexRebuildTask();
                } else {
                    ldapCertificateCache.doRebuildCertIndex();
                }
            }
        }

        @Override
        public boolean cancel() {
            logger.info("Cancelling cert index task for '" + ownerDescription + "'.");
            return super.cancel();
        }
    }
}
