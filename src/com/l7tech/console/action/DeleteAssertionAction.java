package com.l7tech.console.action;

import javax.swing.*;

/**
 * The <code>DeleteAssertionAction</code> action deletes
 * the assertion from the target policy.
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class DeleteAssertionAction extends BaseAction {
    private final static String actionName = "DeleteAssertion";

    public DeleteAssertionAction() {
        super(actionName, RESOURCE_PATH + "/delete.gif");
        putValue(Action.SHORT_DESCRIPTION, actionName);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/delete.gif"));
        if (icon != null)
            putValue(BaseAction.LARGE_ICON, icon);
    }
}
