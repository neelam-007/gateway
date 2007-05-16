/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.util.CertUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A SecurityTokenResolver that is given a small list of certs that it is to recognize.
 * Single threaded only.
 */
public class SimpleSecurityTokenResolver implements SecurityTokenResolver {
    private static final Logger logger = Logger.getLogger(SimpleSecurityTokenResolver.class.getName());

    private final Cert[] certs;
    private Map<String, byte[]> encryptedKeys = new HashMap<String, byte[]>();
    private Map<String, KerberosSecurityToken> kerberosTokens = new HashMap<String, KerberosSecurityToken>();

    private static class Cert {
        private final X509Certificate cert;
        private String thumb = null;
        private String ski = null;

        public Cert(X509Certificate cert) {
            this.cert = cert;
        }

        public String getThumb() {
            if (thumb == null && cert != null) {
                try {
                    thumb = CertUtils.getThumbprintSHA1(cert);
                } catch (CertificateEncodingException e) {
                    logger.log(Level.WARNING, "Invalid certificate: " + e.getMessage(), e);
                }
            }
            return thumb;
        }

        public String getSki() {
            if (ski == null && cert != null) {
                ski = CertUtils.getSki(cert);
            }
            return ski;
        }
    }

    /**
     * Create a thumbprint resolver that will find no certs at all, ever.  Useful for testing
     * (wiring up beans that require a resolver).
     */
    public SimpleSecurityTokenResolver() {
        this.certs = new Cert[0];
    }

    /**
     * Create a thumbprint resolver that will recognize any cert in the specified list.
     * For convenience, the certs array may contain nulls which will be ignored.
     * @param certs certs to resolve
     */
    public SimpleSecurityTokenResolver(X509Certificate[] certs) {
        if (certs != null) {
            this.certs = new Cert[certs.length];
            for (int i = 0; i < certs.length; i++)
                this.certs[i] = new Cert(certs[i]);
        } else {
            this.certs = new Cert[0];
        }
    }

    public SimpleSecurityTokenResolver(X509Certificate cert) {
        this(new X509Certificate[] { cert });
    }

    public X509Certificate lookup(String thumbprint) {
        for (Cert cert : certs) {
            String thumb = cert.getThumb();
            if (thumb != null && thumb.equals(thumbprint))
                return cert.cert;
        }
        return null;
    }

    public X509Certificate lookupBySki(String targetSki) {
        for (Cert cert : certs) {
            String ski = cert.getSki();
            if (ski != null && ski.equals(targetSki))
                return cert.cert;
        }
        return null;
    }

    public X509Certificate lookupByKeyName(final String keyName) {
        for (final Cert cert : certs) {
            final String name = cert.cert.getSubjectDN().getName();
            if (name != null && name.equals(keyName))
                return cert.cert;
        }
        return null;
    }

    public Map getEncryptedKeys() {
        return encryptedKeys;
    }

    public void setEncryptedKeys(Map<String, byte[]> encryptedKeys) {
        if (encryptedKeys == null) throw new NullPointerException();
        this.encryptedKeys = encryptedKeys;
    }

    public Map getKerberosTokens() {
        return kerberosTokens;
    }

    public void setKerberosTokens(Map<String, KerberosSecurityToken> kerberosTokens) {
        if (kerberosTokens == null) throw new NullPointerException();
        this.kerberosTokens = kerberosTokens;
    }

    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        return encryptedKeys.get(encryptedKeySha1);
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        encryptedKeys.put(encryptedKeySha1, secretKey);
    }

    public KerberosSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return kerberosTokens.get(kerberosSha1);
    }
}
