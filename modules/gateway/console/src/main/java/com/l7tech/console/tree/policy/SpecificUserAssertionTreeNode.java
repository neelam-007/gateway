package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.identity.SpecificUser;

/**
 * Class SpecificUserAssertionTreeNode.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SpecificUserAssertionTreeNode extends IdentityAssertionTreeNode<SpecificUser> {

    public SpecificUserAssertionTreeNode(SpecificUser assertion) {
        super(assertion);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName(final boolean decorate) {
        final String assertionName = "Authenticate User: " + getUserName() + " from [" + idProviderName() + "]";
        if(!decorate) return assertionName;

        return DefaultAssertionPolicyNode.addCommentToDisplayText(assertion,
            decorateName(assertionName));
    }

    private String getUserName() {
        SpecificUser specificUser = (SpecificUser)getUserObject();
        String userName = specificUser.getUserLogin();
        if (userName == null || "".equals(userName)) userName = specificUser.getUserName();
        if (userName == null || "".equals(userName)) userName = specificUser.getUserUid();
        return userName;
    }
}
