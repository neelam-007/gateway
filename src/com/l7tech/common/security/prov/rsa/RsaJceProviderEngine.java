/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.rsa;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.util.HexUtils;
import com.rsa.certj.cert.CertificateException;
import com.rsa.certj.cert.PKCS10CertRequest;
import com.rsa.certj.cert.X500Name;
import com.rsa.jsafe.JSAFE_PrivateKey;
import com.rsa.jsafe.JSAFE_PublicKey;
import com.rsa.jsafe.JSAFE_UnimplementedException;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;

/**
 *
 * @author mike
 * @version 1.0
 */
public class RsaJceProviderEngine implements JceProviderEngine {
    private static final Provider PROVIDER = new com.rsa.jsafe.provider.JsafeJCE();
    public static final String DEVICE = "Native/Java";

    public RsaJceProviderEngine() {
        Security.insertProviderAt(PROVIDER, 0);
    }

    private static class RsaCertificateRequest extends CertificateRequest {
        private final com.rsa.certj.cert.PKCS10CertRequest csr;

        private RsaCertificateRequest(com.rsa.certj.cert.PKCS10CertRequest csr) {
            this.csr = csr;
        }

        public String getSubjectAsString() {
            return csr.getSubjectName().toString();
        }

        public byte[] getEncoded() {
            byte[] certificateRequest = new byte[csr.getDERLen(0)];
            try {
                csr.getDEREncoding (certificateRequest, 0, 0);
            } catch (CertificateException e) {
                throw new RuntimeException("Unable to encode certificate request", e);
            }
            return certificateRequest;
        }

        public PublicKey getPublicKey() throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
            try {
                return new RsaRsaPublicKey(csr.getSubjectPublicKey(DEVICE));
            } catch (CertificateException e) {
                throw new RuntimeException("Unable to get certificate request public key", e);
            }
        }
    }

    private static class RsaRsaPublicKey implements RSAPublicKey {
        private JSAFE_PublicKey pk;

        private RsaRsaPublicKey(JSAFE_PublicKey pk) {
            this.pk = pk;
        }

        public String getAlgorithm() {
            return "RSA";
        }

        public String getFormat() {
            return "X.509";
        }

        public byte[] getEncoded() {
            try {
                byte[][] data = pk.getKeyData("RSAPublicKeyBER");
                return data[0];
            } catch (JSAFE_UnimplementedException e) {
                throw new RuntimeException("Unable to get public key as RSA subjectPublicKeyInfo", e);
            }
        }

        public BigInteger getModulus() {
            try {
                byte[][] data = pk.getKeyData("RSAPublicKey");
                return new BigInteger(1, data[0]);
            } catch (JSAFE_UnimplementedException e) {
                throw new RuntimeException("Unable to get public key modulus", e);
            }
        }

        public BigInteger getPublicExponent() {
            try {
                byte[][] data = pk.getKeyData("RSAPublicKey");
                return new BigInteger(1, data[1]);
            } catch (JSAFE_UnimplementedException e) {
                throw new RuntimeException("Unable to get public key public exponent", e);
            }
        }

        public String toString() {
            byte[][] data;
            try {
                data = pk.getKeyData("RSAPublicKey");
            } catch (JSAFE_UnimplementedException e) {
                throw new RuntimeException("Unable to get RSA public key data", e);
            }

            return "RsaJceProviderEngine RSA public key: \n" +
                    "  public exponent:\n" +
                    "    " + HexUtils.hexDump(data[1]) +
                    "\n  modulus:\n" +
                    "    " + HexUtils.hexDump(data[0]);
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
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        return new RsaRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass);
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider.
     *
     * @return
     */
    public KeyPair generateRsaKeyPair() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        }

        kpg.initialize(RSA_KEY_LENGTH);
        return kpg.genKeyPair();
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return
     */
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException
    {

        X500Name subject = null;
        try {
            subject = new X500Name("CN=" + username);
        } catch (com.rsa.certj.cert.NameException e) {
            throw new RuntimeException(e); // can happen if username is no good
        }

        PKCS10CertRequest csr = new PKCS10CertRequest();
        try {
            csr.setSubjectName(subject);
        } catch (CertificateException e) {
            throw new RuntimeException(e);  // might happen if username is no good
        }

        try {
            csr.setSubjectPublicKey(keyPair.getPublic().getEncoded(), 0);
        } catch (CertificateException e) {
            throw new RuntimeException(e); // might happen if BER encoding is no good
        }


        JSAFE_PrivateKey privateKey = null;
        try {
            privateKey = JSAFE_PrivateKey.getInstance(keyPair.getPrivate().getEncoded(), 0, "Native/Java");
            csr.signCertRequest("SHA1/RSA/PKCS1Block01Pad", DEVICE, privateKey, new SecureRandom());
        } catch (JSAFE_UnimplementedException e) {
            throw new RuntimeException(e); // can't heppen
        } catch (CertificateException e) {
            throw new CausedSignatureException(e); // shouldn't happen
        }

        return new RsaCertificateRequest(csr);
    }
}
