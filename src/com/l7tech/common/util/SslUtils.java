/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.security.CertificateRequest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

    /** Thrown when the Certificate Signer doesn't like our username or password. */
    public static class BadCredentialsException extends Exception {
        public BadCredentialsException() {
        }

        public BadCredentialsException(String message) {
            super(message);
        }

        public BadCredentialsException(String message, Throwable cause) {
            super(message, cause);
        }

        public BadCredentialsException(Throwable cause) {
            super(cause);
        }
    }

    /** Thrown when the Certificate Signer says it gave us a certificate already. */
    public static class CertificateAlreadyIssuedException extends Exception {
        public CertificateAlreadyIssuedException() {
        }

        public CertificateAlreadyIssuedException(String message) {
            super(message);
        }

        public CertificateAlreadyIssuedException(String message, Throwable cause) {
            super(message, cause);
        }

        public CertificateAlreadyIssuedException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Transmit a CSR to the SSG and download the newly-signed certificate.
     * Afterward we'll verify that the signed cert is the one we asked for, and was signed by the CA we expect.
     *
     * @param url       The URL that we should post our CSR to, whose response will be assumed to be our certificate.
     * @param username
     * @param password
     * @param csr
     * @param caCert  the cert of the CA that is supposed to be signing the request
     * @return
     * @throws IOException                  if there is a network problem talking to the CSR signing service
     * @throws CertificateException         if the post to the CSR signer results in a status other than 200 (or 401)
     * @throws NoSuchAlgorithmException     if one of the keys uses an algorithm that isn't installed
     * @throws InvalidKeyException          if one of the keys is invalid
     * @throws NoSuchProviderException      if no X.509 cert provider is installed (can't happen)
     * @throws SignatureException           if the resulting cert was not signed by the correct CA key
     * @throws SslUtils.BadCredentialsException if the username or password was rejected by the CSR signer
     */
    public static X509Certificate obtainClientCertificate(URL url, String username, char[] password,
                                                          CertificateRequest csr,
                                                          X509Certificate caCert)
            throws IOException, CertificateException, NoSuchAlgorithmException,
                   InvalidKeyException, NoSuchProviderException, SslUtils.BadCredentialsException,
                   SignatureException, CertificateAlreadyIssuedException
    {
        X500Principal csrName = new X500Principal(csr.getSubjectAsString());
        String csrNameString = csrName.getName(X500Principal.RFC2253);
        HttpClient hc = new HttpClient();
        hc.getState().setAuthenticationPreemptive(true);
        hc.getState().setCredentials(null, null,
                                     new UsernamePasswordCredentials(username,
                                                                     new String(password)));
        PostMethod post = new PostMethod(url.toExternalForm());
        try {
            byte[] csrBytes = csr.getEncoded();
            ByteArrayInputStream bais = new ByteArrayInputStream(csrBytes);
            post.setRequestBody(bais);
            post.setRequestHeader("Content-Type", "application/pkcs10");
            post.setRequestHeader("Content-Length", String.valueOf(csrBytes.length));

            int result = hc.executeMethod(post);
            if ( result == 401 ) throw new BadCredentialsException("HTTP POST to certificate signer returned status " + result );
            if ( result == 403 ) throw new CertificateAlreadyIssuedException("HTTP POST to certificate signer returned status " + result);
            if ( result != 200 ) throw new CertificateException( "HTTP POST to certificate signer generated status " + result );
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(post.getResponseBodyAsStream());
            post.releaseConnection();
            post = null;
            X500Principal certName = new X500Principal(cert.getSubjectDN().toString());
            String certNameString = certName.getName(X500Principal.RFC2253);

            if (!certNameString.equals(csrNameString))
                throw new CertificateException("We got a certificate, but it's distinguished name didn't match what we asked for.");
            if (!cert.getPublicKey().equals(csr.getPublicKey()))
                throw new CertificateException("We got a certificate, but it certified the wrong public key.");
            cert.verify(caCert.getPublicKey());

            return cert;
        } finally {
            if (post != null)
                post.releaseConnection();
        }
    }
}
