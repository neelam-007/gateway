package com.l7tech.console.tree.policy;


import com.l7tech.console.action.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.ComponentManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Enumeration;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class AssertionTreeNode extends AbstractTreeNode {

    AssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the assertion this node represents
     */
    public final Assertion asAssertion() {
        return (Assertion)getUserObject();
    }

    /**
     * @return the node name that is displayed
     */
    abstract public String getName();

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.addAll(Arrays.asList(super.getActions()));

        Action sp = new SavePolicyAction(this);
        //sp.setEnabled(false);
        list.add(sp);

        Action da = new DeleteAssertionAction(this);
        da.setEnabled(canDelete());
        list.add(da);

        Action ea = new ExplainAssertionAction();
        ea.setEnabled(canDelete());
        list.add(ea);

        Action vp = new ValidatePolicyAction((AssertionTreeNode)getRoot());
        list.add(vp);

        Action mu = new AssertionMoveUpAction(this);
        mu.setEnabled(canMoveUp());
        list.add(mu);

        Action md = new AssertionMoveDownAction(this);
        md.setEnabled(canMoveDown());
        list.add(md);

        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Can the node move up in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveUp() {
        return
          getParent() != null && getPreviousSibling() != null;
    }

    /**
     * Can the node move down in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveDown() {
        return getNextSibling() != null;
    }

    /**
     * Swap the position of this node with the target
     * node.
     */
    public void swap(AssertionTreeNode target) {
        JTree tree = ComponentManager.getInstance().getPolicyTree();
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)this.getParent();
        int indexThis = parent.getIndex(this);
        int indexThat = parent.getIndex(target);
        parent.insert(this, indexThat);
        parent.insert(target, indexThis);
        model.nodeStructureChanged(parent);
        // todo: do this with tree model listener
        List newChildren = new ArrayList();
        CompositeAssertion ca = (CompositeAssertion)((AssertionTreeNode)parent).asAssertion();
        for (Enumeration e = parent.children(); e.hasMoreElements();){
            AssertionTreeNode an = (AssertionTreeNode)e.nextElement();
            newChildren.add(an.asAssertion());
        }
        ca.setChildren(newChildren);
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     */
    public void receive(AbstractTreeNode node) {
    }


    /**
     * Does the assertion node accepts the abstract tree node
     *
     * @param node the node to accept
     * @return true if the node can be accepted, false otherwise
     */
    public abstract boolean accept(AbstractTreeNode node);
}

/**
 * Leaf policy nodes extend this node
 */
abstract class LeafAssertionTreeNode extends AssertionTreeNode {
    public LeafAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
    }

    /**
     * By default, the leaf node never accepts a node.
     *
     * @param node the node to accept
     * @return always false
     */
    public boolean accept(AbstractTreeNode node) {
        return false;
    }
}
