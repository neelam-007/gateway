package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.identity.SpecificUser;

/**
 * Class SpecificUserAssertionTreeNode.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SpecificUserAssertionTreeNode extends IdentityAssertionTreeNode {

    public SpecificUserAssertionTreeNode(SpecificUser assertion) {
        super(assertion);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String userName = getUserName();

        if (isAnonymous()) {
            return userName;
        } else if (isAuthenticatedUser()) {
            return userName  + " " + idProviderName();
        } else if (isDelegated()) {
            return userName;
        }
        return "User: " + userName + " [" + idProviderName() + "]";
    }

    private String getUserName() {
        SpecificUser specificUser = (SpecificUser)getUserObject();
        String userName = specificUser.getUserLogin();
        if (userName == null || "".equals(userName)) userName = specificUser.getUserName();
        if (userName == null || "".equals(userName)) userName = specificUser.getUserUid();
        return userName;
    }

    private boolean isAnonymous() {
        String userName = getUserName();
        return IdentityPath.ANONYMOUS.equals(userName);
    }

    private boolean isAuthenticatedUser() {
        String userName = getUserName();
        return IdentityPath.AUTHENTICATED.equals(userName);
    }

    private boolean isDelegated() {
        String userName = getUserName();
        if (userName == null)
            return false;
        else
            return userName.startsWith(IdentityPath.CUSTOM_ACCESS_CONTROL);
    }

}