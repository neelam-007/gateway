package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.action.AddAllAssertionAction;
import com.l7tech.console.action.AddOneOrMoreAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Composite policy nodes extend this node
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class CompositeAssertionTreeNode extends AssertionTreeNode {
    public CompositeAssertionTreeNode(CompositeAssertion assertion) {
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
        Action a = new AddAllAssertionAction(this);
        list.add(a);
        a = new AddOneOrMoreAssertionAction(this);
        list.add(a);

        return (Action[])list.toArray(new Action[]{});
    }

    protected void loadChildren() {
        CompositeAssertion assertion = (CompositeAssertion)getUserObject();
        int index = 0;
        for (Iterator i = assertion.children(); i.hasNext();) {
            insert((AssertionTreeNodeFactory.asTreeNode((Assertion)i.next())), index++);
        }
    }

    /**
     * By default, the composite node accepts a node.
     *
     * @param node the node to accept
     * @return always true
     */
    public boolean accept(AbstractTreeNode node) {
        return true;
    }

    /**
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/folder.gif";
    }


    /** @return  a string representation of the object.  */
    public String toString() {
        return getUserObject().getClass().getName();
    }
}