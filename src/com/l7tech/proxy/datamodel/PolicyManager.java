/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;

import java.io.IOException;

/**
 * Manages policies for SSGs.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 10:19:51 AM
 */
public interface PolicyManager {

    /**
     * Obtain the policy for this SSG, possibly by downloading it over the network.
     * @param ssg
     * @return The root of policy Assertion tree.
     * @throws IOException if we were unable to read the policy.
     */
    Assertion getPolicy(Ssg ssg) throws IOException;
}
