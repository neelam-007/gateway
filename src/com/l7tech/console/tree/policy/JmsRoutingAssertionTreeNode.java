package com.l7tech.console.tree.policy;


import com.l7tech.console.action.JmsRoutingAssertionPropertiesAction;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class JmsRoutingAssertionTreeNode.
 * 
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 */
public class JmsRoutingAssertionTreeNode extends LeafAssertionTreeNode {

    public JmsRoutingAssertionTreeNode(RoutingAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        final String name = "Route to Jms Queue ";
        JmsRoutingAssertion ass = (JmsRoutingAssertion) getUserObject();
        if (ass.getEndpointOid() == null)
            return name + "(NOT YET SPECIFIED)";
        return name + (ass.getEndpointName() == null ? "(unnamed)" : ass.getEndpointName());
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new JmsRoutingAssertionPropertiesAction(this);
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new JmsRoutingAssertionPropertiesAction(this);
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/server16.gif";
    }

}