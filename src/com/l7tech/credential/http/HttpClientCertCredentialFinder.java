/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.http;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.message.Request;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.identity.User;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateEncodingException;

/**
 * @author alex
 */
public class HttpClientCertCredentialFinder extends HttpCredentialFinder {
    public PrincipalCredentials findCredentials(Request request) throws CredentialFinderException {
        Object maybeClientCert = request.getParameter( Request.PARAM_HTTP_X509CERT );
        X509Certificate clientCert = null;

        if ( maybeClientCert == null) {
            throw new CredentialFinderException( "No Client Certificate was present in the request.", AssertionStatus.AUTH_REQUIRED );
        } else {
            if ( maybeClientCert instanceof X509Certificate )
                clientCert = (X509Certificate)maybeClientCert;
            if ( clientCert == null ) {
                throw new CredentialFinderException( "Client Certificate was of the wrong type", AssertionStatus.AUTH_FAILED );
            }
        }

        try {
            clientCert.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new CredentialFinderException( "Client Certificate has expired", e, AssertionStatus.AUTH_FAILED );
        } catch (CertificateNotYetValidException e) {
            throw new CredentialFinderException( "Client Certificate is not yet valid", e, AssertionStatus.AUTH_FAILED );
        }

        // Get DN from cert, ie "CN=testuser, OU=ssg.example.com"
        String certDn = clientCert.getSubjectDN().getName();
        try {
            User u = new User();
            u.setLogin( certDn );
            PrincipalCredentials pc = new PrincipalCredentials( u, clientCert.getEncoded(), CredentialFormat.CLIENTCERT );
            return pc;
        } catch (CertificateEncodingException e) {
            throw new CredentialFinderException( "Client certificate could not be properly encoded", e, AssertionStatus.AUTH_FAILED );
        }
    }
}
