/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.WsspAssertion;

/**
 * Policy node for WSSP assertion.
 */
public class WsspAssertionPolicyNode extends LeafAssertionTreeNode<WsspAssertion> {

    //- PUBLIC

    public WsspAssertionPolicyNode(WsspAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "WS-Security Policy Compliance";
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }
}
