/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.bc;

import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.RsaSignerEngine;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class BouncyCastleRsaSignerEngine implements RsaSignerEngine {

    static {
        // Make sure our provider is installed.  Probably unnecessary.
        JceProvider.init();
    }

    private PrivateKey privateKey;
    private X509Certificate caCert;
    X509Name caSubjectName;
    private static final SecureRandom random = new SecureRandom();

    //----------------------------------------

    //Location of CA keystore;
    private String keyStorePath;

    //Password for server keystore, comment out to prompt for pwd.;
    private String storePass;

    //Alias in keystore for CA private key;
    private String privateKeyAlias;

    //Password for CA private key, only used for JKS-keystore. Leave as null for PKCS12-keystore, comment out to prompt;
    private String privateKeyPassString;

    private final String keyStoreType;

    /**
     *  Constructor for the RsaCertificateSigner object sets all fields to their most common usage using
     * the passed keystore parameters to retreive the private key,
     */
    public BouncyCastleRsaSignerEngine( String keyStorePath,
                                        String storePass,
                                        String privateKeyAlias,
                                        String privateKeyPass,
                                        String keyStoreType ) {
        initDefaults();
        this.keyStorePath = keyStorePath;
        this.keyStoreType = keyStoreType;
        this.storePass = storePass;
        this.privateKeyAlias = privateKeyAlias;
        this.privateKeyPassString = privateKeyPass;
        try {
            initClass();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private static X509V3CertificateGenerator makeCertGenerator( X509Name subject, int validity,
                                                                 PublicKey publicKey, boolean isCa ) {
        Calendar cal = Calendar.getInstance();
        // Set back startdate ten minutes to avoid some problems with wrongly set clocks.
        cal.add(Calendar.MINUTE, -10);
        Date firstDate = cal.getTime();
        cal.add(Calendar.DATE, validity);
        Date lastDate = cal.getTime();

        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        // Serialnumber is random bits, where random generator is initialized with Date.getTime() when this
        // bean is created.
        byte[] serno = new byte[8];
        random.nextBytes(serno);
        certgen.setSerialNumber((new BigInteger(serno)).abs());
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(lastDate);
        certgen.setSignatureAlgorithm("SHA1WithRSA");

        certgen.setSubjectDN(subject);
        certgen.setPublicKey(publicKey);

        BasicConstraints bc = isCa ? new BasicConstraints(2) : new BasicConstraints(false);
        certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, bc);

        // Set key usage (signing only)
        int usage = 0;
        if ( isCa ) {
            usage |= X509KeyUsage.keyCertSign;
        } else {
            usage |= X509KeyUsage.digitalSignature;
            usage |= X509KeyUsage.keyEncipherment;
        }

        certgen.addExtension(X509Extensions.KeyUsage, true, new X509KeyUsage(usage));

        // Add subject public key info for fingerprint (not critical)
        SubjectPublicKeyInfo spki = null;
        try {
            spki = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(new DERInputStream(new ByteArrayInputStream(publicKey.getEncoded())).readObject()));
        } catch ( IOException e ) {
            throw new RuntimeException(e);
        }
        SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
        certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), false, ski);

        return certgen;
    }

    /**
     * Creates and signs a self-signed {@link X509Certificate}.
     * @param subjectDn The distinguished name of the certificate's subject (usually 'cn=hostname')
     * @param validity The period during which the cert should be valid, in days.
     * @param keypair The keypair belonging to the certificate's subject.
     * @return The self-signed {@link X509Certificate}.
     * @throws SignatureException if the certificate cannot be found
     * @throws InvalidKeyException if all or part of the keypair is invalid
     */
    public static X509Certificate makeSelfSignedRootCertificate( String subjectDn, int validity, KeyPair keypair )
            throws SignatureException, InvalidKeyException
    {
        X509Name subject = new X509Name(subjectDn);
        X509V3CertificateGenerator certgen = makeCertGenerator(subject, validity, keypair.getPublic(), true);

        // Self-signed, issuer == subject
        certgen.setIssuerDN(subject);

        X509Certificate cert = certgen.generateX509Certificate(keypair.getPrivate());
        return cert;
    }

    public static X509Certificate makeSignedCertificate( String subjectDn, int validity, PublicKey subjectPublicKey,
                                                   X509Certificate caCert, PrivateKey caKey)
            throws IOException, SignatureException, InvalidKeyException
    {
        X509Name subject = new X509Name(subjectDn);
        X509V3CertificateGenerator certgen = makeCertGenerator(subject, validity, subjectPublicKey, false );
        certgen.setIssuerDN(new X509Name(caCert.getSubjectDN().getName()));

        // Add authority key info (fingerprint)
        SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(new DERInputStream(new ByteArrayInputStream(caCert.getPublicKey().getEncoded())).readObject()));
        AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
        certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), false, aki);

        X509Certificate cert = certgen.generateX509Certificate(caKey);
        return cert;
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req) throws Exception {
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(pkcs10req);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();
        String dn= certReqInfo.getSubject().toString();
        logger.fine("Signing cert for subject DN = " + dn);
        if (pkcs10.verify() == false) {
            logger.severe("POPO verification failed for " + dn);
            throw new Exception("Verification of signature (popo) on PKCS10 request failed.");
        }

        X509Certificate cert = makeSignedCertificate(dn, CERT_DAYS_VALID, pkcs10.getPublicKey(), caCert, privateKey);
        // Verify before returning
        cert.verify(caCert.getPublicKey());
        return cert;
    }


    // makeBCCertificate

    private void initClass() throws Exception {
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
            throw new Exception("Cannot load key with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
        }
        Certificate[] certchain = keyStore.getCertificateChain(privateKeyAlias); // KeyTools.getCertChain(keyStore, privateKeyAlias);
        if (certchain.length < 1) {
            logger.severe("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
            throw new Exception("Cannot load certificate chain with alias '" + privateKeyAlias + "' from keystore '" + keyStorePath + "'");
        }
        // We only support a ca hierarchy with depth 2.
        caCert = (X509Certificate) certchain[0];
    }

    /**
     * Sets all the class variables to default values and assign default properties.
     */
    private void initDefaults() {
        //the passwords are not assigned here by default they stay null

        //file path to the keystore
        keyStorePath = DEFAULT_KEYSTORE_NAME;
        //the alias given to the private key in the keystore
        privateKeyAlias = DEFAULT_PRIVATE_KEY_ALIAS;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
