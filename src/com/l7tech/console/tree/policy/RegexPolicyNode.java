package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Regex;
import com.l7tech.console.action.RegexPropertiesAction;
import com.l7tech.console.action.SslPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class RegexPolicyNode is a policy node that corresponds the
 * <code>Regex</code> assertion.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RegexPolicyNode extends LeafAssertionTreeNode {

    public RegexPolicyNode(Regex assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Reqular Expression";
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
        return new RegexPropertiesAction(this);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new RegexPropertiesAction(this);
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
        return "com/l7tech/console/resources/regex16.gif";
    }
}