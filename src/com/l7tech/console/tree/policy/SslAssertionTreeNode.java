package com.l7tech.console.tree.policy;


import com.l7tech.console.action.SslPropertiesAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>SslAssertionTreeNode</code> specifies the SSL
 * assertion requirement.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SslAssertionTreeNode extends LeafAssertionTreeNode {

    public SslAssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String ret = "Require SSL Transport";
        Object uo = getUserObject();
        if (uo instanceof SslAssertion) {
            SslAssertion sa = (SslAssertion) getUserObject();
            if (SslAssertion.FORBIDDEN.equals(sa.getOption()))
                ret = "Forbid SSL transport";
            else if (SslAssertion.OPTIONAL.equals(sa.getOption()))
                ret = "Optional SSL transport";
        }
        return ret;
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
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
        return new SslPropertiesAction(this);
    }


    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new SslPropertiesAction(this);
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[]{});
    }


    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/ssl.gif";
    }
}