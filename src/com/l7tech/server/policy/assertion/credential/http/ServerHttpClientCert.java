/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

/**
 * The server-side processing for HTTPS with client certificates.  Note that this
 * class is not a subclass of <code>ServerHttpCredentialSource</code> because it
 * works at a lower level without stuff like <code>WWW-Authenticate</code> and
 * <code>Authorization</code> headers.
 *
 * @author alex
 * @version $Revision$
 */
public class ServerHttpClientCert extends ServerCredentialSourceAssertion implements ServerAssertion {
    public ServerHttpClientCert( HttpClientCert data ) {
        super( data );
        _data = data;
    }

    public LoginCredentials findCredentials( Request request, Response response ) throws CredentialFinderException {
        Object param = request.getParameter( Request.PARAM_HTTP_X509CERT );
        X509Certificate clientCert = null;

        if ( param == null ) {
            String err = "No Client Certificate was present in the request.";
            logger.log( Level.WARNING, err );
            throw new CredentialFinderException( err, AssertionStatus.AUTH_REQUIRED );
        } else {
            Object cert = null;

            Object[] maybeCerts;
            if ( param instanceof Object[] ) {
                maybeCerts = (Object[])param;
                for (int i = 0; i < maybeCerts.length; i++) {
                    cert = maybeCerts[i];
                    if ( cert instanceof X509Certificate ) clientCert = (X509Certificate)cert;
                }
            } else if ( param instanceof X509Certificate ) {
                clientCert = (X509Certificate)param;
            } else {
                String msg = "Client Certificate " + param + " was of the wrong type (" + param.getClass().getName() + ")";
                logger.warning( msg );
                throw new CredentialFinderException( msg, AssertionStatus.AUTH_FAILED );
            }
        }

        try {
            clientCert.checkValidity();
        } catch (CertificateExpiredException cee) {
            logger.log( Level.WARNING, cee.toString(), cee );
            throw new CredentialFinderException( "Client Certificate has expired", cee, AssertionStatus.AUTH_FAILED );
        } catch (CertificateNotYetValidException cnyve ) {
            logger.log( Level.WARNING, cnyve.toString(), cnyve );
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
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new CredentialFinderException("cannot extract name from cert", e, AssertionStatus.AUTH_FAILED);
        }

        logger.fine("cert found for user " + certCN);

        return new LoginCredentials( certCN, null, CredentialFormat.CLIENTCERT, _data.getClass(), null, clientCert );
    }

    protected AssertionStatus checkCredentials(Request request, Response response) {
        // TODO: Do we care?
        return AssertionStatus.NONE;
    }

    protected void challenge(Request request, Response response) {
        // TODO
    }

    protected HttpClientCert _data;
}
