package com.l7tech.console.tree.policy;


import com.l7tech.console.action.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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

        Action da = new DeleteAssertionAction(this);
        da.setEnabled(canDelete());
        list.add(da);

        Action sp = new SavePolicyAction(this);
        //sp.setEnabled(false);
        list.add(sp);

        Action vp = new ValidatePolicyAction((AssertionTreeNode)getRoot());
        list.add(vp);

        Action mu = new AssertionMoveUpAction();
        mu.setEnabled(canMoveUp());
        list.add(mu);

        Action md = new AssertionMoveDownAction();
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
        return getParent() != null && getParent() != getRoot();
    }

    /**
     * Can the node move down in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveDown() {
        return false;
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

/**
 * Composite policy nodes extend this node
 */
abstract class CompositeAssertionTreeNode extends AssertionTreeNode {
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