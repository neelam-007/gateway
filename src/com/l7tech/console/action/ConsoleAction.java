package com.l7tech.console.action;

import com.l7tech.console.logging.ConsoleDialog;

import java.io.File;


/**
 * The <code>ConsoleAction</code> shows the application console
 * with log.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ConsoleAction extends SecureAction {
    /**
     * @return the action name
     */
    public String getName() {
        return "Manager Log";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Show Manager log with diagniostic messages";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Information16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        //new ConsoleDialog(new File("find the ssm.log"));
    }
}
