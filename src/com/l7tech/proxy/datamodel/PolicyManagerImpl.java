/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.util.ThreadLocalHttpClient;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Category;

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
    private static final Assertion nullPolicy = TrueAssertion.getInstance();

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
    public Assertion getPolicy(PendingRequest request) {
        Assertion policy = null;
        if (policy == null && request.getSoapAction().length() > 0)
            policy = request.getSsg().getPolicyBySoapAction(request.getSoapAction());
        if (policy == null && request.getUri().length() > 0)
            policy = request.getSsg().getPolicyByUri(request.getUri());
        log.info(policy != null ? "Located policy for this request" : "No policy found for this request");
        return policy == null ? nullPolicy : policy;
    }

    /**
     * Notify the PolicyManager that a policy may be out-of-date.
     * The PolicyManager should attempt to update the policy if it needs to do so.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     * @param policyUrl The URL to fetch the policy from
     * @throws ConfigurationException if a policy update was already attempted for this request
     * @throws IOException if the policy could not be read from the SSG
     */
    public void updatePolicy(PendingRequest request, URL policyUrl) throws ConfigurationException, IOException {
        HttpClient client = ThreadLocalHttpClient.getHttpClient();
        client.getState().setAuthenticationPreemptive(false);
        client.getState().setCredentials(null, null, null);
        GetMethod getMethod = new GetMethod(policyUrl.toString());
        getMethod.setDoAuthentication(false); // TODO: will authentication be required to download a policy?
        try {
            log.info("Downloading new policy from " + policyUrl);
            int status = client.executeMethod(getMethod);
            Assertion policy = WspReader.parse(getMethod.getResponseBodyAsStream());
            request.getSsg().attachPolicy(request.getUri(), request.getSoapAction(), policy);
            Managers.getSsgManager().save(); // save changes
            log.info("New policy saved successfully");
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Client request must have either a SOAPAction header or a valid SOAP body namespace URI");
        } finally {
            getMethod.releaseConnection();
        }
    }
}
