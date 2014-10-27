package com.l7tech.server.identity.cert;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.InvalidClientCertificateException;
import com.l7tech.identity.MissingCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.security.cert.CertValidationProcessor;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that knows how to do cert authentication inside the SSG for internal and LDAP users.
 */
public class CertificateAuthenticator {
    private static final Logger logger = Logger.getLogger(CertificateAuthenticator.class.getName());

    private final ClientCertManager clientCertManager;
    private final CertValidationProcessor certValidationProcessor;

    public CertificateAuthenticator(ClientCertManager clientCertManager, CertValidationProcessor processor) {
        this.clientCertManager = clientCertManager;
        this.certValidationProcessor = processor;
    }

    /**
     * @param validationType may be null, indicating that the {@link CertValidationProcessor} should use whatever type
     *        is the default for this facility
     */
    public AuthenticationResult authenticateX509Credentials(LoginCredentials pc, X509Certificate validCert,
                                                            User user, CertificateValidationType validationType,
                                                            Audit auditor, boolean internalCert)
            throws BadCredentialsException, MissingCredentialsException, InvalidClientCertificateException {
        CredentialFormat format = pc.getFormat();
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


        String userLabel = user.getLogin();
        if (userLabel == null || userLabel.trim().length() < 1) userLabel = user.getId();

        if (validCert == null) {
            String err = "No certificate found for user " + userLabel;
            logger.warning(err);
            throw new InvalidClientCertificateException(err);
        }

        logger.fine("Request cert serial# is " + requestCert.getSerialNumber().toString());
        if (CertUtils.certsAreEqual(requestCert, validCert)) {
            try {
                CertificateValidationResult cvr = certValidationProcessor.check(
                        new X509Certificate[]{requestCert},
                        null,
                        validationType,
                        CertValidationProcessor.Facility.IDENTITY,
                        auditor);
                if (cvr != CertificateValidationResult.OK) {
                    throw new InvalidClientCertificateException("Certificate [" +
                            CertUtils.getCertIdentifyingInformation(requestCert) + "] path validation and/or " +
                            "revocation checking failed");
                }
            } catch (GeneralSecurityException e) {
                throw new InvalidClientCertificateException("Certificate [" +
                        CertUtils.getCertIdentifyingInformation(requestCert) + "] validation failed:" +
                        ExceptionUtils.getMessage(e), e);
            }
            logger.finest(MessageFormat.format("Authenticated user {0} using a client certificate", userLabel));
            if (internalCert) {
                // remember that this cert was used at least once successfully
                try {
                    clientCertManager.forbidCertReset(user);
                    return new AuthenticationResult(user, pc.getSecurityTokens(), requestCert,
                                                    clientCertManager.isCertPossiblyStale(requestCert));
                } catch (ObjectModelException e) {
                    logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                    return null;
                }
            } else {
                return new AuthenticationResult(user, pc.getSecurityTokens(), requestCert, false);
            }
        } else {
            String err = "Failed to authenticate user " + userLabel + " using a client certificate [" +
                    CertUtils.getCertIdentifyingInformation(requestCert) + "] - request certificate doesn't match " +
                    "database certificate [" + CertUtils.getCertIdentifyingInformation(validCert) + "]";
            logger.warning(err);
            throw new InvalidClientCertificateException(err);
        }
    }

    /**
     * @param validationType may be null, indicating that the {@link CertValidationProcessor} should use whatever type
     *        is the default for this facility
     */
    public AuthenticationResult authenticateX509Credentials(LoginCredentials pc, User user, CertificateValidationType validationType, Audit auditor)
            throws BadCredentialsException, MissingCredentialsException, InvalidClientCertificateException
    {
        X509Certificate validCert;
        try {
            validCert = (X509Certificate)clientCertManager.getUserCert(user);
        } catch (FindException e) {
            logger.log(Level.SEVERE, "FindException exception looking for user cert", e);
            validCert = null;
        }
        return authenticateX509Credentials(pc, validCert, user, validationType, auditor, true);
    }
}
