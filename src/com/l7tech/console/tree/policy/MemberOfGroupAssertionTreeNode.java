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

class MemberOfGroupAssertionTreeNode extends CompositeAssertionTreeNode {
    /**
     * The <code>MemberOfGroupAssertionTreeNode</code> is the composite
     * assertion node that represents the group membership.
     *
     * @param assertion the composite assertion
     */
    public MemberOfGroupAssertionTreeNode(CompositeAssertion assertion) {
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
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/group16.png";
    }


    /** @return  a string representation of the object.  */
    public String toString() {
        return getUserObject().getClass().getName();
    }
}