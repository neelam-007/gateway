/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

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
    Policy getPolicy(PendingRequest request);

    /**
     * Notify the PolicyManager that a policy may be out-of-date and should be flushed from the cache.
     * The PolicyManager will not attempt to download a replacement one at this time.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     */
    void flushPolicy(PendingRequest request);

    /**
     * Notify the PolicyManager that a policy may be out-of-date.
     * The PolicyManager should attempt to update the policy if it needs to do so.
     * @param request The request that failed in a way suggestive that its policy may be out-of-date.
     * @param serviceid The ID of the service for which to load the policy.
     * @throws ConfigurationException if a policy for this request cannot be obtained for config reasons
     * @throws IOException if there was a problem getting the policy from the server
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if the user canceled the login dialog
     */
    void updatePolicy(PendingRequest request, String serviceid) throws ConfigurationException, IOException, GeneralSecurityException, OperationCanceledException, HttpChallengeRequiredException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException;
}
