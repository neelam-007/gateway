/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.DashboardWindow;

/**
 * Action to show a {@link DashboardWindow} from the SSM.
 */
public class ShowDashboardAction extends SecureAction {
    public String getName() {
        return "Show Dashboard";
    }

    public String getDescription() {
        return "Shows Dashboard";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Refresh16.gif";
    }

    protected void performAction() {
        DashboardWindow win = new DashboardWindow();
        Utilities.centerOnScreen(win);
        win.setVisible(true);
    }
}
