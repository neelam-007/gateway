package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.composite.AllAssertion;

/**
 * Class AllAssertionTreeNode
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
class AllAssertionTreeNode extends CompositeAssertionTreeNode {
    /**
     * The <code>MemberOfGroupAssertionTreeNode</code> is the composite
     * assertion node that represents the group membership.
     *
     * @param assertion the composite assertion
     */
    public AllAssertionTreeNode(AllAssertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }


    /** @return  a string representation of the object.  */
    public String toString() {
        return getUserObject().getClass().getName();
    }
}