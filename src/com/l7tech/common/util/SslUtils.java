/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Utilities for SSL support.
 * User: mike
 * Date: Jul 30, 2003
 * Time: 10:10:53 AM
 */
public class SslUtils {
    private static final Category log = Category.getInstance(SslUtils.class);
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
        log.info("Generating CSR for user=" + username + "  ssgHostname=" + ssgHostname);
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

    /**
     * Transmit a CSR to the SSG and download the newly-signed certificate.
     * @param url       The URL that we should post our CSR to, whose response will be assumed to be our certificate.
     * @param username
     * @param password
     * @param csr
     * @return
     * @throws IOException
     * @throws CertificateException
     */
    public static X509Certificate obtainClientCertificate(URL url, String username, char[] password,
                                                          PKCS10CertificationRequest csr)
            throws IOException, CertificateException, NoSuchAlgorithmException,
                   InvalidKeyException, NoSuchProviderException
    {
        log.info("Sending certificate request for DN:" + csr.getCertificationRequestInfo().getSubject());
        HttpClient hc = new HttpClient();
        hc.getState().setAuthenticationPreemptive(true);
        hc.getState().setCredentials(null, null,
                                     new UsernamePasswordCredentials(username,
                                                                     new String(password)));
        PostMethod post = new PostMethod(url.toExternalForm());
        ByteArrayInputStream bais = new ByteArrayInputStream(csr.getEncoded());
        post.setRequestBody(bais);
        post.setRequestHeader("Content-Type", "application/pkcs10");
        post.setRequestHeader("Content-Length", String.valueOf(csr.getEncoded().length));
        int result = hc.executeMethod(post);
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(post.getResponseBodyAsStream());

        log.info("Got back a certificate with DN:" + cert.getSubjectDN());

        // TODO: Verify using the CA public key for this SSG.
        if (!cert.getSubjectDN().getName().equals(csr.getCertificationRequestInfo().getSubject().toString()))
            throw new CertificateException("We got a certificate, but it's directory name didn't match what we asked for.");
        if (!cert.getPublicKey().equals(csr.getPublicKey()))
            throw new CertificateException("We got a certificate, but it certified the wrong public key.");

        log.info("Certificate appears to be OK");
        return cert;
    }
}
