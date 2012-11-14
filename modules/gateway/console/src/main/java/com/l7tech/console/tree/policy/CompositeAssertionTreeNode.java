/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AddIdentityAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Composite Assertion policy nodes extend this node
 */
public abstract class CompositeAssertionTreeNode<AT extends CompositeAssertion> extends AssertionTreeNode<AT> {
    private static final Logger log =
      Logger.getLogger(CompositeAssertionTreeNode.class.getName());

    /**
     * Instantiate composite assertion tree node wit the composite
     * assertion
     *
     * @param assertion = the composite assertion
     */
    public CompositeAssertionTreeNode(AT assertion) {
        super(assertion);
        this.setAllowsChildren(true);
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    @Override
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
        if (super.receive(node)) return true;

        JTree tree = (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion[] nass = node.asAssertions();
            for (Assertion assertion : nass) {
                if (assertion instanceof CustomAssertionHolder) {
                    assertion = (CustomAssertionHolder) assertion.clone();
                }
                AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(assertion);
                model.insertNodeInto(as, this, position);
                tree.scrollPathToVisible(new TreePath(as.getPath()));
            }
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }

    @Override
    protected void doLoadChildren() {
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
    @Override
    public boolean accept(AbstractTreeNode node) {
        return super.accept(node);
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getUserObject().getClass().getName();
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    @Override
    public boolean canDelete() {
        return getParent() != null && super.canDelete();
    }

    /**
     * specify this node image resource
     */
    @Override
    protected String iconResource(boolean open) {
        if (open) return assertion.meta().get(AssertionMetadata.POLICY_NODE_ICON_OPEN);
        return assertion.meta().get(AssertionMetadata.POLICY_NODE_ICON);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        list.addAll(Arrays.asList(super.getActions()));
        Action a = new AddIdentityAssertionAction(this);
        list.add(a);
        return list.toArray(new Action[list.size()]);
    }

    @Override
    public String getName(boolean decorate) {
        return DefaultAssertionPolicyNode.getNameFromMeta(asAssertion(), decorate, true);
    }
}
