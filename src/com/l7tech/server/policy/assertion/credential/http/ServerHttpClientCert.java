/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static final String PARAM_HTTP_X509CERT = "javax.servlet.request.X509Certificate";

    public ServerHttpClientCert( HttpClientCert data ) {
        super( data );
        _data = data;
    }

    protected LoginCredentials findCredentials(Message request, Map authParams)
            throws IOException, CredentialFinderException
    {
        HttpRequestKnob httpReq = (HttpRequestKnob)request.getKnob(HttpRequestKnob.class);
        if (httpReq == null) {
            logger.info("Request not received over HTTP; cannot check for client certificate");
            return null;
        }

        X509Certificate[] certChain = httpReq.getClientCertificate();

        if ( certChain == null || certChain.length < 1 ) {
            String err = "No Client Certificate was present in the request.";
            logger.log( Level.WARNING, err );
            throw new CredentialFinderException( err, AssertionStatus.AUTH_REQUIRED );
        }

        X509Certificate clientCert = certChain[0];
        if (clientCert == null) {
            logger.log( Level.WARNING, "Cert chain contained null certificate -- ignoring" );
            return null;
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
        // String certCN = getCachedClientCert.getSubjectDN().getName();
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

        // TODO where's the chain?
        return new LoginCredentials( certCN, null, CredentialFormat.CLIENTCERT, _data.getClass(), null, clientCert );
    }

    protected HttpClientCert _data;

    protected AssertionStatus checkCredentials(LoginCredentials pc, Map authParams) throws CredentialFinderException {
        return AssertionStatus.NONE;
    }

    protected void challenge(PolicyEnforcementContext context, Map authParams) {
        // HOW DO I CHALLENGED X.509
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());    
}
