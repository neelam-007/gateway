/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.gateway.common.LicenseException;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerCompositeAssertion extends AbstractServerAssertion<CompositeAssertion> implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerCompositeAssertion.class.getName());
    private final CompositeAssertion bean;
    protected ServerAssertion[] children;

    public ServerCompositeAssertion( CompositeAssertion composite, ApplicationContext spring) throws PolicyAssertionException, LicenseException {
        super(composite);
        this.bean = composite;
        if (spring == null)
            throw new IllegalArgumentException("The Application Context is required");

        ServerPolicyFactory pf = (ServerPolicyFactory)spring.getBean("policyFactory");
        Assertion child;
        List<ServerAssertion> result = new ArrayList<ServerAssertion>();
        for (Iterator i = composite.children(); i.hasNext();) {
            child = (Assertion)i.next();
            ServerAssertion sass = pf.compileSubtree(child);
            if (sass != null)
                result.add(sass);
        }
        this.children = result.toArray( new ServerAssertion[0] );
    }

    public ServerAssertion[] getChildren() {
        return children;
    }

    /**
     * Ensure that this CompositeAssertion has at least one child.
     * @throws PolicyAssertionException if the children list is empty
     */
    public void mustHaveChildren() throws PolicyAssertionException {
        if ( children.length == 0 )
            throw new PolicyAssertionException(bean, "CompositeAssertion has no children: " + this);
    }

    protected final void mustHaveChildren(CompositeAssertion ca) throws PolicyAssertionException {
        if (ca.getChildren().isEmpty())
            throw new PolicyAssertionException(bean, "CompositeAssertion has no children: " + ca);            
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
