/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.tree.policy.PolicyException;
import org.apache.ws.policy.Policy;

/**
 * Converts WS-SecurityPolicy into Layer 7 policy.
 */
public class WsspReader {
    /**
     * Convert the specified Apache policy into a Layer 7 policy.
     *
     * @param wsspPolicy  the WS-Policy tree including WS-SecurityPolicy assertions that is to be converted into L7 form.  Must not be null.
     * @return the converted Layer 7 policy tree.  Never null
     * @throws PolicyException if the specified wssp policy cannot be expressed in Layer 7 form
     */
    public Assertion convertFromWssp(Policy wsspPolicy) throws PolicyException {
        // TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
