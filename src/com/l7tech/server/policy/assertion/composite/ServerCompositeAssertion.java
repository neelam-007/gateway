/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.message.Response;
import com.l7tech.message.Request;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerCompositeAssertion implements ServerAssertion {
    protected ServerAssertion[] children;

    public ServerCompositeAssertion( CompositeAssertion composite ) {
        this.children = (ServerAssertion[])ServerPolicyFactory.getInstance().makeCompositePolicy( composite ).toArray( new ServerAssertion[0] );
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

    protected void rollbackDeferredAssertions(Request request, Response response) {
        if (request != null)
            request.removeDeferredAssertion(this);
        if (response != null)
            response.removeDeferredAssertion(this);
        for (int i = 0; i < children.length; i++) {
            ServerAssertion child = children[i];
            if (child instanceof ServerCompositeAssertion)
                ((ServerCompositeAssertion)child).rollbackDeferredAssertions(request, response);
            if (request != null)
                request.removeDeferredAssertion(child);
            if (response != null)
                response.removeDeferredAssertion(child);
        }
    }
}
