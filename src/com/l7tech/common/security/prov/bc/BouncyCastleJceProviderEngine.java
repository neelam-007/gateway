/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.bc;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;

import java.security.*;

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

    /**
     * Create an RsaSignerEngine that uses the current crypto API.
     *
     * @param keyStorePath
     * @param storePass
     * @param privateKeyAlias
     * @param privateKeyPass
     * @return
     */
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        return new BouncyCastleRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, KEYSTORE_TYPE, PROVIDER.getName(), REQUEST_SIG_ALG );
    }

    /**
     * Generate an RSA public key / private key pair.
     * @return
     */
    public KeyPair generateRsaKeyPair() {
        JDKKeyPairGenerator.RSA kpg = new JDKKeyPairGenerator.RSA();
        kpg.initialize(RSA_KEY_LENGTH);
        return kpg.generateKeyPair();
    }

    public static final String REQUEST_SIG_ALG = "SHA1withRSA";

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @return
     */
    public CertificateRequest makeCsr( String username, KeyPair keyPair ) throws SignatureException, InvalidKeyException {
        return staticMakeCsr( username, keyPair, PROVIDER.getName() );
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @return
     */
    public static CertificateRequest staticMakeCsr(String username, KeyPair keyPair, String providerName ) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        PKCS10CertificationRequest certReq = null;
        try {
            certReq = new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
        return new BouncyCastleCertificateRequest(certReq, providerName);
    }

    private static final String KEYSTORE_TYPE = "JKS";
}
