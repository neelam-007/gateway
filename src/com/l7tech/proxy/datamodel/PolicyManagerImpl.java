/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.composite.AllAssertion;

import java.io.IOException;
import java.util.Arrays;

/**
 * The ClientProxy's default PolicyManager.  Loads policies from the SSG on-demand
 * TODO: cache polcies inside their SSG objects
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:22:35 AM
 */
public class PolicyManagerImpl implements PolicyManager {
    private static final PolicyManagerImpl INSTANCE = new PolicyManagerImpl();

    private PolicyManagerImpl() {
    }

    public static PolicyManagerImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Obtain the policy for this SSG, possibly by downloading it over the network.
     * @param ssg
     * @return The root of policy Assertion tree.
     * @throws IOException if we were unable to read the policy.
     */
    public Assertion getPolicy(Ssg ssg) throws IOException {

        // TODO: write this for real, instead of using hardcoded policy
        return new AllAssertion(Arrays.asList(new Assertion[] {
            new SslAssertion(),
            new HttpBasic()
        }));
    }
}
