/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientCompositeAssertion extends ClientAssertion {
    protected ClientAssertion[] children;

    public ClientCompositeAssertion() {
        super();
    }

    public ClientCompositeAssertion( CompositeAssertion composite ) {
        this.children = (ClientAssertion[])ClientPolicyFactory.getInstance().makeCompositePolicy( composite ).toArray( new ClientAssertion[0] );
    }

    /**
     * Create a new CompositeAssertion with no parent and the specified children.
     * The children will be copied, and each of their parents reset to point to us.
     * @param children
     */
    public ClientCompositeAssertion( ClientAssertion[] children ) {
        this.children = children;
    }

    public ClientAssertion[] getChildren() {
        return children;
    }

    /** Remove all this composite assertion's children's pending decorations. */
    protected void rollbackPendingDecorations(PolicyApplicationContext context) {
        context.getPendingDecorations().remove(this);
        for (int i = 0; i < children.length; i++) {
            ClientAssertion child = children[i];
            if (child instanceof ClientCompositeAssertion)
                ((ClientCompositeAssertion)child).rollbackPendingDecorations(context);
            context.getPendingDecorations().remove(child);
        }
    }

    /**
     * Ensure that this CompositeAssertion has at least one child.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if the children list is empty
     */
    public void mustHaveChildren() throws PolicyAssertionException {
        if ( children.length == 0 )
            throw new PolicyAssertionException("CompositeAssertion has no children: " + this);
    }

    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + ">";
    }

    public String iconResource(boolean open) {
        if (open)
            return "com/l7tech/proxy/resources/tree/folderOpen.gif";

        return "com/l7tech/proxy/resources/tree/folder.gif";
    }

    protected void mustHaveChildren(CompositeAssertion data) throws PolicyAssertionException {
        if (data.getChildren().isEmpty())
            throw new PolicyAssertionException("CompositeAssertion has no children: " + this);
    }
}
