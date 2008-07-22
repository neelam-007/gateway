/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;

import java.security.Principal;
import java.security.PrivateKey;
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

    private Cert[] certs;
    private final MyKey[] keys;
    private Map<String, byte[]> encryptedKeys = new HashMap<String, byte[]>();
    private Map<String, KerberosSecurityToken> kerberosTokens = new HashMap<String, KerberosSecurityToken>();

    private static class Cert {
        private final X509Certificate cert;
        private String thumb = null;
        private String ski = null;
        private Principal subjectDn = null;

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

        public Object getPayload() {
            return cert;
        }

        public Principal getSubjectDN() {
            if (subjectDn == null && cert != null) {
                subjectDn = cert.getSubjectDN();
            }
            return subjectDn;
        }
    }

    private static class MyKey extends Cert {
        private final SignerInfo signerInfo;

        public MyKey(SignerInfo info) {
            super(info.getCertificateChain()[0]);
            this.signerInfo = info;
        }

        @Override
        public Object getPayload() {
            return signerInfo;
        }
    }

    /**
     * Create a thumbprint resolver that will find no certs at all, ever.  Useful for testing
     * (wiring up beans that require a resolver).
     */
    public SimpleSecurityTokenResolver() {
        this.certs = new Cert[0];
        this.keys = new MyKey[0];
    }

    /**
     * Create a thumbprint resolver that will recognize any cert in the specified list.
     * For convenience, the certs array may contain nulls which will be ignored.
     * @param certs certs to resolve
     */
    public SimpleSecurityTokenResolver(X509Certificate[] certs) {
        this(certs, null);
    }

    /**
     * Create a resolver that will recognize the specified cert.
     *
     * @param cert the cert to resolve
     */
    public SimpleSecurityTokenResolver(X509Certificate cert) {
        this(new X509Certificate[] { cert });
    }

    /**
     * Create a resolver that will recognize the specified private key (with corresponding cert).
     *
     * @param publicCert  the certificate that corresponds to privateKey.  Required.
     * @param privateKey  the private key that corresponds to publicCert.  Required.
     */
    public SimpleSecurityTokenResolver(X509Certificate publicCert, PrivateKey privateKey) {
        this(new X509Certificate[] { publicCert },
             new SignerInfo[] { new SignerInfo(privateKey, new X509Certificate[] { publicCert } ) });
    }

    /**
     * Create a resolver that will recognize any cert or private key in the specified lists.
     * For convenience, the certs or keys arrays may contain nulls which will be ignored.
     *
     * @param certs certs to resolve
     * @param privateKeys keys to resolve
     */
    public SimpleSecurityTokenResolver(X509Certificate[] certs, SignerInfo[] privateKeys) {
        if (certs != null) {
            this.certs = new Cert[certs.length];
            for (int i = 0; i < certs.length; i++)
                this.certs[i] = new Cert(certs[i]);
        } else {
            this.certs = new Cert[0];
        }
        if (privateKeys != null) {
            this.keys = new MyKey[privateKeys.length];
            for (int i = 0; i < privateKeys.length; i++)
                this.keys[i] = new MyKey(privateKeys[i]);
        } else {
            this.keys = new MyKey[0];
        }
    }

    public void addCerts(X509Certificate[] newcerts) {
        int newSize = newcerts.length;
        if (certs != null) newSize += certs.length;
        Cert[] sum = new Cert[newSize];
        int pos = 0;
        if (certs != null) {
            System.arraycopy(certs, 0, sum, 0, certs.length);
            pos += certs.length;
        }
        for (int i = 0; i < newcerts.length; i++) {
            sum[i+pos] = new Cert(newcerts[i]);
        }
        certs = sum;
    }

    public X509Certificate lookup(String thumbprint) {
        X509Certificate found = (X509Certificate)doLookupByX09Thumbprint(certs, thumbprint);
        if (found != null) return found;
        SignerInfo siFound = lookupPrivateKeyByX509Thumbprint(thumbprint);
        if (siFound != null) return siFound.getCertificateChain()[0];
        return null;
    }

    private <C extends Cert> Object doLookupByX09Thumbprint(C[] toSearch, String thumbprint) {
        for (Cert cert : toSearch) {
            String thumb = cert.getThumb();
            if (thumb != null && thumb.equals(thumbprint))
                return cert.getPayload();
        }
        return null;
    }

    public X509Certificate lookupBySki(String targetSki) {
        X509Certificate found = (X509Certificate)doLookupBySki(certs, targetSki);
        if (found != null) return found;
        SignerInfo siFound = lookupPrivateKeyBySki(targetSki);
        if (siFound != null) return siFound.getCertificateChain()[0];
        return null;
    }

    private <C extends Cert> Object doLookupBySki(C[] toSearch, String targetSki) {
        for (Cert cert : toSearch) {
            String ski = cert.getSki();
            if (ski != null && ski.equals(targetSki))
                return cert.getPayload();
        }
        return null;
    }

    public X509Certificate lookupByKeyName(final String keyName) {
        X509Certificate found = (X509Certificate)doLookupByKeyName(certs, keyName);
        if (found != null) return found;
        SignerInfo siFound = lookupPrivateKeyByKeyName(keyName);
        if (siFound != null) return siFound.getCertificateChain()[0];
        return null;
    }

    private <C extends Cert> Object doLookupByKeyName(C[] toSearch, String keyName) {
        for (final Cert cert : toSearch) {
            final String name = cert.getSubjectDN().getName();
            if (name != null && name.equals(keyName))
                return cert.getPayload();
        }
        return null;
    }

    public SignerInfo lookupPrivateKeyByCert(X509Certificate cert) {
        try {
            String thumb = CertUtils.getThumbprintSHA1(cert);
            return lookupPrivateKeyByX509Thumbprint(thumb);
        } catch (CertificateEncodingException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Ignoring badly-encoded certificate: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    public SignerInfo lookupPrivateKeyByX509Thumbprint(String thumbprint) {
        return (SignerInfo)doLookupByX09Thumbprint(keys, thumbprint);
    }

    public SignerInfo lookupPrivateKeyBySki(String ski) {
        return (SignerInfo)doLookupBySki(keys, ski);
    }

    public SignerInfo lookupPrivateKeyByKeyName(String keyName) {
        return (SignerInfo)doLookupByKeyName(keys, keyName);
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
