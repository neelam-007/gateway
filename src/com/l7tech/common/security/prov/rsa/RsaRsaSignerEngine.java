/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.rsa;

import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.logging.LogManager;
import com.rsa.certj.cert.PKCS10CertRequest;
import com.rsa.certj.cert.X500Name;
import com.rsa.jsafe.JSAFE_PrivateKey;
import sun.security.x509.X509CertImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class RsaRsaSignerEngine implements RsaSignerEngine {
    private String keyStorePath;
    private String storePass;
    private String privateKeyPassString;
    private String privateKeyAlias;
    private PrivateKey privateKey;
    private X509Certificate caCert;
    private SecureRandom random;

    public RsaRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        this.keyStorePath = keyStorePath;
        this.storePass = storePass;
        this.privateKeyAlias = privateKeyAlias;
        this.privateKeyPassString = privateKeyPass;

        try {
            init();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void init() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
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

        // Use this random ONLY for serial numbers
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
    public Certificate createCertificate(byte[] pkcs10req) throws Exception {
        PKCS10CertRequest csr = new PKCS10CertRequest(pkcs10req, 0, 0);
        JSAFE_PrivateKey caKey = JSAFE_PrivateKey.getInstance(privateKey.getEncoded(), 0, "Native/Java");
        com.rsa.certj.cert.X509Certificate cert = new com.rsa.certj.cert.X509Certificate();
        cert.setVersion(com.rsa.certj.cert.X509Certificate.X509_VERSION_3);

        byte[] serialBytes = new byte[8];
        random.nextBytes(serialBytes);
        cert.setSerialNumber(serialBytes, 0, serialBytes.length);

        GregorianCalendar cal = new GregorianCalendar();
        Date notBefore = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH,  CERT_DAYS_VALID);
        Date notAfter = cal.getTime();
        cert.setValidity(notBefore, notAfter);

        cert.setIssuerName(new X500Name(caCert.getSubjectDN().getName().toString()));
        cert.setSubjectName(csr.getSubjectName());
        cert.setSubjectPublicKey(csr.getSubjectPublicKey("Native/Java"));

        cert.signCertificate ("SHA1/RSA/PKCS1Block01Pad", "Native/Java", caKey, new SecureRandom());
        byte[] encoded = new byte[cert.getDERLen(0)];
        cert.getDEREncoding(encoded, 0, encoded.length);
        return new X509CertImpl(encoded);
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
}
