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
     * Obtain the policy for this SSG, possibly by downloading it over the network.
     * TODO: This currently assumes only one service per Ssg object.  fix this
     *
     * @param request the request whose policy is to be found
     * @return The root of policy Assertion tree.
     */
    public Assertion getPolicy(PendingRequest request) {
        Assertion policy = request.getSsg().getPolicy();
        return policy == null ? nullPolicy : policy;
    }
}
