package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EmailAlertAssertionPropertiesAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Policy tree node for {@link EmailAlertAssertion}.
 */
public class EmailAlertAssertionTreeNode extends LeafAssertionTreeNode {
    public EmailAlertAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof EmailAlertAssertion) {
            nodeAssertion = (EmailAlertAssertion)assertion;
        } else
            throw new IllegalArgumentException("assertion passed must be of type " + EmailAlertAssertion.class.getName());
    }

    public String getName() {
        return "Send Email Alert";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Edit16.gif";
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
        return new EmailAlertAssertionPropertiesAction(this);
    }

    public boolean canDelete() {
        return true;
    }

    public EmailAlertAssertion getSnmpTrapAssertion() {
        return nodeAssertion;
    }

    private EmailAlertAssertion nodeAssertion;
}
