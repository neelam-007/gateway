package com.l7tech.console.action;

import com.l7tech.console.AboutBox;


/**
 * The <code>AboutAction</code> shows the about the applcation
 * information.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AboutAction extends SecureAction {
    /**
     * @return the action name
     */
    public String getName() {
        return "About";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View SecureSpan Manager information";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/About16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        AboutBox.showDialog(null);
    }
}
