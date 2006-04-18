/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import org.apache.ws.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.tree.policy.PolicyException;

/**
 * Converts a layer 7 policy into a WS-SecurityPolicy tree.
 */
public class WsspWriter {
    /**
     * Convert the specified layer 7 policy into WS-SecurityPolicy format.  The Layer 7 policy must already
     * have been run through the {@link com.l7tech.server.policy.filter.FilterManager} to remove any assertions
     * that are not relevant to the client consuming the service.
     *
     * @param layer7Root  the layer 7 policy tree to convert.  Must not be null.
     * @return  the converted Apache Policy.  Never null.
     * @throws PolicyException if the specified policy tree cannot be expressed in WS-SecurityPolicy format.
     */
    public Policy convertFromLayer7(Assertion layer7Root) throws PolicyException {
        // TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
