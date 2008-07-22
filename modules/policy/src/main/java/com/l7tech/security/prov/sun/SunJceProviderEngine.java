package com.l7tech.security.prov.sun;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProviderEngine;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.sun.crypto.provider.SunJCE;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
public class SunJceProviderEngine implements JceProviderEngine {
    private final Provider PROVIDER = new SunJCE();

    public SunJceProviderEngine() {
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
     */
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return new BouncyCastleRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType, PROVIDER.getName() );
    }

    /**
     * Generate an RSA public key / private key pair.
     */
    public KeyPair generateRsaKeyPair(int len) {
        JDKKeyPairGenerator.RSA kpg = new JDKKeyPairGenerator.RSA();
        kpg.initialize(len);
        return kpg.generateKeyPair();
    }

    public KeyPair generateRsaKeyPair() {
        return generateRsaKeyPair(RSA_KEY_LENGTH);
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/ECB/NoPadding", PROVIDER.getName());
    }

    public Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/ECB/OAEPPadding", PROVIDER.getName());
    }

    public Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/ECB/PKCS1Padding", PROVIDER.getName());
    }

    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        PKCS10CertificationRequest certReq;
        try {
            certReq = new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey, "BC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
        return new BouncyCastleCertificateRequest(certReq, PROVIDER.getName());
    }

    public static final String REQUEST_SIG_ALG = "SHA1withRSA";

}