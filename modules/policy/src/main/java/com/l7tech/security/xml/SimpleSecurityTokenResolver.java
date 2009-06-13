/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.util.ExceptionUtils;

import javax.security.auth.x500.X500Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigInteger;

/**
 * A SecurityTokenResolver that is given a small list of certs that it is to recognize.
 * Single threaded only.
 */
public class SimpleSecurityTokenResolver implements SecurityTokenResolver {
    private static final Logger logger = Logger.getLogger(SimpleSecurityTokenResolver.class.getName());

    private final List<Cert> certs = new ArrayList<Cert>();
    private final List<MyKey> keys = new ArrayList<MyKey>();
    private Map<String, byte[]> encryptedKeys = new HashMap<String, byte[]>();
    private Map<String, KerberosSigningSecurityToken> kerberosTokens = new HashMap<String, KerberosSigningSecurityToken>();

    private static class Cert extends TrustedCert {

        public Cert(X509Certificate cert) {
            if (cert == null)
                throw new NullPointerException("A certificate is required");
            setCertificate(cert);
        }

        public Object getPayload() {
            return getCertificate();
        }
    }

    private static class MyKey extends Cert {
        private final SignerInfo signerInfo;

        public MyKey(SignerInfo info) {
            super(info.getCertificateChain()[0]);
            this.signerInfo = info;
        }

        public Object getPayload() {
            return signerInfo;
        }
    }

    /**
     * Create a thumbprint resolver that will find no certs at all, ever.  Useful for testing
     * (wiring up beans that require a resolver), or if you plan to add certs or keys later
     * with {@link #addCert(java.security.cert.X509Certificate)} or {@link #addPrivateKey(SignerInfo)}. 
     */
    public SimpleSecurityTokenResolver() {
    }

    /**
     * Create a thumbprint resolver that will recognize any cert in the specified list.
     * For convenience, the certs array may contain nulls which will be ignored.
     * @param certs certs to resolve
     * @throws java.security.cert.CertificateEncodingException if one of the certificates cannot be encoded
     */
    public SimpleSecurityTokenResolver(X509Certificate[] certs) throws CertificateEncodingException {
        this(certs, null);
    }

    /**
     * Create a resolver that will recognize the specified cert.
     *
     * @param cert the cert to resolve
     * @throws java.security.cert.CertificateEncodingException if the certificate cannot be encoded
     */
    public SimpleSecurityTokenResolver(X509Certificate cert) throws CertificateEncodingException {
        this(new X509Certificate[] { cert });
    }

    /**
     * Create a resolver that will recognize the specified private key (with corresponding cert).
     *
     * @param publicCert  the certificate that corresponds to privateKey, or null to resolve nothing.
     * @param privateKey  the private key that corresponds to publicCert, or null to resolve the cert but not the key.
     */
    public SimpleSecurityTokenResolver(X509Certificate publicCert, PrivateKey privateKey) {
        if (publicCert != null) {
            addCerts(new X509Certificate[] { publicCert });
            if (privateKey != null)
                addPrivateKeys(new SignerInfo[] { new SignerInfo(privateKey, new X509Certificate[] { publicCert } ) });
        }
    }

    /**
     * Create a resolver that will recognize any cert or private key in the specified lists.
     * For convenience, the certs or keys arrays may contain nulls which will be ignored.
     *
     * @param certs certs to resolve
     * @param privateKeys keys to resolve
     */
    public SimpleSecurityTokenResolver(X509Certificate[] certs, SignerInfo[] privateKeys) {
        addCerts(certs);
        addPrivateKeys(privateKeys);
    }

    public void addCerts(X509Certificate[] newcerts) {
        if (newcerts != null) for (X509Certificate newcert : newcerts) {
            addCert(newcert);
        }
    }

    public void addCert(X509Certificate cert) {
        if (cert != null) certs.add(new Cert(cert));
    }

    public void addPrivateKeys(SignerInfo[] privateKeys) {
        if (privateKeys != null) for (SignerInfo privateKey : privateKeys) {
            addPrivateKey(privateKey);
        }
    }

    public void addPrivateKey(SignerInfo privateKey) {
        if (privateKey != null && privateKey.getCertificate() != null) keys.add(new MyKey(privateKey));
    }

    public X509Certificate lookup(String thumbprint) {
        X509Certificate found = (X509Certificate)doLookupByX09Thumbprint(certs, thumbprint);
        if (found != null) return found;
        SignerInfo siFound = lookupPrivateKeyByX509Thumbprint(thumbprint);
        if (siFound != null) return siFound.getCertificateChain()[0];
        return null;
    }

    private <C extends Cert> Object doLookupByX09Thumbprint(Collection<C> toSearch, String thumbprint) {
        for (Cert cert : toSearch) {
            String thumb = cert.getThumbprintSha1();
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

    private <C extends Cert> Object doLookupBySki(Collection<C> toSearch, String targetSki) {
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

    private <C extends Cert> Object doLookupByKeyName(Collection<C> toSearch, String keyName) {
        for (final Cert cert : toSearch) {
            final String name = cert.getSubjectDn();
            if (name != null && name.equals(keyName))
                return cert.getPayload();
        }
        return null;
    }

    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        X509Certificate found = (X509Certificate)doLookupByIssuerAndSerial(certs, issuer, serial);
        if (found != null) return found;
        SignerInfo siFound = (SignerInfo)doLookupByIssuerAndSerial(keys, issuer, serial);
        if (siFound != null) return siFound.getCertificateChain()[0];
        return null;
    }

    private <C extends Cert> Object doLookupByIssuerAndSerial( final Collection<C> toSearch, final X500Principal issuer, final BigInteger serial ) {
        for (final Cert cert : toSearch) {
            final X509Certificate certificate = (X509Certificate) cert.getPayload();
            if (certificate.getIssuerX500Principal().equals(issuer) && certificate.getSerialNumber().equals(serial))
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

    public void setKerberosTokens(Map<String, KerberosSigningSecurityToken> kerberosTokens) {
        if (kerberosTokens == null) throw new NullPointerException();
        this.kerberosTokens = kerberosTokens;
    }

    public byte[] getSecretKeyByEncryptedKeySha1(String encryptedKeySha1) {
        return encryptedKeys.get(encryptedKeySha1);
    }

    public void putSecretKeyByEncryptedKeySha1(String encryptedKeySha1, byte[] secretKey) {
        encryptedKeys.put(encryptedKeySha1, secretKey);
    }

    public KerberosSigningSecurityToken getKerberosTokenBySha1(String kerberosSha1) {
        return kerberosTokens.get(kerberosSha1);
    }
}
