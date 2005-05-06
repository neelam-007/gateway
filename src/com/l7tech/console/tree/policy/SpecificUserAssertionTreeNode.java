package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

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
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        //Action a = new IdentityPolicyAction(this);
        //list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return null;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String userName = getUserName();

        if (isAnonymous()) {
            return userName;
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

    private boolean isDelegated() {
        String userName = getUserName();
        if (userName == null)
            return false;
        else
            return userName.startsWith(IdentityPath.CUSTOM_ACCESS_CONTROL);
    }

}