package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Iterator;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
abstract class AssertionTreeNode extends DefaultMutableTreeNode {
    protected boolean hasLoadedChildren;

    AssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        if (!hasLoadedChildren) {
            loadChildren();
        }
        return super.getChildCount();
    }
    /**
     * subclasses override this method
     */
    protected abstract void loadChildren();

}


class LeafAssertionTreeNode extends AssertionTreeNode {
    public LeafAssertionTreeNode(Assertion assertion) {
        super(assertion);
         if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    protected void loadChildren() {
        hasLoadedChildren = true;
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
        CompositeAssertion assertion =  (CompositeAssertion)getUserObject();
        int index = 0;
        for (Iterator i= assertion.children(); i.hasNext();) {
            insert((AssertionTreeNodeFactory.asTreeNode((Assertion)i.next())), index++);
        }
        hasLoadedChildren = true;
    }


    /** @return  a string representation of the object.  */
    public String toString() {
        return getUserObject().getClass().getName();
    }
}