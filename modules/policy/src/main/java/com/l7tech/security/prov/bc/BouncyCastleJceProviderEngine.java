/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.bc;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProviderEngine;
import com.l7tech.security.prov.RsaSignerEngine;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * BouncyCastle-specific JCE provider engine.
 * @author mike
 * @version 1.0
 */
public class BouncyCastleJceProviderEngine implements JceProviderEngine {
    private final Provider PROVIDER = new BouncyCastleProvider();

    public BouncyCastleJceProviderEngine() {
        Security.addProvider(PROVIDER);
    }

    /**
     * Get the asymmetric crypto {@link Provider}.
     * @return the JCE Provider
     */
    public Provider getAsymmetricProvider() {
        return PROVIDER;
    }

    /**
     * Get the symmetric crypto {@link Provider}.
     * @return the JCE Provider
     */
    public Provider getSymmetricProvider() {
        return PROVIDER;
    }

    public RsaSignerEngine createRsaSignerEngine(PrivateKey caKey, X509Certificate[] caCertChain) {
        return new BouncyCastleRsaSignerEngine(caKey, caCertChain[0], "BC" );
    }

    public KeyPair generateRsaKeyPair(int len) {
        JDKKeyPairGenerator.RSA kpg = new JDKKeyPairGenerator.RSA();
        kpg.initialize(len);
        return kpg.generateKeyPair();
    }

    public static final String REQUEST_SIG_ALG = "SHA1withRSA";

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     */
    public CertificateRequest makeCsr( String username, KeyPair keyPair ) throws SignatureException, InvalidKeyException {
        return staticMakeCsr( username, keyPair, PROVIDER.getName() );
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/NoPadding", PROVIDER.getName());
    }

    public Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/OAEPPadding", PROVIDER.getName());
    }

    public Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/PKCS1Padding", PROVIDER.getName());
    }

    /**
     * Generate a CertificateRequest using the specified Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @param providerName name of the provider to use for crypto operations.
     * @return a new CertificateRequest instance.  Never null.
     * @throws java.security.InvalidKeyException  if a CSR cannot be created using the specified keypair
     * @throws java.security.SignatureException   if the CSR cannot be signed
     */
    public static CertificateRequest staticMakeCsr(String username, KeyPair keyPair, String providerName ) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        try {
            PKCS10CertificationRequest certReq = new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey);
            return new BouncyCastleCertificateRequest(certReq, providerName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
    }
}