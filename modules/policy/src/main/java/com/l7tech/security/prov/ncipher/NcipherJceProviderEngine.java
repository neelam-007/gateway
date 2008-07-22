/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.ncipher;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProviderEngine;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.ncipher.provider.km.KMRSAKeyPairGenerator;
import com.ncipher.provider.km.nCipherKM;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 *
 * @author mike
 * @version 1.0
 */
public class NcipherJceProviderEngine implements JceProviderEngine {
    static final Provider PROVIDER = new nCipherKM();

    public NcipherJceProviderEngine() {
        Security.insertProviderAt(bouncyCastleProvider, 0);
        Security.insertProviderAt(PROVIDER, 0);
    }

    /**
     * Get the Provider.
     * @return the JCE Provider
     */
    public Provider getAsymmetricProvider() {
        return PROVIDER;
    }

    public Provider getSymmetricProvider() {
        return bouncyCastleProvider;
    }

    /**
     * Create an RsaSignerEngine that uses the current crypto API.
     *
     * @param keyStorePath
     * @param storePass
     * @param privateKeyAlias
     * @param privateKeyPass
     */
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return new BouncyCastleRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType, PROVIDER.getName());
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     */
    public KeyPair generateRsaKeyPair(int len) {
        KMRSAKeyPairGenerator kpg = new KMRSAKeyPairGenerator();
        try {
            // TODO can the Random be null or is there some other algorithm we should be using?
            // TODO can the Random be null or is there some other algorithm we should be using?
            // TODO can the Random be null or is there some other algorithm we should be using?
            // TODO can the Random be null or is there some other algorithm we should be using?
            kpg.initialize(len, SecureRandom.getInstance("SHA1PRNG", PROVIDER));
            // TODO can the Random be null or is there some other algorithm we should be using?
            // TODO can the Random be null or is there some other algorithm we should be using?
            // TODO can the Random be null or is there some other algorithm we should be using?
            // TODO can the Random be null or is there some other algorithm we should be using?
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Can't happen
        }
        return kpg.generateKeyPair();
    }

    public KeyPair generateRsaKeyPair() {
        return generateRsaKeyPair(RSA_KEY_LENGTH);
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     */
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        return BouncyCastleCertificateRequest.makeCsr(username, keyPair, PROVIDER.getName());
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

    private BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
}