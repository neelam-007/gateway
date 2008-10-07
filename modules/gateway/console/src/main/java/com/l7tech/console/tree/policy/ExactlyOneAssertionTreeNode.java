/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;

/**
 * Represents an ExactlyOne assertion.  Needed for proper WS-SecurityPolicy compatibility.
 */
public class ExactlyOneAssertionTreeNode extends CompositeAssertionTreeNode<ExactlyOneAssertion> {
    public ExactlyOneAssertionTreeNode(ExactlyOneAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Exactly one assertion must evaluate to true";
    }
}