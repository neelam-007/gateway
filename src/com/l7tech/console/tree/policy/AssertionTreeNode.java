package com.l7tech.console.tree.policy;


import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class AssertionTreeNode extends AbstractTreeNode {

    AssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return this.toString();
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
        if (canDelete())  list.add(new DeleteAssertionAction(this));
       return (Action[]) list.toArray(new Action[]{});
   }


}


abstract class LeafAssertionTreeNode extends AssertionTreeNode {
    public LeafAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
    }
}

class CompositeAssertionTreeNode extends AssertionTreeNode {
    public CompositeAssertionTreeNode(CompositeAssertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
        CompositeAssertion assertion = (CompositeAssertion) getUserObject();
        int index = 0;
        for (Iterator i = assertion.children(); i.hasNext();) {
            insert((AssertionTreeNodeFactory.asTreeNode((Assertion) i.next())), index++);
        }
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