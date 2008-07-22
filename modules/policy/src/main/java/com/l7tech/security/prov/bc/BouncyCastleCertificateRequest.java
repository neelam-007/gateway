/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.prov.bc;

import com.l7tech.security.prov.CertificateRequest;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import java.security.*;

/**
 * Encapsulate a BC-specific CSR.
 *
 * @author mike
 * @version 1.0
 */
public class BouncyCastleCertificateRequest implements CertificateRequest {
    public static final String REQUEST_SIG_ALG = "SHA1withRSA";

    private PKCS10CertificationRequest certReq;
    private String providerName;

    /**
     * Create a CSR object that uses BouncyCastle format to serialize the CSR, but creates crypto keys using
     * the specified JCE provider.
     *
     * @param certReq
     * @param providerName
     */

    public BouncyCastleCertificateRequest(PKCS10CertificationRequest certReq, String providerName) {
        this.certReq = certReq;
        this.providerName = providerName;
    }

    public PKCS10CertificationRequest getCertReq() {
        return certReq;
    }

    /**
     * @return a string like "cn=lyonsm"
     */
    public String getSubjectAsString() {
        return certReq.getCertificationRequestInfo().getSubject().toString();
    }

    /**
     * @return the bytes of the encoded form of this certificate request
     */
    public byte[] getEncoded() {
        return certReq.getEncoded();
    }

    /**
     * @return the public key in this certificate request
     */
    public PublicKey getPublicKey() throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        return certReq.getPublicKey(providerName);
    }

    /**
     * Create a new CSR using Bouncy Castle's X.509 classes, but using the specified JCE provider for crypto
     * operations.
     *
     * @param username
     * @param keyPair
     * @param providerName
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public static CertificateRequest makeCsr(String username, KeyPair keyPair, String providerName)
            throws InvalidKeyException, SignatureException
    {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        PKCS10CertificationRequest certReq = null;
        try {
            certReq = new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey, providerName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
        return new BouncyCastleCertificateRequest(certReq, providerName);
    }
}
