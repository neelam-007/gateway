/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SetVariableAssertionPropertiesAction;
import com.l7tech.policy.assertion.SetVariableAssertion;

import javax.swing.*;

/**
 * Class SetVariableAssertionPolicyNode is a policy node that corresponds to
 * {@link com.l7tech.policy.assertion.SetVariableAssertion}.
 */
public class SetVariableAssertionPolicyNode extends LeafAssertionTreeNode {
    private SetVariableAssertion assertion;

    public SetVariableAssertionPolicyNode(SetVariableAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        StringBuffer name = new StringBuffer("Set ");
        name.append(assertion.getVariableToSet());
        name.append(" to ");
        name.append(assertion.getExpression());
        return name.toString();
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new SetVariableAssertionPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/check16.gif";
    }
}
