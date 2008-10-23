/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ServerCompositeAssertion<CT extends CompositeAssertion>
    extends AbstractServerAssertion<CT>
    implements ServerAssertion
{
    private static final Logger logger = Logger.getLogger(ServerCompositeAssertion.class.getName());
    private List<ServerAssertion> children;

    public ServerCompositeAssertion(CT composite, ApplicationContext spring) throws PolicyAssertionException, LicenseException {
        super(composite);
        if (spring == null)
            throw new IllegalArgumentException("The Application Context is required");

        if (composite.getChildren().isEmpty()) throw new PolicyAssertionException(assertion, "Must have children");

        ServerPolicyFactory pf = (ServerPolicyFactory)spring.getBean("policyFactory");
        Assertion child;
        List<ServerAssertion> result = new ArrayList<ServerAssertion>(composite.getChildren().size());
        for (Iterator i = composite.children(); i.hasNext();) {
            child = (Assertion)i.next();
            ServerAssertion sass = pf.compileSubtree(child);
            if (sass != null)
                result.add(sass);
        }
        this.children = Collections.unmodifiableList(result);
    }

    public List<ServerAssertion> getChildren() {
        return children;
    }

    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + ">";
    }

    protected void seenAssertionStatus(PolicyEnforcementContext context, AssertionStatus assertionStatus) {
        if(context != null) {
            context.addSeenAssertionStatus(assertionStatus);
        }
    }

    protected void rollbackDeferredAssertions(PolicyEnforcementContext context) {
        if (context != null)
            context.removeDeferredAssertion(this);
        for (ServerAssertion child : children) {
            if (child instanceof ServerCompositeAssertion)
                ((ServerCompositeAssertion)child).rollbackDeferredAssertions(context);
            if (context != null)
                context.removeDeferredAssertion(child);
        }
    }

    public void close() {
        for (ServerAssertion child : children) {
            try {
                child.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Child assertion did not close cleanly", e);
            }
        }
    }
}
