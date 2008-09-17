/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAny;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.console.panels.dashboard.DashboardWindow;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

/**
 * Action to show a {@link com.l7tech.console.panels.dashboard.DashboardWindow} from the SSM.
 */
public class ShowDashboardAction extends SecureAction {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.DashboardWindow");
    private DashboardWindow dashboardWindow;

    public ShowDashboardAction() {
        super(new AttemptedReadAny(EntityType.METRICS_BIN), UI_DASHBOARD_WINDOW);
    }

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
        DashboardWindow dw = dashboardWindow;
        if (dw == null) {
            dw = new DashboardWindow();
            dw.addWindowListener(new WindowAdapter() {
                public void windowClosed(final WindowEvent e) {
                    dashboardWindow = null;
                }

                public void windowClosing(final WindowEvent e) {
                    dashboardWindow = null;
                }
            });
            dw.pack();
            dw.setVisible(true);
            dashboardWindow = dw;
        }
        else {
            // ensure not minimized
            dw.setState(Frame.NORMAL);
            dw.toFront();
        }
    }

    public void onLogon(LogonEvent e) {
        super.onLogon(e);
        DashboardWindow dw = dashboardWindow;
        if (dw != null) {
            dw.onLogon(e);
        }
    }

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        DashboardWindow dw = dashboardWindow;
        if (dw != null) {
            dw.onLogoff(e);
        }
    }
}
