/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.util.PolicyServiceClient;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * The ClientProxy's default PolicyManager.  Loads policies from the SSG on-demand
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:22:35 AM
 */
public class PolicyManagerImpl implements PolicyManager {
    private static final Logger log = Logger.getLogger(PolicyManagerImpl.class.getName());
    private static final PolicyManagerImpl INSTANCE = new PolicyManagerImpl();
    public static final String PROPERTY_LOGPOLICIES    = "com.l7tech.proxy.datamodel.logPolicies";

    private PolicyManagerImpl() {
    }

    public static PolicyManagerImpl getInstance() {
        return INSTANCE;
    }

    private static class LogFlags {
        private static final boolean logPolicies = Boolean.getBoolean(PROPERTY_LOGPOLICIES);
    }

    /**
     * Look up any cached policy for this request+SSG.  If we don't know it, or if our
     * cached copy is out-of-date, no sweat: we'll get a SOAP fault from the server telling
     * us to download a new policy.
     *
     * @param request the request whose policy is to be found
     * @return The Policy we found, or null if we didn't find one.
     */
    public Policy getPolicy(PendingRequest request) {
        Policy policy = request.getSsg().lookupPolicy(request.getUri(), request.getSoapAction(), request.getOriginalUrl().getFile());
        if (policy != null) {
            if (LogFlags.logPolicies)
                log.info("PolicyManager: Found a policy for this request: " + policy.getAssertion());
            else
                log.info("PolicyManager: Found a policy for this request");
        } else
            log.info("PolicyManager: No policy found for this request");
        return policy;
    }

    /**
     * Notify the PolicyManager that a policy may be out-of-date and should be flushed from the cache.
     * The PolicyManager will not attempt to download a replacement one at this time.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     */
    public void flushPolicy(PendingRequest request) {
        request.getSsg().removePolicy(request.getUri(), request.getSoapAction(), request.getOriginalUrl().getFile());
    }

    /**
     * Notify the PolicyManager that a policy may be out-of-date.
     * The PolicyManager should attempt to update the policy if it needs to do so.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     * @param serviceId The ID of the service for which to load policy.
     * @throws ConfigurationException if the PendingRequest did not contain enough information to construct a
     *                                valid PolicyAttachmentKey
     * @throws IOException if the policy could not be read from the SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException if an SSL handshake with the SSG could not be established due to
     *                                             the SSG's SSL certificate being unrecognized
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if credentials were required, but the user canceled the logon dialog
     */
    public void updatePolicy(PendingRequest request, String serviceId)
            throws ConfigurationException, IOException, GeneralSecurityException,
                   OperationCanceledException, HttpChallengeRequiredException, KeyStoreCorruptException,
                   ClientCertificateException, PolicyRetryableException
    {
        PolicyAttachmentKey pak = new PolicyAttachmentKey(request.getUri(), request.getSoapAction(), request.getOriginalUrl().getFile());
        Ssg ssg = request.getSsg();
        boolean useSsl = ssg.isUseSslByDefault();
        X509Certificate serverCert = SsgKeyStoreManager.getServerCert(ssg);
        if (serverCert == null)
            throw new ServerCertificateUntrustedException("Server certificate not yet known");

        // Try anonymous download first
        try {
            if (useSsl && SsgKeyStoreManager.isClientCertAvailabile(ssg))
                log.info("Trying SSL-with-client-cert policy download from " + ssg);
            else
                log.info("Trying anonymous policy download from " + ssg);
            Policy policy = PolicyServiceClient.downloadPolicyWithNoAuthentication(ssg, serviceId, serverCert, useSsl);
            request.getSsg().attachPolicy(pak, policy);
            request.getRequestInterceptor().onPolicyUpdated(request.getSsg(), pak, policy);
            log.info("New policy saved successfully");
            return;
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
                if (ssg.getTrustedGateway() != null) {
                    // Federated SSG -- use a SAML token for authentication.
                    log.info("Trying SAML-authenticated policy download from Federated Gateway " + ssg);
                    request.prepareClientCertificate();
                    SamlHolderOfKeyAssertion samlHok = request.getOrCreateSamlHolderOfKeyAssertion();
                    PrivateKey key = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
                    if (key == null) throw new ConfigurationException("Unable to obtain client cert private key"); // shouldn't happen
                    policy = PolicyServiceClient.downloadPolicyWithSamlAssertion(ssg, serviceId, serverCert, useSsl, samlHok, key);
                } else if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                    // Trusted SSG, but with a client cert -- use WSS signature for authentication.
                    log.info("Trying WSS-signature-authenticated policy download from Trusted Gateway " + ssg);
                    request.prepareClientCertificate();
                    X509Certificate clientCert = SsgKeyStoreManager.getClientCert(ssg);
                    PrivateKey key = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
                    policy = PolicyServiceClient.downloadPolicyWithWssSignature(ssg, serviceId, serverCert, useSsl,
                                                                                clientCert, key);
                } else {
                    // Trusted SSG, but with no client cert -- use HTTP Basic over SSL for authentication.
                    log.info("Trying HTTP Basic-over-SSL authenticated policy download from Trusted Gateway " + ssg);
                    PasswordAuthentication creds = request.getCredentials();
                    policy = PolicyServiceClient.downloadPolicyWithHttpBasicOverSsl(ssg, serviceId, serverCert, creds);
                }
                if (policy == null)
                    throw new ConfigurationException("Unable to obtain a policy."); // can't happen
                request.getSsg().attachPolicy(pak, policy);
                request.getRequestInterceptor().onPolicyUpdated(request.getSsg(), pak, policy);
                log.info("New policy saved successfully");
                return;
            } catch (BadCredentialsException e) {
                String msg = "Policy service denies access to this policy with current credentials";
                if (ssg.getTrustedGateway() != null) {
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
