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

    private final PKCS10CertificationRequest certReq;
    private final PublicKey publicKey;

    /**
     * Create a CSR object that uses BouncyCastle format to serialize the CSR, but creates crypto keys using
     * the specified JCE provider.
     *
     * @param certReq
     * @param publicKey the public key for this req
     */

    public BouncyCastleCertificateRequest(PKCS10CertificationRequest certReq, PublicKey publicKey) {
        this.certReq = certReq;
        this.publicKey = publicKey;
    }

    public PKCS10CertificationRequest getCertReq() {
        return certReq;
    }

    /**
     * @return a string like "cn=lyonsm"
     */
    @Override
    public String getSubjectAsString() {
        return certReq.getCertificationRequestInfo().getSubject().toString();
    }

    /**
     * @return the bytes of the encoded form of this certificate request
     */
    @Override
    public byte[] getEncoded() {
        return certReq.getEncoded();
    }

    /**
     * @return the public key in this certificate request
     */
    @Override
    public PublicKey getPublicKey() throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        return publicKey;
    }

    /**
     * Create a new CSR using Bouncy Castle's X.509 classes, but using the specified JCE provider for crypto
     * operations.
     *
     * @param username      username to include as CN in DN of generated CSR.  Required.
     * @param keyPair       the client's key pair.  public key will be included in generated CSR, and private key will be used to sign it.
     * @param providerName  alternative provider to use for Signature, or null to use current best-preference.
     * @throws InvalidKeyException  if key type is incorrect for CSR signature algorithm (SHA1withRSA)
     * @throws SignatureException
     */
    public static CertificateRequest makeCsr(String username, KeyPair keyPair, String providerName)
            throws InvalidKeyException, SignatureException
    {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        try {
            // Generate request
            PKCS10CertificationRequest certReq =
                    new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey, providerName);
            return new BouncyCastleCertificateRequest(certReq, publicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
