package com.l7tech.common.security.prov.sun;

import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.common.security.prov.bc.BouncyCastleCertificateRequest;
import com.sun.crypto.provider.SunJCE;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

import org.bouncycastle.jce.provider.JDKKeyPairGenerator;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.ASN1Set;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
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
     * @return
     */
    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return new BouncyCastleRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType );
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

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/ECB/NoPadding", PROVIDER.getName());
    }

    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        PKCS10CertificationRequest certReq = null;
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
