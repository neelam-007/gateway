package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

import java.util.Iterator;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */

class MemberOfGroupAssertionTreeNode extends LeafAssertionTreeNode {
    /**
     * The <code>MemberOfGroupAssertionTreeNode</code> is the composite
     * assertion node that represents the group membership.
     *
     * @param assertion the composite assertion
     */
    public MemberOfGroupAssertionTreeNode(IdentityAssertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
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