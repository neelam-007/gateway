/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.action;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.LicenseDialog;
import com.l7tech.console.MainWindow;
import com.l7tech.common.gui.util.Utilities;

/**
 * Action to manage the Gateway's license from the SSM.
 */
public class ManageClusterLicensesAction extends SecureAction {
    public String getName() {
        return "Manage Gateway License";
    }

    public String getDescription() {
        return "View/Install SecureSpan Gateways Cluster License.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/cert16.gif";
    }

    protected void performAction() {
        // TODO find the real gateway name
        final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();
        final String ssgUrl = mainWindow.ssgURL();
        LicenseDialog dlg = new LicenseDialog(mainWindow, ssgUrl);

        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setModal(true);
        dlg.setVisible(true);
    }
}
