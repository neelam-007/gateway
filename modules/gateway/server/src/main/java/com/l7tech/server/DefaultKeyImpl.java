package com.l7tech.server;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.KeyManager;
import javax.security.auth.x500.X500Principal;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows about the location and passwords for the SSL and CA keystores, server certificates, etc.
 */
public class DefaultKeyImpl implements DefaultKey, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(DefaultKeyImpl.class.getName());

    private static final Pattern KEYSTORE_ID_AND_ALIAS_PATTERN = Pattern.compile("^([0-9a-fA-F]{32}|-?\\d{1,20}):(.*)$");
    private static final SsgKeyEntry NULL_ENTRY = new SsgKeyEntry(new Goid(0,Long.MIN_VALUE), null, null, null);

    private final ServerConfig serverConfig;
    private final ClusterPropertyManager clusterPropertyManager;
    private final SsgKeyStoreManager keyStoreManager;
    private final PlatformTransactionManager transactionManager;
    private final AtomicReference<SsgKeyEntry> cachedSslInfo = new AtomicReference<SsgKeyEntry>();
    private final AtomicReference<SsgKeyEntry> cachedCaInfo = new AtomicReference<SsgKeyEntry>();
    private final AtomicReference<SsgKeyEntry> cachedAuditViewerInfo = new AtomicReference<SsgKeyEntry>();
    private final AtomicReference<SsgKeyEntry> cachedAuditSigningInfo = new AtomicReference<SsgKeyEntry>();
    private static final String SC_PROP_SSL_KEY = ServerConfigParams.PARAM_KEYSTORE_DEFAULT_SSL_KEY;

    @Inject
    @Named( "masterPasswordManager" )
    private MasterPasswordManager masterPasswordManager;

    @Inject
    @Named( "trustedCertManager" )
    private TrustedCertManager trustedCertManager;

    public DefaultKeyImpl( final ServerConfig serverConfig,
                           final ClusterPropertyManager clusterPropertyManager,
                           final SsgKeyStoreManager keyStoreManager,
                           final PlatformTransactionManager transactionManager) {
        this.serverConfig = serverConfig;
        this.clusterPropertyManager = clusterPropertyManager;
        this.keyStoreManager = keyStoreManager;
        this.transactionManager = transactionManager;
    }

    void setTrustedCertManager( TrustedCertManager trustedCertManager ) {
        this.trustedCertManager = trustedCertManager;
    }

    private SsgKeyEntry getOrCreateSslInfo() throws IOException {
        try {
            return getCachedEntry(cachedSslInfo, SC_PROP_SSL_KEY, true);
        } catch (ObjectNotFoundException e) {
            // Get the lock, then try again
            synchronized (this) {
                try {
                    // If someone else created it in the meantime, use theirs
                    return getCachedEntry(cachedSslInfo, SC_PROP_SSL_KEY, true);
                } catch (ObjectNotFoundException e1) {
                    // Create a new one
                    TransactionTemplate template = new TransactionTemplate(transactionManager);
                    template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                    final IOException[] holder = new IOException[1];

                    final boolean wasSystem = AuditContextUtils.isSystem();
                    AuditContextUtils.setSystem(true);
                    SsgKeyEntry entry;
                    try {
                        entry = (SsgKeyEntry) template.execute(new TransactionCallback(){
                            public Object doInTransaction(TransactionStatus transactionStatus) {
                                try {
                                    return generateSelfSignedSslCert();
                                } catch (IOException ioe) {
                                    holder[0] = ioe;
                                    transactionStatus.setRollbackOnly();
                                    return null;
                                }
                            }
                        });
                    } finally {
                        AuditContextUtils.setSystem(wasSystem);
                    }

                    if ( holder[0] != null ) {
                        throw holder[0];
                    }

                    return entry;
                }
            }
        }
    }

    public SsgKeyEntry getSslInfo() throws IOException {
        // TODO grody work-around to help diagnostics -- we will redundantly log + throw failures for now, until we figure out how to stop Spring from
        // too-readily overwriting the original failure cause with subsequent incidental stuff (like "rollback-only" errors)
        try {
            return getOrCreateSslInfo();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to prepare default SSL key: " + ExceptionUtils.getMessage(e), e);
            throw e;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Unable to prepare default SSL key: " + ExceptionUtils.getMessage(e), e);
            throw e;
        }
    }

    // There's currently no default SSL key designated.  Generate a new self-signed one to get us started.
    private SsgKeyEntry generateSelfSignedSslCert() throws IOException {
        logger.log(Level.INFO, "No default SSL private key is designated, or the designated SSL private key is not found; " +
                "creating new self-signed SSL private key and certificate chain");
        final boolean wasSystem = AuditContextUtils.isSystem();
        AuditContextUtils.setSystem(true);
        try {
            String alias = findUnusedAlias(SC_PROP_SSL_KEY, "SSL");
            SsgKeyStore sks = findFirstMutableKeystore();
            findOrGenerateKeyPair(sks, alias);
            return configureAsDefaultSslCert(sks, alias);
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    private SsgKeyEntry configureAsDefaultSslCert(SsgKeyStore sks, String alias) throws IOException {
        try {
            SsgKeyEntry entry = sks.getCertificateChain(alias);
            String name = serverConfig.getClusterPropertyName(SC_PROP_SSL_KEY);
            if (name == null)
                throw new IOException("Unable to configure default SSL key: no cluster property defined for ServerConfig property " + SC_PROP_SSL_KEY);
            String value = entry.getKeystoreId() + ":" + entry.getAlias();
            clusterPropertyManager.putProperty(name, value);
            invalidateCachedCerts();
            return entry;
        } catch (ObjectNotFoundException e) {
            throw new IOException("Unable to find default SSL key: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (UpdateException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (SaveException e) {
            throw new IOException("Unable to configure default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void findOrGenerateKeyPair( final SsgKeyStore sks, final String alias ) throws IOException {
        // Check if a SSG_SSL_KEY env var is present; if so, it is a Base-64 encoded
        // PKCS#12 key store containing a single key entry,
        // with the passphrase provided in the SSG_SSL_KEY_PASS env var.

        String pkcs12b64 = getenv( "SSG_SSL_KEY" );
        if ( Boolean.getBoolean( "com.l7tech.bootstrap.env.sslkey.enable" ) &&
                pkcs12b64 != null &&
                pkcs12b64.trim().length() > 0 )
        {
            logger.info( "Trying to create default SSL key from the SSG_SSL_KEY environment variable" );

            String kspass = getenv( "SSG_SSL_KEY_PASS" );
            if ( kspass == null ) {
                logger.info( "SSG_SSL_KEY_PASS environment variable not set -- guessing default keystore passphrase for SSG_SSL_KEY" ) ;
                kspass = "password";
            }

            if ( masterPasswordManager != null ) {
                kspass = new String( masterPasswordManager.decryptPasswordIfEncrypted( kspass ) );
            }

            try {
                byte[] pkcs12bytes = HexUtils.decodeBase64( pkcs12b64 );
                KeyStore ks = KeyStore.getInstance( "PKCS12" );
                ks.load( new ByteArrayInputStream( pkcs12bytes ), kspass.toCharArray() );
                Enumeration<String> aliases = ks.aliases();

                PrivateKey privateKey = null;
                X509Certificate[] chain = null;

                while ( aliases.hasMoreElements() ) {
                    String ksalias = aliases.nextElement();
                    if ( ks.isKeyEntry( ksalias ) ) {
                        privateKey = (PrivateKey) ks.getKey( ksalias, kspass.toCharArray() );
                        Certificate[] certs = ks.getCertificateChain( ksalias );
                        chain = new X509Certificate[certs.length];
                        for ( int i = 0; i < certs.length; i++ ) {
                            chain[i] = (X509Certificate) certs[i];
                        }
                        break;
                    }
                }

                if ( null == privateKey )
                    throw new IOException( "No private key entry found in SSG_SSL_KEY keystore" );

                SsgKeyEntry entry = new SsgKeyEntry( sks.getGoid(), alias, chain, privateKey );
                Future<Boolean> job = sks.storePrivateKeyEntry( true, null, entry, false );
                job.get();
                logger.info( "Default SSL key successfully created from SSG_SSL_KEY environment variable" );
                maybeAddTrustStoreEntryForKey( chain[0] );
                return;

            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Failed to create default SSL key from SSG_SSL_KEY environment variable: " +
                            ExceptionUtils.getMessage( e ), e );
                /* FALLTHROUGH and generate new one */
            }
        }

        // Need to generate new one
        generateKeyPair( sks, alias );
    }

    protected String getenv( String env ) {
        return System.getenv( env );
    }

    private void generateKeyPair(SsgKeyStore sks, String alias) throws IOException {
        X500Principal dn = findDefaultSslKeyDn();
        try {
            Future<X509Certificate> job = sks.generateKeyPair(null, alias, new KeyGenParams(), new CertGenParams(dn, 365 * 10, false, null), null);
            X509Certificate cert = job.get();
            maybeAddTrustStoreEntryForKey( cert );
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (ExecutionException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            throw new IOException("Unable to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * After a default SSL key has been generated or imported for the first time, check if we need to add a
     * trust store entry that trusts our own SSL cert.
     *
     * @param cert the cert of the default SSL key that was just generated or imported.
     */
    private void maybeAddTrustStoreEntryForKey( X509Certificate cert ) {
        String params = SyspropUtil.getString( "com.l7tech.bootstrap.autoTrustSslKey", null );
        if ( params == null || params.trim().length() < 1 )
            return;

        // Example usage:
        // -Dcom.l7tech.bootstrap.autoTrustSslKey=trustAnchor,verifyHostname,TrustedFor.SSL,TrustedFor.SAML_ISSUER

        logger.fine( "autoTrustSslKey: checking cert store" );

        // Check for existing trust store entry
        if ( trustedCertManager == null ) {
            throw new IllegalStateException( "Unable to set up SSL key autotrust: no trustedCertManager" );
        }

        try {
            String thumbprint = CertUtils.getThumbprintSHA1( cert );
            List<TrustedCert> certs = trustedCertManager.findByThumbprint( thumbprint );
            if ( certs != null && !certs.isEmpty() ) {
                logger.fine( "autoTrustSslKey: SSL key is already trusted" );
                return;
            }

            logger.info( "Setting up automatic trust of SSL key" );
            TrustedCert tc = new TrustedCert();
            tc.setCertificate( cert );
            tc.setName( "AutoTrustSslKey" );
            tc.setTrustAnchor( params.contains( "trustAnchor" ) );
            tc.setVerifyHostname( params.contains( "verifyHostname") );
            tc.setRevocationCheckPolicyType( TrustedCert.PolicyUsageType.USE_DEFAULT );
            for ( TrustedCert.TrustedFor trustedFor : TrustedCert.TrustedFor.values() ) {
                if ( params.contains( "TrustedFor." + trustedFor.name() ) ) {
                    tc.setTrustedFor( trustedFor, true );
                }
            }

            Goid goid = trustedCertManager.save( tc );
            logger.info( "autoTrustSslKey: new trusted certificate saved with goid=" + goid );

        } catch ( CertificateEncodingException e ) {
            throw new RuntimeException( "Unable to set up SSL key autotrust: cert invalid: " + ExceptionUtils.getMessage( e ), e );
        } catch ( FindException e ) {
            throw new RuntimeException( "Unable to set up SSL key autotrust: unable to look up trusted cert: " + ExceptionUtils.getMessage( e ), e );
        } catch ( SaveException e ) {
            throw new RuntimeException( "Unable to set up SSL key autotrust: unable to save new trusted cert: " + ExceptionUtils.getMessage( e ) , e );
        }
    }

    private X500Principal findDefaultSslKeyDn() {
        String hostname = serverConfig.getProperty( "clusterHost" );
        if ( hostname == null || hostname.trim().length() == 0) {
            hostname = serverConfig.getHostname();
        }
        return new X500Principal("cn=" + hostname);
    }

    private SsgKeyStore findFirstMutableKeystore() throws IOException {
        SsgKeyStore sks = null;
        try {
            List<SsgKeyFinder> got = keyStoreManager.findAll();
            for (SsgKeyFinder ssgKeyFinder : got) {
                if (ssgKeyFinder.isMutable()) {
                    sks = ssgKeyFinder.getKeyStore();
                    break;
                }
            }
            if (sks == null)
                throw new IOException("No mutable keystores found in which to create initial default SSL key");
        } catch (FindException e) {
            throw new IOException("Unable to find keystore in which to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to find keystore in which to create initial default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
        return sks;
    }

    private String findUnusedAlias(String propertyName, String defaultBaseAlias) throws IOException {
        Pair<Goid, String> keyaddr = getKeyStoreGoidAndAlias(propertyName);
        String baseAlias = keyaddr == null ? null : keyaddr.right;
        if (baseAlias == null || baseAlias.trim().length() < 1)
            baseAlias = defaultBaseAlias;
        String alias = baseAlias;
        int count = 1;
        while (aliasAlreadyUsed(alias)) {
            alias = baseAlias + (count++);
        }
        return alias;
    }

    private boolean aliasAlreadyUsed(String alias) throws IOException {
        try {
            keyStoreManager.lookupKeyByKeyAlias(alias, PersistentEntity.DEFAULT_GOID);
            return true;
        } catch (ObjectNotFoundException e) {
            return false;
        } catch (FindException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new IOException("Unable to check if alias \"" + alias + "\" is already used: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public SsgKeyEntry getCaInfo() {
        try {
            return getCachedEntry(cachedCaInfo, ServerConfigParams.PARAM_KEYSTORE_DEFAULT_CA_KEY, false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to look up default CA key: " + ExceptionUtils.getMessage(e), e);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException("Unable to look up default CA key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public SsgKeyEntry getAuditSigningInfo() {
        try {
            return getCachedEntry(cachedAuditSigningInfo, ServerConfigParams.PARAM_KEYSTORE_AUDIT_SIGNING_KEY, false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to look up audit signing key: " + ExceptionUtils.getMessage(e), e);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException("Unable to look up audit signing key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public SsgKeyEntry getAuditViewerInfo() {
        try {
            return getCachedEntry(cachedAuditViewerInfo, ServerConfigParams.PARAM_KEYSTORE_AUDIT_VIEWER_KEY, false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to look up audit viewer key: " + ExceptionUtils.getMessage(e), e);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException("Unable to look up audit viewer key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public Pair<Goid, String> getAuditViewerAlias() {
        SsgKeyEntry info = cachedAuditViewerInfo.get();
        if (info != null)
            return new Pair<Goid, String>(info.getKeystoreId(), info.getAlias());

        try {
            return getKeyStoreGoidAndAlias(ServerConfigParams.PARAM_KEYSTORE_AUDIT_VIEWER_KEY);
        } catch (IOException e) {
            throw new RuntimeException("Unable to look up audit viewer key alias: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public KeyManager[] getSslKeyManagers() {
        try {
            SsgKeyEntry info = getSslInfo();
            X509Certificate[] chain = info.getCertificateChain();
            PrivateKey key = info.getPrivate();
            String alias = info.getAlias();
            return new KeyManager[] { new SingleCertX509KeyManager(chain, key, alias) };
        } catch (IOException e) {
            throw new RuntimeException("No default SSL key available: " + ExceptionUtils.getMessage(e), e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException("Unable to access default SSL key: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, Goid preferredKeystoreId) throws FindException, KeyStoreException, IOException {
        return keyAlias == null ? getSslInfo() : keyStoreManager.lookupKeyByKeyAlias(keyAlias, preferredKeystoreId);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        invalidateCachedCerts();
    }

    private void invalidateCachedCerts() {
        cachedSslInfo.set(null);
        cachedCaInfo.set(null);
        cachedAuditViewerInfo.set(null);
        cachedAuditSigningInfo.set(null);
    }

    private SsgKeyEntry getCachedEntry(AtomicReference<SsgKeyEntry> cache, String propertyName, boolean required) throws IOException, ObjectNotFoundException {
        SsgKeyEntry ret;
        for (;;) {
            ret = cache.get();
            if (ret != null)
                break;

            try {
                ret = lookupEntry(propertyName);
            } catch (KeyStoreException e) {
                throw new IOException("Unable to retrieve key for " + propertyName + ": " + ExceptionUtils.getMessage(e), e);
            } catch (FindException e) {
                String reason = ExceptionUtils.getMessage(e);
                logger.log(required ? Level.WARNING : Level.INFO, "Private key for " + propertyName + " not available: " + reason);
                ret = NULL_ENTRY;
                /* FALLTHROUGH and cache the NULL_ENTRY */
            }

            if (cache.compareAndSet(null, ret))
                break;

            /* We lost the race, but a value is now available in the cache.  Loop and try the lookup again. */
        }

        if (required && (ret == null || ret == NULL_ENTRY))
            throw new ObjectNotFoundException("Required key unavailable: " + propertyName);
        return ret == NULL_ENTRY ? null : ret;
    }

    private Pair<Goid, String> getKeyStoreGoidAndAlias(String propertyName) throws IOException {
        String propVal = ConfigFactory.getUncachedConfig().getProperty( propertyName );
        if (propVal == null || propVal.trim().length() < 1)
            return null;

        Matcher matcher = KEYSTORE_ID_AND_ALIAS_PATTERN.matcher(propVal);
        if (!matcher.matches()) {
            logger.log(Level.WARNING, "Badly formatted value for serverconfig property " + propertyName + ": \"" + propVal + "\"");
            return null;
        }

        try {
            Goid keystoreGoid = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, matcher.group(1));
            String keyAlias = matcher.group(2);
            return new Pair<Goid, String>(keystoreGoid, keyAlias);
        } catch (IllegalArgumentException iae) {
            logger.log(Level.WARNING, "Badly formatted value for serverconfig property " + propertyName + ": " + propVal);
            return null;
        }
    }

    private SsgKeyEntry lookupEntry(String propertyName) throws IOException, FindException, KeyStoreException {
        Pair<Goid, String> keyaddr = getKeyStoreGoidAndAlias(propertyName);
        if (keyaddr == null)
            return NULL_ENTRY;

        try {
            return keyStoreManager.lookupKeyByKeyAlias(keyaddr.right, keyaddr.left);
        } catch (ObjectNotFoundException e) {
            return NULL_ENTRY;
        }
    }
}
