/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.composite.AllAssertion;

/**
 * The <code>AllAssertionTreeNode</code> is the composite
 * assertion node that represents the 'AND' conjunction.
 */
public class AllAssertionTreeNode extends CompositeAssertionTreeNode<AllAssertion> {
    public AllAssertionTreeNode(AllAssertion assertion) {
        super(assertion);
    }

    public String getName(final boolean decorate) {
        return "All assertions must evaluate to true";
    }

}
