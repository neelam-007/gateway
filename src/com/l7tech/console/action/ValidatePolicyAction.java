package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;


/**
 * The <code>ValidatePolicyAction</code> validates the service policy.
 *
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

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
    }
}
