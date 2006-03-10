/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.dashboard.DashboardWindow;

import java.util.ResourceBundle;

/**
 * Action to show a {@link com.l7tech.console.panels.dashboard.DashboardWindow} from the SSM.
 */
public class ShowDashboardAction extends SecureAction {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.DashboardWindow");

    public String getName() {
        return resources.getString("menu.text");
    }

    public String getDescription() {
        return resources.getString("menu.description");
    }

    protected String iconResource() {
        return resources.getString("menu.icon");
    }

    protected void performAction() {
        DashboardWindow win = new DashboardWindow();
        Utilities.centerOnScreen(win);
        win.setVisible(true);
    }
}
