/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.phaos;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;
import com.phaos.cert.PKIX;
import com.phaos.cert.X500Name;
import com.phaos.crypto.AlgID;
import com.phaos.crypto.KeyPairGenerator;
import com.phaos.crypto.RSAKeyPairGenerator;
import com.phaos.crypto.RandomBitsSource;
import com.phaos.jce.provider.Phaos;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 *
 * @author mike
 * @version 1.0
 */
public class PhaosJceProviderEngine implements JceProviderEngine {
    private static final Provider PROVIDER = new Phaos();

    public PhaosJceProviderEngine() {
        Security.insertProviderAt(PROVIDER, 0);
    }

    private static class PhaosCertificateRequest implements CertificateRequest {
        private final com.phaos.cert.CertificateRequest csr;

        private PhaosCertificateRequest(com.phaos.cert.CertificateRequest csr) {
            this.csr = csr;
        }

        public String getSubjectAsString() {
            return csr.getSubject().toString();
        }

        public byte[] getEncoded() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            try {
                csr.output(baos);
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
            return baos.toByteArray();
        }

        public PublicKey getPublicKey() throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
            return new PhaosRsaPublicKey((com.phaos.crypto.RSAPublicKey) csr.getPublicKey());
        }
    }

    private static class PhaosRsaPublicKey implements RSAPublicKey {
        private com.phaos.crypto.RSAPublicKey pk;

        private PhaosRsaPublicKey(com.phaos.crypto.RSAPublicKey pk) {
            this.pk = pk;
        }

        private com.phaos.crypto.RSAPublicKey toPhaos() {
            return pk;
        }

        public String getAlgorithm() {
            return pk.getAlgorithm();
        }

        public String getFormat() {
            return pk.getFormat();
        }

        public byte[] getEncoded() {
            return pk.getEncoded();
        }

        public BigInteger getModulus() {
            return pk.getModulus();
        }

        public BigInteger getPublicExponent() {
            return pk.getExponent();
        }
    }

    private static class PhaosRsaPrivateKey implements RSAPrivateKey {
        private com.phaos.crypto.RSAPrivateKey pk;

        private PhaosRsaPrivateKey(com.phaos.crypto.RSAPrivateKey pk) {
            this.pk = pk;
        }

        private com.phaos.crypto.RSAPrivateKey toPhaos() {
            return pk;
        }

        public String getAlgorithm() {
            return pk.getAlgorithm();
        }

        public String getFormat() {
            return pk.getFormat();
        }

        public byte[] getEncoded() {
            return pk.getEncoded();
        }

        public BigInteger getModulus() {
            return pk.getModulus();
        }

        public BigInteger getPrivateExponent() {
            return pk.getExponent();
        }
    }

    private static class CausedSignatureException extends SignatureException {
        CausedSignatureException(Throwable t) {
            super();
            initCause(t);
        }
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
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return new PhaosRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType);
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     *
     * @return
     */
    public KeyPair generateRsaKeyPair() {
        KeyPairGenerator kpg = RSAKeyPairGenerator.getInstance(AlgID.rsaEncryption);
        kpg.initialize(RSA_KEY_LENGTH, RandomBitsSource.getDefault());
        com.phaos.crypto.KeyPair kp = kpg.generateKeyPair();
        return new KeyPair(new PhaosRsaPublicKey((com.phaos.crypto.RSAPublicKey)kp.getPublic()),
                           new PhaosRsaPrivateKey((com.phaos.crypto.RSAPrivateKey)kp.getPrivate()));
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return
     */
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        X500Name subject = new X500Name();
        subject.addComponent(PKIX.id_at_commonName, username);
        com.phaos.crypto.RSAPublicKey publicKey = ((PhaosRsaPublicKey) keyPair.getPublic()).toPhaos();
        com.phaos.crypto.RSAPrivateKey privateKey = ((PhaosRsaPrivateKey) keyPair.getPrivate()).toPhaos();
        com.phaos.crypto.KeyPair phaosKeyPair = new com.phaos.crypto.KeyPair(publicKey, privateKey);

        // Generate request
        com.phaos.cert.CertificateRequest csr = new com.phaos.cert.CertificateRequest(subject, phaosKeyPair);
        try {
            csr.sign();
        } catch (com.phaos.crypto.SignatureException e) {
            throw new CausedSignatureException(e);
        }
        return new PhaosCertificateRequest(csr);
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/NoPadding", PROVIDER.getName());
    }
}
