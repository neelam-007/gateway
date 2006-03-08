package com.l7tech.console.tree.policy;


import com.l7tech.console.action.ComparisonAssertionPropertiesAction;
import com.l7tech.policy.assertion.ComparisonAssertion;
import com.l7tech.common.util.ComparisonOperator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class ComparisonAssertionPolicyNode is a policy node that corresponds to
 * {@link com.l7tech.policy.assertion.ComparisonAssertion}.
 */
public class ComparisonAssertionPolicyNode extends LeafAssertionTreeNode {
    private ComparisonAssertion assertion;

    public ComparisonAssertionPolicyNode(ComparisonAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        StringBuffer name = new StringBuffer("Proceed if ");
        name.append(assertion.getExpression1());
        if (assertion.getOperator() == ComparisonOperator.CONTAINS) {
            if (assertion.isNegate()) {
                name.append(" does not contain ");
            } else {
                name.append(" contains ");
            }
        } else {
            name.append(" is ");
            if (assertion.isNegate()) name.append("not ");
            name.append(assertion.getOperator().toString());
        }
        if (!assertion.getOperator().isUnary()) {
            name.append(" ").append(assertion.getExpression2());
        }
        return name.toString();

    }

    /**
     * Test if the node can be deleted.
     *
     * @return always true
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new ComparisonAssertionPropertiesAction(this);
    }

    protected void loadChildren() {

    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new ComparisonAssertionPropertiesAction(this);
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
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