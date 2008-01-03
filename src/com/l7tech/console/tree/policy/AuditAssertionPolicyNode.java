/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AuditAssertionPropertiesAction;
import com.l7tech.policy.assertion.AuditAssertion;

import javax.swing.*;

/**
 * Represent an AuditAssertion in the policy tree.
 */
public class AuditAssertionPolicyNode extends LeafAssertionTreeNode<AuditAssertion> {
    public AuditAssertionPolicyNode(AuditAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Audit Assertion";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new AuditAssertionPropertiesAction(this, isDescendantOfInclude(true));
    }

}
