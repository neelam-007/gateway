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
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.*;
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

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Create a Certificate Signing Request.  The newly-created CSR will use the username
     * as the Common Name, and the SSG hostname as the Organizational Unit.
     *
     * @param username the Username, ie "joeblow"
     * @throws SignatureException
     * @throws InvalidKeyException
     * @throws RuntimeException if a needed algorithm or crypto provider was not found
     */
    public static PKCS10CertificationRequest makeCsr(String username,
                                               PublicKey publicKey,
                                               PrivateKey privateKey)
            throws SignatureException, InvalidKeyException, RuntimeException
    {
        log.info("Generating CSR for user=" + username);
        X509Name subject = new X509Name("cn=" + username);
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
        X500Principal csrName = new X500Principal(csr.getCertificationRequestInfo().getSubject().toString());
        String csrNameString = csrName.getName(X500Principal.CANONICAL);
        log.info("Sending certificate request to " + url + " for DN:" + csrNameString);
        HttpClient hc = new HttpClient();
        hc.getState().setAuthenticationPreemptive(true);
        hc.getState().setCredentials(null, null,
                                     new UsernamePasswordCredentials(username,
                                                                     new String(password)));
        PostMethod post = new PostMethod(url.toExternalForm());
        byte[] csrBytes = csr.getEncoded();
        ByteArrayInputStream bais = new ByteArrayInputStream(csrBytes);
        post.setRequestBody(bais);
        post.setRequestHeader("Content-Type", "application/pkcs10");
        post.setRequestHeader("Content-Length", String.valueOf(csrBytes.length));
        int result = hc.executeMethod(post);
        log.info("Post of CSR completed with status " + result);
        if ( result != 200 ) throw new CertificateException( "HTTP POST to certificate signer generated status " + result );
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(post.getResponseBodyAsStream());
        X500Principal certName = new X500Principal(cert.getSubjectDN().toString());
        String certNameString = certName.getName(X500Principal.CANONICAL);

        log.info("Got back a certificate with DN:" + certNameString);

        // TODO: Verify using the CA public key for this SSG.
        if (!certNameString.equals(csrNameString))
            throw new CertificateException("We got a certificate, but it's directory name didn't match what we asked for.");
        if (!cert.getPublicKey().equals(csr.getPublicKey()))
            throw new CertificateException("We got a certificate, but it certified the wrong public key.");

        log.info("Certificate appears to be OK");
        return cert;
    }
}
