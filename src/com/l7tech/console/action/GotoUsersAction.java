package com.l7tech.console.action;

import javax.swing.*;

/**
 * Provide default details (text, images)lfor the <code>GotoUsersAction</code>
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class GotoUsersAction extends BaseAction {
    private final static String actionName = "Users";

    public GotoUsersAction() {
        super(actionName, RESOURCE_PATH + "/user16.png");
        putValue(Action.SHORT_DESCRIPTION, actionName);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/user32.png"));
        if (icon != null)
            putValue(BaseAction.LARGE_ICON, icon);
    }
}
