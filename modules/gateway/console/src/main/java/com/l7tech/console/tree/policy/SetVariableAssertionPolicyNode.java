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
    private static final int MAX_DISPLAY_LENGTH = 40;

    private SetVariableAssertion assertion;

    public SetVariableAssertionPolicyNode(SetVariableAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName(final boolean decorate) {
        final String assertionName = "Set Context Variable";

        StringBuffer name = new StringBuffer(assertionName + " ");
        name.append(assertion.getVariableToSet());
        name.append(" as ");
        name.append(assertion.getDataType().getName());
        name.append(" to");
        final String expression = assertion.expression();
        if (expression.length() == 0) {
            name.append(" empty");
        } else if (expression.length() <= MAX_DISPLAY_LENGTH) {
            name.append(": ");
            name.append(expression);
        } else {
            name.append(": ");
            name.append(expression, 0, MAX_DISPLAY_LENGTH - 1);
            name.append("...");
        }
        return (decorate)? name.toString(): assertionName;
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
