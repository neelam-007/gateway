package com.l7tech.console.poleditor;

import com.l7tech.console.action.BaseAction;


/**
 * The <code>PolicyIdentityViewAction</code> action switches the policy
 * view to the identity based policy view.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyViewAction extends BaseAction {

    public PolicyViewAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Policy View";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View policy assertions by assertion flow";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/policy16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
    }
}
