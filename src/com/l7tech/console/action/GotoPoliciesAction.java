package com.l7tech.console.action;

import javax.swing.*;

/**
 * Provide default details (text, images)lfor the <code>GotoPoliciesAction</code>
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class GotoPoliciesAction extends BaseAction {
    private final static String actionName = "Policies";

    public GotoPoliciesAction() {
        super(actionName, RESOURCE_PATH + "/policy16.gif");
        putValue(Action.SHORT_DESCRIPTION, actionName);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/policy32.gif"));
        if (icon != null)
            putValue(BaseAction.LARGE_ICON, icon);
    }
}
