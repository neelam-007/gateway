/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientTrueAssertion;
import com.l7tech.proxy.processor.ServerCertificateUntrustedException;
import com.l7tech.proxy.processor.OperationCanceledException;
import com.l7tech.proxy.util.ThreadLocalHttpClient;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Category;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URL;

/**
 * The ClientProxy's default PolicyManager.  Loads policies from the SSG on-demand
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:22:35 AM
 */
public class PolicyManagerImpl implements PolicyManager {
    private static final Category log = Category.getInstance(PolicyManagerImpl.class);
    private static final PolicyManagerImpl INSTANCE = new PolicyManagerImpl();
    private static final ClientAssertion nullClientPolicy = new ClientTrueAssertion();

    private PolicyManagerImpl() {
    }

    public static PolicyManagerImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Look up any cached policy for this request+SSG.  If we don't know it, or if our
     * cached copy is out-of-date, no sweat: we'll get a SOAP fault from the server telling
     * us to download a new policy.
     *
     * @param request the request whose policy is to be found
     * @return The root of policy Assertion tree.
     */
    public ClientAssertion getClientPolicy(PendingRequest request) {
        ClientAssertion policy = null;
        if (policy == null && request.getSoapAction().length() > 0)
            policy = request.getSsg().lookupClientPolicy(request.getUri(), request.getSoapAction());
        log.info(policy != null ? "Located policy for this request" : "No policy found for this request");
        return policy == null ? nullClientPolicy : policy;
    }

    /**
     * Notify the PolicyManager that a policy may be out-of-date.
     * The PolicyManager should attempt to update the policy if it needs to do so.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     * @param policyUrl The URL to fetch the policy from
     * @throws ConfigurationException if the PendingRequest did not contain enough information to construct a
     *                                valid PolicyAttachmentKey
     * @throws IOException if the policy could not be read from the SSG
     * @throws ServerCertificateUntrustedException if an SSL handshake with the SSG could not be established due to
     *                                             the SSG's SSL certificate being unrecognized
     * @throws OperationCanceledException if credentials were required, but the user canceled the logon dialog
     */
    public void updatePolicy(PendingRequest request, URL policyUrl)
            throws ConfigurationException, IOException, ServerCertificateUntrustedException, OperationCanceledException
    {
        HttpClient client = ThreadLocalHttpClient.getHttpClient();
        client.getState().setAuthenticationPreemptive(false);
        client.getState().setCredentials(null, null, null);
        GetMethod getMethod = new GetMethod(policyUrl.toString());
        getMethod.setDoAuthentication(false);
        try {
            log.info("Downloading new policy from " + policyUrl);
            int status = client.executeMethod(getMethod);

            // fla, added - try again once after first 401
            if (status == 401) {
                getMethod.releaseConnection();
                // was a new url provided ?
                Header newURLHeader = getMethod.getResponseHeader("PolicyUrl");
                String newUrl = policyUrl.toString();
                if (newURLHeader != null) {
                    newUrl = newURLHeader.getValue();
                }
                log.info("Policy download unauthorized, trying again with credentials at " + newUrl);

                // Make sure we actually have the credentials
                Ssg ssg = request.getSsg();
                if (!ssg.isCredentialsConfigured())
                    Managers.getCredentialManager().getCredentials(ssg);

                client.getState().setAuthenticationPreemptive(true);

                String username = ssg.getUsername();
                char[] password = ssg.password();
                int attempts = 0;
                for (;;) {
                    client.getState().setCredentials(null, null, new UsernamePasswordCredentials(username, new String(password)));
                    getMethod = new GetMethod(newUrl);
                    getMethod.setDoAuthentication(true);
                    try {
                        status = client.executeMethod(getMethod);
                        if (status == 401 && ++attempts < 3) {
                            Managers.getCredentialManager().notifyInvalidCredentials(ssg);
                            Managers.getCredentialManager().getCredentials(ssg);
                            continue;
                        } else
                            break;
                    } catch (SSLHandshakeException e) {
                        if (e.getCause() instanceof ServerCertificateUntrustedException)
                            throw (ServerCertificateUntrustedException) e.getCause();
                        throw e;
                    }
                }
            }

            log.info("Policy download completed with HTTP status " + status);
            Assertion policy = WspReader.parse(getMethod.getResponseBodyAsStream());
            PolicyAttachmentKey pak = new PolicyAttachmentKey(request.getUri(), request.getSoapAction());
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
