/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerAllAssertion extends ServerCompositeAssertion implements ServerAssertion {
    public ServerAllAssertion(AllAssertion data, ApplicationContext applicationContext) {
        super(data, applicationContext);
        this.data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        mustHaveChildren(data);
        ServerAssertion[] kids = getChildren();
        ServerAssertion child;
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for (int i = 0; i < kids.length; i++) {
            child = kids[i];
            result = child.checkRequest(context);
            if (result != AssertionStatus.NONE) {
                seenAssertionStatus(context,result);
                rollbackDeferredAssertions(context);
                return result;
            }
        }
        return result;
    }

    protected AllAssertion data;
}
