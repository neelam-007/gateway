/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerFalseAssertion extends AbstractServerAssertion<FalseAssertion> {
    public ServerFalseAssertion( FalseAssertion ass ) {
        super(ass);
    }

    public ServerFalseAssertion() {
        super(null);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }
}
