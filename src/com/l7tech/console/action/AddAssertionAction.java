package com.l7tech.console.action;

import javax.swing.*;

/**
 * The <code>AddAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class AddAssertionAction extends BaseAction {
    private final static String actionName = "AssignAssertion";

    public AddAssertionAction() {
        super(actionName, RESOURCE_PATH + "/assign.gif");
        putValue(Action.SHORT_DESCRIPTION, actionName);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/assign.gif"));
        if (icon != null)
            putValue(BaseAction.LARGE_ICON, icon);
    }
}
