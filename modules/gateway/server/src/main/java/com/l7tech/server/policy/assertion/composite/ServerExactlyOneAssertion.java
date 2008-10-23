/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.gateway.common.LicenseException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;

public final class ServerExactlyOneAssertion extends ServerCompositeAssertion<ExactlyOneAssertion> implements ServerAssertion {
    public ServerExactlyOneAssertion( ExactlyOneAssertion data, ApplicationContext applicationContext ) throws PolicyAssertionException, LicenseException {
        super( data, applicationContext );
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final List<ServerAssertion> kids = getChildren();
        AssertionStatus result;
        int numSucceeded = 0;
        for (ServerAssertion kid : kids) {
            try {
                result = kid.checkRequest(context);
            } catch (AssertionStatusException e) {
                result = e.getAssertionStatus();
            }

            context.assertionFinished(kid, result);

            if (result == AssertionStatus.NONE)
                ++numSucceeded;
        }

        result = numSucceeded == 1 ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;

        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(context);

        return result;
    }
}
