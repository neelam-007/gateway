/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerCompositeAssertion implements ServerAssertion {
    protected ServerAssertion[] children;
    private ApplicationContext applicationContext;

    public ServerCompositeAssertion( CompositeAssertion composite, ApplicationContext context ) {
        this.applicationContext = context;
        if (applicationContext == null) {
            throw new IllegalArgumentException("The Application Context is required");
        }
        PolicyFactory pf = (PolicyFactory)applicationContext.getBean("policyFactory");
        this.children = (ServerAssertion[])pf.makeCompositePolicy( composite ).toArray( new ServerAssertion[0] );
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
            throw new PolicyAssertionException("CompositeAssertion has no children: " + this);
    }

    protected final static void mustHaveChildren(CompositeAssertion ca) throws PolicyAssertionException {
        if (ca.getChildren().isEmpty())
            throw new PolicyAssertionException("CompositeAssertion has no children: " + ca);            
    }

    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + ">";
    }

    protected void rollbackDeferredAssertions(PolicyEnforcementContext context) {
        if (context != null)
            context.removeDeferredAssertion(this);
        for (int i = 0; i < children.length; i++) {
            ServerAssertion child = children[i];
            if (child instanceof ServerCompositeAssertion)
                ((ServerCompositeAssertion)child).rollbackDeferredAssertions(context);
            if (context != null)
                context.removeDeferredAssertion(child);
        }
    }
}
