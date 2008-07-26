/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.gateway.common.LicenseException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerOneOrMoreAssertion extends ServerCompositeAssertion implements ServerAssertion {
    public ServerOneOrMoreAssertion( OneOrMoreAssertion data, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);
        this.data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        mustHaveChildren(data);
        ServerAssertion[] kids = getChildren();
        ServerAssertion child;
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for (ServerAssertion kid : kids) {
            // If the assertion is disabled, then ignore it and continue to check the next assertion.
            if (! kid.getAssertion().isEnabled()) {
                continue;
            }
            child = kid;
            result = child.checkRequest(context);
            context.assertionFinished(child, result);
            if (result == AssertionStatus.NONE) return result;
            else {
                seenAssertionStatus(context, result);
            }
        }
        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(context);
        return result;
    }

    protected OneOrMoreAssertion data;
}
