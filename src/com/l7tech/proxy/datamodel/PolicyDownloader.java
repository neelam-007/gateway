package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.util.PolicyServiceClient;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * A class that specializes in downloading policies from an Ssg.
 * TODO move all request-specific code out of this class.  There should be one PolicyDownloader instance per Ssg,
 * rather than one created per policy download attempt.
 */
public class PolicyDownloader {
    private static final Logger log = Logger.getLogger(PolicyDownloader.class.getName());

    private final PolicyApplicationContext request;
    private final Ssg ssg;

    public PolicyDownloader(PolicyApplicationContext context) {
        if (context == null) throw new NullPointerException("context is null");
        this.request = context;
        Ssg ssg = context.getSsg();
        if (ssg == null) throw new NullPointerException("context.ssg is null");
        this.ssg = ssg;
    }

    /**
     * Notify the PolicyManager that a policy may be out-of-date and should be (re)loaded from the PolicyManager's
     * underlying policy source.
     *
     * @param serviceId The ID of the service for which to load the policy.
     * @throws IOException if the policy could not be read from the SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException if an SSL handshake with the SSG could not be established due to
     *                                             the SSG's SSL certificate being unrecognized
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if credentials were required, but the user canceled the logon dialog
     */
    public Policy downloadPolicy(PolicyAttachmentKey pak, String serviceId)
            throws IOException, GeneralSecurityException,
                   OperationCanceledException, HttpChallengeRequiredException, KeyStoreCorruptException,
                   ClientCertificateException, PolicyRetryableException, ConfigurationException
    {
        final boolean useSsl = ssg.isUseSslByDefault();
        final X509Certificate serverCert = ssg.getServerCertificate();
        if (serverCert == null)
            throw new ServerCertificateUntrustedException("Server certificate not yet known");

        // Try anonymous download first
        try {
            if (useSsl && ssg.getClientCertificate() != null)
                log.info("Trying SSL-with-client-cert policy download from " + ssg);
            else
                log.info("Trying anonymous policy download from " + ssg);
            Policy policy = PolicyServiceClient.downloadPolicyWithNoAuthentication(ssg, serviceId, serverCert, useSsl);
            return policy;
        } catch (BadCredentialsException e) {
            // FALLTHROUGH and try again using credentials
            log.info("Policy service declined our anonymous download; will try again using credentials");
        } catch (InvalidDocumentFormatException e) {
            throw new CausedIOException("Unable to download new policy", e);
        }

        for (int attempts = 0; attempts < 10; ++attempts) {
            // Anonymous download failed; need to try again with credentials.
            try {
                Policy policy = null;
                if (ssg.isFederatedGateway()) {
                    // Federated SSG -- use a SAML token for authentication.
                    log.info("Trying SAML-authenticated policy download from Federated Gateway " + ssg);
                    PrivateKey key = null;
                    if (ssg.getTrustedGateway() != null) {
                        request.prepareClientCertificate(); // TODO make client cert work with third-part WS-Trust
                        key = ssg.getClientCertificatePrivateKey();
                    }
                    SamlAssertion saml = request.getOrCreateSamlAssertion();
                    policy = PolicyServiceClient.downloadPolicyWithSamlAssertion(ssg,
                                                                                 serviceId,
                                                                                 serverCert,
                                                                                 useSsl || key == null, 
                                                                                 saml,
                                                                                 key);
                } else if (ssg.getClientCertificate() != null) {
                    // Trusted SSG, but with a client cert -- use WSS signature for authentication.
                    log.info("Trying WSS-signature-authenticated policy download from Trusted Gateway " + ssg);
                    request.prepareClientCertificate();
                    X509Certificate clientCert = ssg.getClientCertificate();
                    PrivateKey key = ssg.getClientCertificatePrivateKey();
                    policy = PolicyServiceClient.downloadPolicyWithWssSignature(ssg, serviceId, serverCert, useSsl,
                                                                                clientCert, key);
                } else {
                    // Trusted SSG, but with no client cert -- use HTTP Basic over SSL for authentication.
                    log.info("Trying HTTP Basic-over-SSL authenticated policy download from Trusted Gateway " + ssg);
                    PasswordAuthentication creds = request.getCredentialsForTrustedSsg();
                    policy = PolicyServiceClient.downloadPolicyWithHttpBasicOverSsl(ssg, serviceId, serverCert, creds);
                }
                if (policy == null)
                    throw new ConfigurationException("Unable to obtain a policy."); // can't happen
                return policy;
            } catch (BadCredentialsException e) {
                String msg = "Policy service denies access to this policy with current credentials";
                if (ssg.isFederatedGateway()) {
                    msg += "; federated policy download therefore fails.";
                    log.info(msg);
                    throw new ConfigurationException(msg);
                } else
                    log.info(msg);

                log.info("Prompting for new credentials");
                request.getNewCredentials();
                log.info("Retrying policy download with new credentials");
                // FALLTHROUGH and retry
            } catch (InvalidDocumentFormatException e) {
                throw new CausedIOException("Unable to download new policy", e);
            } catch (SSLHandshakeException e) {
                if (e.getCause() instanceof ServerCertificateUntrustedException)
                    throw (ServerCertificateUntrustedException) e.getCause();
                throw e;
            }
        }

        throw new ConfigurationException("Too many unsuccessful attempts; policy download therefore fails");
    }
}
