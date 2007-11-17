/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.FalseAssertion;

/**
 * Class FalseAssertionPolicyNode is a policy node that corresponds the
 * <code>FalseAssertion</code>.
 */
public class FalseAssertionPolicyNode extends LeafAssertionTreeNode {

    public FalseAssertionPolicyNode(FalseAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Stop processing";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Stop16.gif";
    }
}
