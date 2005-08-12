package com.l7tech.console.tree.policy;


import com.l7tech.console.action.EqualityAssertionPropertiesAction;
import com.l7tech.policy.assertion.EqualityAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class EqualityAssertionPolicyNode is a policy node that corresponds to
 * {@link com.l7tech.policy.assertion.EqualityAssertion}.
 */
public class EqualityAssertionPolicyNode extends LeafAssertionTreeNode {
    private EqualityAssertion assertion;

    public EqualityAssertionPolicyNode(EqualityAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Proceed if " + assertion.getExpression1() + " = " + assertion.getExpression2();
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
        return new EqualityAssertionPropertiesAction(this);
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
        Action a = new EqualityAssertionPropertiesAction(this);
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