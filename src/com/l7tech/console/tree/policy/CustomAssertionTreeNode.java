package com.l7tech.console.tree.policy;


import com.l7tech.console.action.CustomAssertionPropertiesAction;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.Category;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>CustomAssertionTreeNode</code> contains the custom
 * assertion element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertionTreeNode extends LeafAssertionTreeNode {

    public CustomAssertionTreeNode(CustomAssertionHolder assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        final CustomAssertion ca = cha.getCustomAssertion();
        String name = ca.getName();
        if (name == null) {
            name = "Unspecified custom assertion (class '" + ca.getClass() + "'";
        }
        return name;
    }

    /**
     * @return the custom assertion category
     */
    public Category getCategory() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        return cha.getCategory();
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
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new CustomAssertionPropertiesAction(this);
    }


    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new CustomAssertionPropertiesAction(this);
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
        return "com/l7tech/console/resources/custom.gif";
    }
}