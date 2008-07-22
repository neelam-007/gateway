/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.LicenseDialog;
import com.l7tech.console.util.TopComponents;

import java.awt.*;

/**
 * Action to manage the Gateway's license from the SSM.
 */
public class ManageClusterLicensesAction extends SecureAction {
    public ManageClusterLicensesAction() {
        super(null);
    }

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
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        final String ssgUrl = TopComponents.getInstance().ssgURL();
        LicenseDialog dlg = new LicenseDialog(mainWindow, ssgUrl);

        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setModal(true);
        DialogDisplayer.display(dlg);
    }
}
