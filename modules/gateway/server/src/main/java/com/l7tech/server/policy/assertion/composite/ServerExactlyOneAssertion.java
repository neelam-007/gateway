/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
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

/**
 * @author alex
 * @version $Revision$
 */
public class ServerExactlyOneAssertion extends ServerCompositeAssertion implements ServerAssertion {
    public ServerExactlyOneAssertion( ExactlyOneAssertion data, ApplicationContext applicationContext ) throws PolicyAssertionException, LicenseException {
        super( data, applicationContext );
        this.data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        mustHaveChildren(data);
        ServerAssertion[] kids = getChildren();
        ServerAssertion child;
        AssertionStatus result = null;
        int numSucceeded = 0;
        for (ServerAssertion kid : kids) {
            child = kid;
            try {
                result = child.checkRequest(context);
            } catch (AssertionStatusException e) {
                result = e.getAssertionStatus();
            }
            context.assertionFinished(child, result);
            if (result == AssertionStatus.NONE)
                ++numSucceeded;
        }

        result =  numSucceeded == 1 ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(context);
        return result;
    }

    protected ExactlyOneAssertion data;
}
