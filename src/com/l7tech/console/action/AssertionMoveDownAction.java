package com.l7tech.console.action;

import javax.swing.*;

/**
 * The <code>AssertionMoveDownAction</code> is the action that moves
 * the assertion down in the policy assertion tree.
 * <p>
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class AssertionMoveDownAction extends BaseAction {
    private final static String actionName = "AssertionDown";

    public AssertionMoveDownAction() {
        super(actionName, RESOURCE_PATH + "/Down16.gif");
        putValue(Action.SHORT_DESCRIPTION, actionName);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Down16.gif"));
        if (icon != null)
            putValue(BaseAction.LARGE_ICON, icon);
    }
}
