/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.ASN1Set;

import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.NoSuchAlgorithmException;

/**
 * Utilities for SSL support.
 * User: mike
 * Date: Jul 30, 2003
 * Time: 10:10:53 AM
 */
public class SslUtils {
    public static final String REQUEST_SIG_ALG = "SHA1withRSA";

    /**
     * Create a Certificate Signing Request.  The newly-created CSR will use the username
     * as the Common Name, and the SSG hostname as the Organizational Unit.
     *
     * @param username the Username, ie "joeblow"
     * @param ssgHostname the SSG hostname, ie "ssg.some-company.com"
     * @throws SignatureException
     * @throws InvalidKeyException
     * @throws RuntimeException if a needed algorithm or crypto provider was not found
     */
    public static PKCS10CertificationRequest makeCsr(String username,
                                               String ssgHostname,
                                               PublicKey publicKey,
                                               PrivateKey privateKey)
            throws SignatureException, InvalidKeyException, RuntimeException
    {
        X509Name subject = new X509Name("CN=" + username + ", OU=" + ssgHostname);
        ASN1Set attrs = null;

        // Generate request
        PKCS10CertificationRequest certReq = null;
        try {
            certReq = new PKCS10CertificationRequest(REQUEST_SIG_ALG, subject, publicKey, attrs, privateKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
        return certReq;
    }



}
