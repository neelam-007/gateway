/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.ConfigurationException;

import java.io.IOException;
import java.net.URL;

/**
 * Manages policies for SSGs.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:19:51 AM
 */
public interface PolicyManager {

    /**
     * Obtain the policy for this pending request, possibly by downloading
     * it over the network.
     * @param request the request whos policy is to be found
     * @return The root of policy Assertion tree.
     */
    Assertion getPolicy(PendingRequest request);

    /**
     * Notify the PolicyManager that a policy may be out-of-date.
     * The PolicyManager should attempt to update the policy if it needs to do so.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     * @param policyUrl The URL from which to load the policy (using a simple HTTP GET).
     * @throws ConfigurationException if a policy for this request cannot be obtained for config reasons
     * @throws IOException if there was a problem getting the policy from the server
     */
    void updatePolicy(PendingRequest request, URL policyUrl) throws ConfigurationException, IOException;
}
