/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

/**
 * The <code>OneOrMoreAssertionTreeNode</code> is the composite
 * assertion that represens 'OR' disjunction
 */
public class OneOrMoreAssertionTreeNode extends CompositeAssertionTreeNode<OneOrMoreAssertion> {
    public OneOrMoreAssertionTreeNode(OneOrMoreAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName(final boolean decorate) {
        return "At least one assertion must evaluate to true";
    }
}
