package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.console.util.IconManager2;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.Iterator;
import java.awt.*;

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

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    public Icon getIcon() {
        Image image = IconManager2.getInstance().getIcon(iconResource());
        if (image !=null) {
            return new ImageIcon(image);
        }
        return null;
    }

    /**
     * subclasses override this method specifying
     */
    protected abstract String iconResource();
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

    /**
     * specify this node image resource
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
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

    /**
     * specify this node image resource
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }


    /** @return  a string representation of the object.  */
    public String toString() {
        return getUserObject().getClass().getName();
    }
}