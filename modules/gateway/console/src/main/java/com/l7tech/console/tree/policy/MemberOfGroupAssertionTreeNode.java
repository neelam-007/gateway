package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.identity.MemberOfGroup;

/**
 * Class AssertionTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */

public class MemberOfGroupAssertionTreeNode extends IdentityAssertionTreeNode<MemberOfGroup> {
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
        // assume that the strid is a valuable piece of information if it;s something else than a number
        String strid = assertion.getGroupId();
        try {
            Long.parseLong(strid);
            tooltip = null;
        } catch (NumberFormatException nfe) {
            tooltip = strid;
        }
    }

    /**
     * specify this node image resource
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/group16.png";
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName(final boolean decorate) {
        final String assertionName = "Authenticate Group: " + ((MemberOfGroup) getUserObject()).getGroupName() +
            " from [" + idProviderName() + "]";
        if(!decorate) return assertionName;

        final String displayText = decorateName(assertionName);
        return DefaultAssertionPolicyNode.addCommentToDisplayText(assertion,
                displayText);
    }
}