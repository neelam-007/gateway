/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AuditAssertionPropertiesAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represent an AuditAssertion in the policy tree.
 */
public class AuditAssertionTreeNode extends LeafAssertionTreeNode {
    public AuditAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (!(assertion instanceof AuditAssertion))
            throw new IllegalArgumentException("Argument is not an AuditAssertion");
    }

    public String getName() {
        return "Audit Assertion";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    public AuditAssertion getAssertion() {
        return (AuditAssertion)getUserObject();
    }

    public boolean canDelete() {
        return true;
    }
    
    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new AuditAssertionPropertiesAction(this);
    }

}
