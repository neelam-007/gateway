/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.common.security.X509Entity;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.WhirlycacheFactory;
import com.l7tech.common.util.Background;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.FindException;
import com.whirlycott.cache.Cache;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that can look up any certificate known to the SSG by thumbprint.
 */
public class TrustedAndUserCertificateResolver implements SecurityTokenResolver {
    private static final Logger logger = Logger.getLogger(TrustedAndUserCertificateResolver.class.getName());
    private final TrustedCertManager trustedCertManager;
    private final ClientCertManager clientCertManager;

    private final X509Certificate sslKeystoreCertificate;
    private final String sslKeystoreCertThumbprint;
    private final String sslKeystoreCertSki;

    private final X509Certificate rootCertificate;
    private final String rootCertificateThumbprint;
    private final String rootCertificateSki;

    private final Cache encryptedKeyCache;
    private final AtomicBoolean encryptedKeyCacheEnabled = new AtomicBoolean();


    /**
     * Construct the Gateway's security token resolver.
     *
     * @param clientCertManager      required
     * @param trustedCertManager     required
     * @param sslKeystoreCertificate     the Gateway's SSL cert. required
     * @param rootCertificate           the Gateway's CA cert. required
     * @param serverConfig           required
     */
    public TrustedAndUserCertificateResolver(ClientCertManager clientCertManager,
                                             TrustedCertManager trustedCertManager,
                                             X509Certificate sslKeystoreCertificate,
                                             X509Certificate rootCertificate,
                                             final ServerConfig serverConfig)
    {
        this.trustedCertManager = trustedCertManager;
        this.clientCertManager = clientCertManager;
        this.sslKeystoreCertificate = sslKeystoreCertificate;
        this.rootCertificate = rootCertificate;

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

        try {
            if (sslKeystoreCertificate != null) {
                this.sslKeystoreCertThumbprint = CertUtils.getThumbprintSHA1(sslKeystoreCertificate);
                this.sslKeystoreCertSki = CertUtils.getSki(sslKeystoreCertificate);
            } else {
                this.sslKeystoreCertThumbprint = null;
                this.sslKeystoreCertSki = null;
            }
            if (rootCertificate != null) {
                this.rootCertificateThumbprint = CertUtils.getThumbprintSHA1(rootCertificate);
                this.rootCertificateSki = CertUtils.getSki(rootCertificate);
            } else {
                this.rootCertificateThumbprint = null;
                this.rootCertificateSki = null;
            }
        } catch (CertificateException e) {
            throw new RuntimeException("Invalid SSL or root certificate", e);
        }
    }

    public X509Certificate lookup(String thumbprint) {
        try {
            if (rootCertificateThumbprint != null && rootCertificateThumbprint.equals(thumbprint))
                return rootCertificate;

            if (sslKeystoreCertThumbprint != null && sslKeystoreCertThumbprint.equals(thumbprint))
                return sslKeystoreCertificate;

            List got = trustedCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            got = clientCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Bad certificate in database: " + e.getMessage(), e);
            return null;
        }
    }

    public X509Certificate lookupBySki(String ski) {
        try {
            if (rootCertificateSki != null && rootCertificateSki.equals(ski))
                return rootCertificate;

            if (sslKeystoreCertSki != null && sslKeystoreCertSki.equals(ski))
                return sslKeystoreCertificate;

            List got = trustedCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            got = clientCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Bad certificate in database: " + e.getMessage(), e);
            return null;
        }
    }

    public X509Certificate lookupByKeyName(String keyName) {
        // TODO Implement this using a lookup by cert DN if we decide to bother supporting this feature here
        return null;
    }

    public SecretKey getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        if (!encryptedKeyCacheEnabled.get()) return null;
        return (SecretKey)encryptedKeyCache.retrieve(encryptedKeySha1);
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, SecretKey secretKey) {
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
}
