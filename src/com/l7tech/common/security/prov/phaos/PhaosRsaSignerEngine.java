/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.phaos;

import com.l7tech.common.security.RsaSignerEngine;
import com.phaos.cert.CertificateRequest;
import com.phaos.cert.X509;
import com.phaos.cert.X500Name;
import sun.security.x509.X509CertImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class PhaosRsaSignerEngine implements RsaSignerEngine {
    private String keyStorePath;
    private String keyStoreType;
    private String storePass;
    private String privateKeyPassString;
    private String privateKeyAlias;
    private PrivateKey privateKey;
    private X509Certificate caCert;
    private SecureRandom random;

    public PhaosRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        this.keyStorePath = keyStorePath;
        this.storePass = storePass;
        this.privateKeyAlias = privateKeyAlias;
        this.privateKeyPassString = privateKeyPass;
        this.keyStoreType = storeType;

        try {
            init();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void init() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        InputStream is = new FileInputStream(keyStorePath);

        if (storePass == null)
            throw new IllegalArgumentException("A CA keystore passphrase must be provided");
        char[] keyStorePass = storePass.toCharArray();
        keyStore.load(is, keyStorePass);

        if (privateKeyPassString == null)
            throw new IllegalArgumentException("A CA private key passphrase must be provided");
        char[] privateKeyPass = privateKeyPassString.toCharArray();

        privateKey = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPass);
        if (privateKey == null) {
            logger.severe("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            throw new IllegalArgumentException("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
        }
        Certificate[] certchain = keyStore.getCertificateChain(privateKeyAlias);
        if (certchain.length < 1) {
            logger.severe("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            throw new IllegalArgumentException("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
        }
        // We only support a ca hierarchy with depth 2.
        caCert = (X509Certificate) certchain[0];


        random = new SecureRandom();
        long seed = (new Date().getTime()) + this.hashCode();
        random.setSeed(seed);
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req, String subject) throws Exception {
        CertificateRequest csr = new CertificateRequest(pkcs10req);
        if (subject != null) {
            csr.setSubject(new X500Name(subject));
        }
        X509 phaosCaCert = new X509(caCert.getEncoded());
        com.phaos.crypto.PrivateKey phaosPrivateKey = new com.phaos.crypto.RSAPrivateKey(privateKey.getEncoded());

        byte[] serialBytes = new byte[8];
        random.nextBytes(serialBytes);
        BigInteger serial = new BigInteger(serialBytes);

        final X509 cert = new X509(csr, phaosCaCert, phaosPrivateKey, serial, CERT_DAYS_VALID);
        return new X509CertImpl(cert.getEncoded());
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req, String subject, long expiration) throws Exception {
        /*CertificateRequest csr = new CertificateRequest(pkcs10req);
        X509 phaosCaCert = new X509(caCert.getEncoded());
        com.phaos.crypto.PrivateKey phaosPrivateKey = new com.phaos.crypto.RSAPrivateKey(privateKey.getEncoded());

        byte[] serialBytes = new byte[8];
        random.nextBytes(serialBytes);
        BigInteger serial = new BigInteger(serialBytes);

        final X509 cert = new X509(csr, phaosCaCert, phaosPrivateKey, serial, new Date(System.currentTimeMillis()), new Date(expiration));
        return new X509CertImpl(cert.getEncoded());*/
        // TODO, plug in code to support this. i dont have access to PHAOS doc right now --fla
        throw new UnsupportedOperationException("this is not yet supported");
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
