/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.prov.bc;

import com.l7tech.common.security.CertificateRequest;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;

/**
 * Encapsulate a BC-specific CSR.
 *
 * @author mike
 * @version 1.0
 */
public class BouncyCastleCertificateRequest implements CertificateRequest {
    private PKCS10CertificationRequest certReq;
    private String providerName;

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
}
