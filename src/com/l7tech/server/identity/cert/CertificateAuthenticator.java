/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.identity.cert;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that knows how to do cert authentication inside the SSG for internal and LDAP users.
 */
public class CertificateAuthenticator {
    private static final Logger logger = Logger.getLogger(CertificateAuthenticator.class.getName());
    private final ClientCertManager clientCertManager;

    public CertificateAuthenticator(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public AuthenticationResult authenticateX509Credentials(LoginCredentials pc, User user)
            throws BadCredentialsException, MissingCredentialsException, InvalidClientCertificateException
    {
        CredentialFormat format = pc.getFormat();
        X509Certificate dbCert;
        X509Certificate requestCert = null;
        Object payload = pc.getPayload();

        // TODO: This is a bit ugly.  Use security tokens instead
        if (format.isClientCert()) {
            // get the cert from the credentials
            requestCert = (X509Certificate)payload;
        } else if (format == CredentialFormat.SAML) {
            if (payload instanceof SamlAssertion) {
                SamlAssertion assertion = (SamlAssertion)payload;
                requestCert = assertion.getSubjectCertificate();
            } else {
                throw new BadCredentialsException("Unsupported SAML Assertion type: " +
                        payload.getClass().getName());
            }
        }

        if (requestCert == null) {
            String err = "Request was supposed to contain a certificate, but does not";
            logger.severe(err);
            throw new MissingCredentialsException(err);
        }

        try {
            dbCert = (X509Certificate)clientCertManager.getUserCert(user);
        } catch (FindException e) {
            logger.log(Level.SEVERE, "FindException exception looking for user cert", e);
            dbCert = null;
        }

        String userLabel = user.getLogin();
        if (userLabel == null || userLabel.trim().length() < 1) userLabel = user.getId();

        if (dbCert == null) {
            String err = "No certificate found for user " + userLabel;
            logger.warning(err);
            throw new InvalidClientCertificateException(err);
        }

        logger.fine("Request cert serial# is " + requestCert.getSerialNumber().toString());
        if (CertUtils.certsAreEqual(requestCert, dbCert)) {
            logger.finest("Authenticated user " + userLabel + " using a client certificate");
            // remember that this cert was used at least once successfully
            try {
                clientCertManager.forbidCertReset(user);
                return new AuthenticationResult(user, requestCert,
                                                clientCertManager.isCertPossiblyStale(requestCert));
            } catch (ObjectModelException e) {
                logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                return null;
            }
        } else {
            String err = "Failed to authenticate user " + userLabel + " using a client certificate " +
                    "(request certificate doesn't match database's)";
            logger.warning(err);
            throw new InvalidClientCertificateException(err);
        }
    }
}
