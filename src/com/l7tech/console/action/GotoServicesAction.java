package com.l7tech.console.action;

import javax.swing.*;

/**
 * Provide default details (text, images)lfor the <code>GotoServicesAction</code>
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class GotoServicesAction extends BaseAction {
    private final static String actionName = "Services";

    public GotoServicesAction() {
        super(actionName, RESOURCE_PATH + "/services16.png");
        putValue(Action.SHORT_DESCRIPTION, actionName);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/services32.png"));
        if (icon != null)
            putValue(BaseAction.LARGE_ICON, icon);
    }
}
