package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.identity.Group;


/**
 * The <code>ValidatePolicyAction</code> validates the service policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ValidatePolicyAction extends SecureAction {
    protected AssertionTreeNode rootNode;

    public ValidatePolicyAction() {
    }

    public ValidatePolicyAction(AssertionTreeNode node) {
        this.rootNode = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Validate";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Validate the service policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/validate.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
    }

    /**
     * Return the required roles for this action, one of the roles. The base
     * implementatoinm requires the strongest admin role.
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME};
    }
}
