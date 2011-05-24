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
}
