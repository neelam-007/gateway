package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Composite policy nodes extend this node
 *
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
        this.setAllowsChildren(true);
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    public boolean receive(AbstractTreeNode node) {
        return receive(node, getChildCount());
    }

    /**
     * Receive the abstract tree node at the given position
     *
     * @param node the node to receive
     * @param position the node position
     */
    public boolean receive(AbstractTreeNode node, int position) {
        if (true == super.receive(node)) {
            return true;
        }
        JTree tree = (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion[] nass = node.asAssertions();
            for (int i = 0; i < nass.length; i++) {
                Assertion nas = nass[i];
                AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(nas);
                model.insertNodeInto(as, this, position);
            }
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }


    protected void loadChildren() {
        CompositeAssertion assertion = (CompositeAssertion)getUserObject();
        children = null;
        LoadChildrenStrategy loader = LoadChildrenStrategy.newStrategy(this);
        loader.loadChildren(this, assertion);
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


    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return getUserObject().getClass().getName();
    }
}