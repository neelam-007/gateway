/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.ConfigurationException;
import java.util.logging.Logger;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
        Policy policy = request.getSsg().lookupPolicy(request.getUri(), request.getSoapAction());
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
        request.getSsg().removePolicy(request.getUri(), request.getSoapAction());
    }

    /**
     * Notify the PolicyManager that a policy may be out-of-date.
     * The PolicyManager should attempt to update the policy if it needs to do so.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     * @param policyUrl The URL to fetch the policy from
     * @throws ConfigurationException if the PendingRequest did not contain enough information to construct a
     *                                valid PolicyAttachmentKey
     * @throws IOException if the policy could not be read from the SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException if an SSL handshake with the SSG could not be established due to
     *                                             the SSG's SSL certificate being unrecognized
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if credentials were required, but the user canceled the logon dialog
     */
    public void updatePolicy(PendingRequest request, URL policyUrl)
            throws ConfigurationException, IOException, ServerCertificateUntrustedException, OperationCanceledException, HttpChallengeRequiredException
    {
        HttpClient client = new HttpClient();
        HttpState state = client.getState();
        state.setAuthenticationPreemptive(false);
        state.setCredentials(null, null, null);
        GetMethod getMethod = new GetMethod(policyUrl.toString());
        getMethod.setDoAuthentication(false);
        try {
            log.info("Downloading new policy from " + policyUrl);
            CurrentRequest.setPeerSsg(request.getSsg());
            int status = client.executeMethod(getMethod);
            CurrentRequest.setPeerSsg(null);

            // fla, added - try again once after first 401
            if (status == 401) {
                if (request.getSsg().getTrustedGateway() != null) {
                    throw new ConfigurationException("Only anonymous policy downloads are supported when using a Federated Gateway");
                }

                getMethod.releaseConnection();
                // was a new url provided ?
                Header newURLHeader = getMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER);
                URL newUrl = policyUrl;
                if (newURLHeader != null) {
                    try {
                        newUrl = new URL(newURLHeader.getValue());
                    } catch (MalformedURLException e) {
                        throw new ConfigurationException("Policy server sent us an invalid policy URL: " + newURLHeader.getValue());
                    }
                }
                if (!newUrl.getProtocol().equalsIgnoreCase("https"))
                    throw new ConfigurationException("Policy server sent us a 401 status with a non-https policy URL");

                int newPort = newUrl.getPort();
                if (newPort == -1)
                    newPort = 443;
                if ((newPort == 443 || newPort == 8443) &&
                        (request.getSsg().getSslPort() != 443) &&
                        (request.getSsg().getSslPort() != 8443)) {
                    // Work-around for disco.modulator Bug #826.  If we are expecting a non-default SSL port,
                    // but the Gateway sends us a URL using the default SSL port, replace it with our non-default.
                    newPort = request.getSsg().getSslPort();
                }
                URL safeUrl = new URL("https", policyUrl.getHost(), newPort, newUrl.getFile());
                log.info("Policy download unauthorized, trying again with credentials at " + safeUrl);

                // Make sure we actually have the credentials
                request.getCredentials();
                state.setAuthenticationPreemptive(true);

                int attempts = 0;
                for (;;) {
                    String username = request.getUsername();
                    char[] password = request.getPassword();
                    state.setCredentials(null, null, new UsernamePasswordCredentials(username, new String(password)));
                    getMethod = new GetMethod(safeUrl.toString());
                    getMethod.setDoAuthentication(true);
                    try {
                        CurrentRequest.setPeerSsg(request.getSsg());
                        status = client.executeMethod(getMethod);
                        CurrentRequest.setPeerSsg(null);
                        if ((status == 401 || status == 404) && ++attempts < 10) {
                            log.info("Got " + status + " status downloading policy; will get new credentials and try download again");
                            request.getNewCredentials();
                            continue;
                        }
                        if (status == 200)
                            break;
                        throw new IOException("Got back unexpected HTTP status " + status + " from the policy server");
                    } catch (SSLHandshakeException e) {
                        if (e.getCause() instanceof ServerCertificateUntrustedException)
                            throw (ServerCertificateUntrustedException) e.getCause();
                        throw e;
                    }
                }
            }

            Header versionHeader = getMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION);
            if (versionHeader == null)
                throw new ConfigurationException("The policy server failed to provide a " + SecureSpanConstants.HttpHeaders.POLICY_VERSION + " header");
            log.info("Policy id and version: " + versionHeader.getValue());

            log.info("Policy download completed with HTTP status " + status);
            Assertion assertion = WspReader.parse(getMethod.getResponseBodyAsStream());
            PolicyAttachmentKey pak = new PolicyAttachmentKey(request.getUri(), request.getSoapAction());
            Policy policy = new Policy(assertion, versionHeader.getValue());
            request.getSsg().attachPolicy(pak, policy);
            request.getRequestInterceptor().onPolicyUpdated(request.getSsg(), pak, policy);
            log.info("New policy saved successfully");
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Client request must have either a SOAPAction header or a valid SOAP body namespace URI");
        } finally {
            getMethod.releaseConnection();
        }
    }
}
