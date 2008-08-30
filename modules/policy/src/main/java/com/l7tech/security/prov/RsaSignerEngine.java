/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.prov;

import java.security.cert.Certificate;

/**
 * Provides the guts of an RSA Signer, using the current underlying crypto engine.
 * @author mike
 * @version 1.0
 */
public interface RsaSignerEngine {
    int CERT_DAYS_VALID = 730; // how many days of validity the cert should have

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @param subject the subject for the cert, if null, use the subject contained in the csr
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    Certificate createCertificate(byte[] pkcs10req, String subject) throws Exception;

    /**
     * Same as other createCertificate except allows caller to request a specific expiration date
     * @param pkcs10req the PKCS10 certificate signing request, expressed in binary form.
     * @param expiration expiration of the cert (millis since era)
     * @param subject the subject for the cert, if null, use the subject contained in the csr
     * @return a signed X509 client certificate
     * @throws Exception if the certificate cannot be signed
     */
    Certificate createCertificate(byte[] pkcs10req, String subject, long expiration) throws Exception;

    public static class CertType {
        public static final CertType CA = new CertType("CA");
        public static final CertType SSL = new CertType("SSL");
        public static final CertType CLIENT = new CertType("Client");

        private CertType( String desc ) {
        }
    }
}
