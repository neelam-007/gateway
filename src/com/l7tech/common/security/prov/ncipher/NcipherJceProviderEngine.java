/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.ncipher;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.security.prov.bc.BouncyCastleCertificateRequest;
import com.ncipher.provider.km.KMRSAKeyPairGenerator;
import com.ncipher.provider.km.nCipherKM;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
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
    public static final String REQUEST_SIG_ALG = "SHA1withRSA";

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
     * @return
     */
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return new NcipherRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType);
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     *
     * @return
     */
    public KeyPair generateRsaKeyPair() {
        KMRSAKeyPairGenerator kpg = new KMRSAKeyPairGenerator();
        return kpg.generateKeyPair();
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return
     */
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        PKCS10CertificationRequest certReq = null;
        try {
            certReq = new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey, PROVIDER.getName());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
        return new BouncyCastleCertificateRequest(certReq, PROVIDER.getName());
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/NoPadding", PROVIDER.getName());
    }

    private BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
}
