package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.action.SslPropertiesAction;

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
        return "Require SSL transport";
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
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.addAll(Arrays.asList(super.getActions()));
        Action a = new SslPropertiesAction(this);
        list.add(a);
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