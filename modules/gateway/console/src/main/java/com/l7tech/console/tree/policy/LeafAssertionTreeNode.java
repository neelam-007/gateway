/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.action.SavePolicyAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Leaf policy nodes extend this node
 */
public abstract class LeafAssertionTreeNode<AT extends Assertion> extends AssertionTreeNode<AT> {
    private static final Logger log =
      Logger.getLogger(LeafAssertionTreeNode.class.getName());

    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public LeafAssertionTreeNode(AT assertion) {
        super(assertion);
    }

    @Override
    protected void doLoadChildren() {
    }

    @Override
    public Action[] getActions() {
        final Action[] supers = super.getActions();

        final Action preferred = getPreferredAction();
        if (preferred == null) return supers;

        final Action[] news = new Action[supers.length+1];
        news[0] = preferred;
        System.arraycopy(supers, 0, news, 1, supers.length);
        return news;
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    @Override
    public boolean receive(AbstractTreeNode node) {
        if (super.receive(node)) return true;
        
        JTree tree = (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            Assertion[] nass = node.asAssertions();
            if (nass != null) {
                for (int i = 0; i < nass.length; i++) {
                    Assertion nas = nass[i];
                    if (nas instanceof CustomAssertionHolder) {
                        nas = (CustomAssertionHolder) nas.clone();
                    }
                    AssertionTreeNode as = AssertionTreeNodeFactory.asTreeNode(nas);
                    final MutableTreeNode parent = (MutableTreeNode)getParent();
                    int index = parent.getIndex(this);
                    if (index == -1) {
                        throw new IllegalStateException("Unknown node to the tree model " + this);
                    }
                    model.insertNodeInto(as, parent, index + (i + 1));
                }
            } else {
                log.log(Level.WARNING, "The node has no associated assertion " + node);
            }
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
        return true;
    }

    /**
     * True whether this node accepts a node
     *
     * @param draggingNode the node to accept
     * @return true if policy template node
     */
    @Override
    public boolean accept(AbstractTreeNode draggingNode) {
        return super.accept(draggingNode) &&
                (draggingNode instanceof PolicyTemplateNode || getParent() != null) &&
                new SavePolicyAction(true).isAuthorized();
    }
}
