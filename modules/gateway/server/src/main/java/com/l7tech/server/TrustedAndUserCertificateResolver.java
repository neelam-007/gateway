/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.cert.X509Entity;
import com.l7tech.security.token.KerberosSecurityToken;
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
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigInteger;

/**
 * Bean that can look up any certificate known to the SSG by thumbprint.
 */
public class TrustedAndUserCertificateResolver implements SecurityTokenResolver, ApplicationListener {
    private static final Logger logger = Logger.getLogger(TrustedAndUserCertificateResolver.class.getName());
    private final TrustedCertManager trustedCertManager;
    private final ClientCertManager clientCertManager;

    private final Cache encryptedKeyCache;
    private final AtomicBoolean encryptedKeyCacheEnabled = new AtomicBoolean();

    private final SsgKeyStoreManager keyStoreManager;

    private final AtomicReference<SimpleSecurityTokenResolver> keyCache = new AtomicReference<SimpleSecurityTokenResolver>();


    /**
     * Construct the Gateway's security token resolver.
     *
     * @param clientCertManager      required
     * @param trustedCertManager     required
     * @param serverConfig           required
     * @param keyStoreManager     private key sources.  required
     */
    public TrustedAndUserCertificateResolver(ClientCertManager clientCertManager,
                                             TrustedCertManager trustedCertManager,
                                             final ServerConfig serverConfig,
                                             SsgKeyStoreManager keyStoreManager)
    {
        this.trustedCertManager = trustedCertManager;
        this.clientCertManager = clientCertManager;
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
            public void run() {
                int csize = serverConfig.getIntPropertyCached(ServerConfig.PARAM_EPHEMERAL_KEY_CACHE_MAX_ENTRIES, defaultSize, checkPeriod - 1);
                boolean newval = csize > 0;
                boolean oldval = encryptedKeyCacheEnabled.getAndSet(newval);
                if (newval != oldval && logger.isLoggable(Level.INFO))
                    logger.info("Ephemeral key cache is now " + (newval ? "enabled" : "disabled"));
            }
        }, checkPeriod, checkPeriod);
    }

    public X509Certificate lookup(String thumbprint) {
        try {
            SignerInfo si = lookupPrivateKeyByX509Thumbprint(thumbprint);
            if (si != null) return si.getCertificateChain()[0];

            List got = trustedCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            got = clientCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        }
    }

    public X509Certificate lookupBySki(String ski) {
        try {
            SignerInfo si = lookupPrivateKeyBySki(ski);
            if (si != null) return si.getCertificateChain()[0];

            List got = trustedCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            got = clientCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        }
    }

    public X509Certificate lookupByKeyName(String keyName) {
        // TODO Implement this using a lookup by cert DN if we decide to bother supporting this feature here

        SignerInfo si = lookupPrivateKeyByKeyName(keyName);
        if (si != null) return si.getCertificateChain()[0];

        return null;
    }

    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        return null;
    }

    private SecurityTokenResolver getKeyCache() {
        SimpleSecurityTokenResolver kc = keyCache.get();
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
                } catch (KeyStoreException e) {
                    logger.log(Level.WARNING, "Unable to access private key alias " + alias + " in key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), e);
                } catch (UnrecoverableKeyException e) {
                    logger.log(Level.WARNING, "Unable to access private key alias " + alias + " in key store " + keyFinder.getName() + ": " + ExceptionUtils.getMessage(e), e);
                }
            }
        }

        SimpleSecurityTokenResolver kc = new SimpleSecurityTokenResolver(null, infos.toArray(new SignerInfo[infos.size()]));
        keyCache.set(kc);
        return kc;
    }

    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        return getKeyCache().lookupPrivateKeyByCert(cert);
    }

    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        return getKeyCache().lookupPrivateKeyByX509Thumbprint(thumbprint);
    }

    public SignerInfo lookupPrivateKeyBySki(String ski) {
        return getKeyCache().lookupPrivateKeyBySki(ski);
    }

    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        return getKeyCache().lookupPrivateKeyByKeyName(keyName);
    }

    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        if (!encryptedKeyCacheEnabled.get()) return null;
        return (byte[])encryptedKeyCache.retrieve(encryptedKeySha1);
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        if (encryptedKeyCacheEnabled.get()) encryptedKeyCache.store(encryptedKeySha1, secretKey);
    }

    /**
     * Ssg implementation lookup of kerberos token; currently always fails.
     *
     * @return currently always returns null
     */
    public KerberosSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return null;
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (KeystoreFile.class.equals(event.getEntityClass())) {
                // Invalidate key cache so it gets rebuilt on next use
                keyCache.set(null);
            }
        }
    }
}
