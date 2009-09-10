/*
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.cert.X509Entity;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.security.keystore.KeystoreFile;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.whirlycott.cache.Cache;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Looks up any certificate known to the SSG by a variety of search criteria.
 */
public class TrustedCertificateResolver implements SecurityTokenResolver, ApplicationListener {
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
     * @param serverConfig           required
     * @param keyStoreManager     private key sources.  required
     */
    public TrustedCertificateResolver( final TrustedCertManager trustedCertManager,
                                       final ServerConfig serverConfig,
                                       final SsgKeyStoreManager keyStoreManager )
    {
        this.trustedCertManager = trustedCertManager;
        this.keyStoreManager = keyStoreManager;

        final int checkPeriod = 10181;
        final int defaultSize = 1000; // Size to use if not configured, or if not enabled at first (since we can't change the size if it's later enabled)
        int csize = serverConfig.getIntPropertyCached(ServerConfig.PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES, defaultSize, checkPeriod);
        encryptedKeyCacheEnabled.set(csize > 0);
        encryptedKeyCache = WhirlycacheFactory.createCache("Ephemeral key cache",
                                                           encryptedKeyCacheEnabled.get() ? csize : defaultSize,
                                                           127,
                                                           WhirlycacheFactory.POLICY_LFU);

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                int csize = serverConfig.getIntPropertyCached(ServerConfig.PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES, defaultSize, checkPeriod - 1);
                boolean newval = csize > 0;
                boolean oldval = encryptedKeyCacheEnabled.getAndSet(newval);
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
        // TODO Implement this using a lookup by cert DN if we decide to bother supporting this feature here

        SignerInfo si = lookupPrivateKeyByKeyName(keyName);
        if (si != null) return si.getCertificateChain()[0];

        return null;
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

                        infos.add(new SignerInfo(entry.getPrivateKey(), entry.getCertificateChain()));
                    }
                } catch (ObjectNotFoundException e) {
                    logger.log(Level.WARNING, "Unable to access private key alias " + alias + " in key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (KeyStoreException e) {
                    logger.log(Level.WARNING, "Unable to access private key alias " + alias + " in key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), e);
                } catch (UnrecoverableKeyException e) {
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
        if (!encryptedKeyCacheEnabled.get()) return null;
        return (byte[])encryptedKeyCache.retrieve(encryptedKeySha1);
    }

    @Override
    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        if (encryptedKeyCacheEnabled.get()) encryptedKeyCache.store(encryptedKeySha1, secretKey);
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
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (KeystoreFile.class.equals(event.getEntityClass())) {
                // Invalidate key cache so it gets rebuilt on next use
                keyCache = null;
            }
        }
    }
}
