package com.l7tech.console.action;

import com.l7tech.console.logging.ConsoleDialog;


/**
 * The <code>ConsoleAction</code> shows the applcation console
 * with log.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class ConsoleAction extends BaseAction {
    /**
     * @return the action name
     */
    public String getName() {
        return "Application Console";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Show application console with internal messages";
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
    public void performAction() {
        ConsoleDialog.getInstance().show();
    }
}
