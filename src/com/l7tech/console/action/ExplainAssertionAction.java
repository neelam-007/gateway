package com.l7tech.console.action;



/**
 * The <code>ExplainAssertionAction</code> is the action that invokes
 * the context sensitive assertion 'explain' help.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ExplainAssertionAction extends SecureAction {
    /**
     * @return the action name
     */
    public String getName() {
        return "Explain";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Explain the assertion";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Help16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
    }
}
