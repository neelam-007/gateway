/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;

/**
 * The ClientProxy's default PolicyManager.  Loads policies from the SSG on-demand
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:22:35 AM
 */
public class PolicyManagerImpl implements PolicyManager {
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
        return policy == null ? nullPolicy : policy;
    }
}
