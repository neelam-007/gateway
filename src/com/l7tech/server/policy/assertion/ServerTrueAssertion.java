/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerTrueAssertion implements ServerAssertion {
    public ServerTrueAssertion( TrueAssertion ass ) {
        // meaningless
    }

    public ServerTrueAssertion() {
        // meaningless
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
