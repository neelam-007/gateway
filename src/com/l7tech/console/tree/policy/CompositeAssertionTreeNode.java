package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AddAllAssertionAction;
import com.l7tech.console.action.AddOneOrMoreAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Composite policy nodes extend this node
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class CompositeAssertionTreeNode extends AssertionTreeNode {
    private static final Logger log =
      Logger.getLogger(CompositeAssertionTreeNode.class.getName());

    /**
     * Instantiate composite assertion tree node wit the composite
     * assertion
     *
     * @param assertion = the composite assertion
     */
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

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    public boolean receive(AbstractTreeNode node) {
        if (true == super.receive(node)) {
            return true;
        }
        JTree tree =
          (JTree)ComponentRegistry.
          getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion nass = node.asAssertion();

            if (nass != null) {
                AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(nass);
                model.insertNodeInto(as, this, getChildCount());
            } else {
                log.log(Level.WARNING, "The node has no associated assertion " + node);
            }
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }

    protected void loadChildren() {
        CompositeAssertion assertion = (CompositeAssertion)getUserObject();
        int index = 0;
        children = null;
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