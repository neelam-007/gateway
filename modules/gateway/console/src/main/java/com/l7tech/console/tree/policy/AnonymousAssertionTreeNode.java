/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.TrueAssertion;

/**
 * Class <code>AnonymousAssertionTreeNode</code> is a tree node that
 * represents the anonymous access. It is modelled as <code>TrueAssertion</code>
 */
class AnonymousAssertionTreeNode extends LeafAssertionTreeNode<TrueAssertion> {
    public AnonymousAssertionTreeNode(TrueAssertion node) {
        super(node);
    }

    public String getName(final boolean decorate) {
        return "Anonymous access";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Alert16x16.gif";
    }
}