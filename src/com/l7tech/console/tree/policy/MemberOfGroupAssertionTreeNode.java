package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.identity.MemberOfGroup;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */

public class MemberOfGroupAssertionTreeNode extends LeafAssertionTreeNode {
    /**
     * The <code>MemberOfGroupAssertionTreeNode</code> is the composite
     * assertion node that represents the group membership.
     *
     * @param assertion the composite assertion
     */
    public MemberOfGroupAssertionTreeNode(MemberOfGroup assertion) {
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


    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Group membership "+ ((MemberOfGroup)getUserObject()).getGroupOid();
    }
}