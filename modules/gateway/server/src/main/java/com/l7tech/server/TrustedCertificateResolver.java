package com.l7tech.server;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.cert.X509Entity;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.event.EntityClassEvent;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Background;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SoapConstants;
import com.whirlycott.cache.Cache;
import org.springframework.context.ApplicationEvent;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Looks up any certificate known to the SSG by a variety of search criteria.
 */
public class TrustedCertificateResolver implements SecurityTokenResolver, PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(TrustedCertificateResolver.class.getName());
    private final TrustedCertManager trustedCertManager;

    private final Cache encryptedKeyCache;
    private final AtomicBoolean encryptedKeyCacheEnabled = new AtomicBoolean();

    private final SsgKeyStoreManager keyStoreManager;

    private volatile SimpleSecurityTokenResolver keyCache;

    /**
     * Construct the Gateway's security token resolver.
     *
     * @param trustedCertManager     required
     * @param config                 required
     * @param keyStoreManager     private key sources.  required
     */
    public TrustedCertificateResolver( final TrustedCertManager trustedCertManager,
                                       final Config config,
                                       final SsgKeyStoreManager keyStoreManager )
    {
        this.trustedCertManager = trustedCertManager;
        this.keyStoreManager = keyStoreManager;

        final int checkPeriod = 10181;
        final int defaultSize = 1000; // Size to use if not configured, or if not enabled at first (since we can't change the size if it's later enabled)
        final int csize = config.getIntProperty( ServerConfigParams.PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES, defaultSize );
        encryptedKeyCacheEnabled.set(csize > 0);
        final int cacheSize = encryptedKeyCacheEnabled.get() ? csize : defaultSize;
        encryptedKeyCache = WhirlycacheFactory.createCache("Ephemeral key cache",
                                                           cacheSize,
                                                           127,
                                                           WhirlycacheFactory.POLICY_LRU);
        logger.info("Initializing ephemeral key cache with size " + cacheSize);

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                final int csize = config.getIntProperty( ServerConfigParams.PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES, defaultSize );
                final boolean newval = csize > 0;
                final boolean oldval = encryptedKeyCacheEnabled.getAndSet(newval);
                if (newval != oldval && logger.isLoggable(Level.INFO))
                    logger.info("Ephemeral key cache is now " + (newval ? "enabled" : "disabled"));
            }
        }, checkPeriod, checkPeriod);
    }

    @Override
    public X509Certificate lookup(String thumbprint) {
        try {
            SignerInfo si = lookupPrivateKeyByX509Thumbprint(thumbprint);
            if (si != null) return si.getCertificateChain()[0];

            List<? extends X509Entity> got = trustedCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return got.get(0).getCertificate();

            return asCert(lookupPrivateKeyByX509Thumbprint(thumbprint));
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        }
    }

    @Override
    public X509Certificate lookupBySki(String ski) {
        try {
            SignerInfo si = lookupPrivateKeyBySki(ski);
            if (si != null) return si.getCertificateChain()[0];

            List<? extends X509Entity> got = trustedCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return got.get(0).getCertificate();

            return asCert(lookupPrivateKeyBySki(ski));
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        }
    }

    @Override
    public X509Certificate lookupByKeyName(String keyName) {
        try {
            Collection<TrustedCert> got = trustedCertManager.findBySubjectDn(keyName);
            if (got != null && !got.isEmpty()) return got.iterator().next().getCertificate();

            SignerInfo si = lookupPrivateKeyByKeyName(keyName);
            if (si != null) return si.getCertificateChain()[0];

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        try {
            List<? extends X509Entity> got = trustedCertManager.findByIssuerAndSerial(issuer, serial);
            if (got != null && got.size() >= 1) return got.get(0).getCertificate();

            return asCert(lookupPrivateKeyByIssuerAndSerial(issuer, serial));
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    private SecurityTokenResolver getKeyCache() {
        SimpleSecurityTokenResolver kc = keyCache;
        return kc != null ? kc : rebuildKeyCache();
    }

    private synchronized SecurityTokenResolver rebuildKeyCache() {
        List<SignerInfo> infos = new ArrayList<SignerInfo>();

        final List<SsgKeyFinder> found;
        try {
            found = keyStoreManager.findAll();
        } catch (FindException e) {
            String mess = "Unable to access key store: " + ExceptionUtils.getMessage(e);
            throw new RuntimeException(mess, e);
        } catch (KeyStoreException e) {
            String mess = "Unable to access key store: " + ExceptionUtils.getMessage(e);
            throw new RuntimeException(mess, e);
        }

        for (SsgKeyFinder keyFinder : found) {
            final List<String> aliases;
            try {
                aliases = keyFinder.getAliases();
            } catch (KeyStoreException e) {
                logger.log(Level.WARNING, "Unable to access key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), e);
                continue;
            }
            for (String alias : aliases) {
                try {
                    SsgKeyEntry entry = keyFinder.getCertificateChain(alias);
                    if (entry.isPrivateKeyAvailable()) {
                        infos.add(entry);
                    }
                } catch (ObjectNotFoundException e) {
                    logger.log(Level.WARNING, "Unable to access private key alias " + alias + " in key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (KeyStoreException e) {
                    logger.log(Level.WARNING, "Unable to access private key alias " + alias + " in key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), e);
                }
            }
        }

        return keyCache = new SimpleSecurityTokenResolver(null, infos.toArray(new SignerInfo[infos.size()]));
    }

    private static X509Certificate asCert(SignerInfo signerInfo) {
        return signerInfo == null ? null : signerInfo.getCertificate();
    }

    @Override
    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        return getKeyCache().lookupPrivateKeyByCert(cert);
    }

    @Override
    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        return getKeyCache().lookupPrivateKeyByX509Thumbprint(thumbprint);
    }

    @Override
    public SignerInfo lookupPrivateKeyBySki(String ski) {
        return getKeyCache().lookupPrivateKeyBySki(ski);
    }

    @Override
    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        return getKeyCache().lookupPrivateKeyByKeyName(keyName);
    }

    @Override
    public SignerInfo lookupPrivateKeyByIssuerAndSerial(X500Principal issuer, BigInteger serial) {
        return getKeyCache().lookupPrivateKeyByIssuerAndSerial(issuer, serial);
    }

    @Override
    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        return getSecretKeyByTokenIdentifier( SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1, encryptedKeySha1 );
    }

    @Override
    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        putSecretKeyByTokenIdentifier( SoapConstants.VALUETYPE_ENCRYPTED_KEY_SHA1, encryptedKeySha1, secretKey );
    }

    @Override
    public byte[] getSecretKeyByTokenIdentifier( final String type,
                                                 final String identifier ) {
        if (!encryptedKeyCacheEnabled.get()) return null;
        return (byte[])encryptedKeyCache.retrieve( new SecretKeyKey( type, identifier ) );
    }

    @Override
    public void putSecretKeyByTokenIdentifier( final String type,
                                               final String identifier,
                                               final byte[] secretKey ) {
        if (encryptedKeyCacheEnabled.get()) encryptedKeyCache.store( new SecretKeyKey( type, identifier ), secretKey);
    }

    /**
     * Ssg implementation lookup of kerberos token; currently always fails.
     *
     * @return currently always returns null
     */
    @Override
    public KerberosSigningSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityClassEvent) {
            EntityClassEvent event = (EntityClassEvent)applicationEvent;
            if (KeystoreFile.class.equals(event.getEntityClass())) {
                // Invalidate key cache so it gets rebuilt on next use
                keyCache = null;
            }
        }
    }

    private static final class SecretKeyKey {
        private final String type;
        private final String identifier;

        private SecretKeyKey( final String type,
                              final String identifier ) {
            if ( type==null ) throw new IllegalArgumentException( "type is required" );
            if ( identifier==null ) throw new IllegalArgumentException( "identifier is required" );
            this.type = type;
            this.identifier = identifier;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final SecretKeyKey that = (SecretKeyKey) o;

            if ( !identifier.equals( that.identifier ) ) return false;
            if ( !type.equals( that.type ) ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + identifier.hashCode();
            return result;
        }
    }
}
