package com.l7tech.console.action;




/**
 * The <code>PolicyIdentityViewAction</code> action switches the policy
 * view to the identity based policy view.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyIdentityViewAction extends BaseAction {

    public PolicyIdentityViewAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Identity view";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Toggle the policy view policy/identity";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/identity.png";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
    }
}
