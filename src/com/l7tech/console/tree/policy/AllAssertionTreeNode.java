package com.l7tech.console.tree.policy;


import com.l7tech.console.action.AddIdentityAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.composite.AllAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class AllAssertionTreeNode
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AllAssertionTreeNode extends CompositeAssertionTreeNode {
    /**
     * The <code>AllAssertionTreeNode</code> is the composite
     * assertion node that represents the 'AND' conjunction.
     *
     * @param assertion the composite assertion
     */
    public AllAssertionTreeNode(AllAssertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
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
        Action a = new AddIdentityAssertionAction(this);
        list.add(a);
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return getParent() != null;
    }

    /**
     * By default, this node accepts leaf nodes.
     *
     * @param node the node to accept
     * @return true if sending node is leaf
     */
    public boolean accept(AbstractTreeNode node) {
        return node.isLeaf();
    }

    /**
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }


    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "All assertions must evaluate to true";
    }
}