/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.identity.User;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.logging.Level;
import java.util.Map;
import java.util.Collections;
import java.io.IOException;

import sun.security.x509.X500Name;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpClientCert extends ServerHttpCredentialSource implements ServerAssertion {
    public ServerHttpClientCert( HttpClientCert data ) {
        super( data );
        _data = data;
    }

    public PrincipalCredentials doFindCredentials( Request request, Response response ) throws CredentialFinderException {
        Object param = request.getParameter( Request.PARAM_HTTP_X509CERT );
        X509Certificate clientCert = null;

        if ( param == null ) {
            String err = "No Client Certificate was present in the request.";
            _log.log( Level.WARNING, err );
            throw new CredentialFinderException( err, AssertionStatus.AUTH_REQUIRED );
        } else {
            Object cert = null;
            Object[] maybeCerts;
            try {
                maybeCerts = (Object[])param;
                for (int i = 0; i < maybeCerts.length; i++) {
                    cert = maybeCerts[i];
                    if ( cert instanceof X509Certificate ) clientCert = (X509Certificate)cert;
                }
            } catch ( ClassCastException cce ) {
                _log.log( Level.WARNING, cce.toString(), cce );
                throw new CredentialFinderException( "Client Certificate " + cert + " was of the wrong type (" + cert.getClass().getName() + ")", AssertionStatus.AUTH_FAILED );
            }
        }

        try {
            clientCert.checkValidity();
        } catch (CertificateExpiredException cee) {
            _log.log( Level.WARNING, cee.toString(), cee );
            throw new CredentialFinderException( "Client Certificate has expired", cee, AssertionStatus.AUTH_FAILED );
        } catch (CertificateNotYetValidException cnyve ) {
            _log.log( Level.WARNING, cnyve.toString(), cnyve );
            throw new CredentialFinderException( "Client Certificate is not yet valid", cnyve, AssertionStatus.AUTH_FAILED );
        }

        // Get DN from cert, ie "CN=testuser, OU=ssg.example.com"
        // String certCN = clientCert.getSubjectDN().getName();
        // fla changed this to:
        String certCN = null;
        try {
            X500Name x500name = new X500Name( clientCert.getSubjectX500Principal().getName() );
            certCN = x500name.getCommonName();
        } catch (IOException e) {
            _log.log(Level.SEVERE, e.getMessage(), e);
            throw new CredentialFinderException("cannot extract name from cert", e, AssertionStatus.AUTH_FAILED);
        }

        _log.log(Level.INFO, "cert found for user " + certCN);

        User u = new User();
        u.setLogin(certCN);
        return new PrincipalCredentials( u, null, CredentialFormat.CLIENTCERT, null, clientCert );
    }

    protected AssertionStatus doCheckCredentials(Request request, Response response) {
        // TODO: Do we care?
        return AssertionStatus.NONE;
    }

    protected Map challengeParams(Request request, Response response) {
        return Collections.EMPTY_MAP;
    }

    protected String scheme() {
        return SCHEME;
    }

    protected HttpClientCert _data;
    protected static final String SCHEME = "ClientCert";
}
