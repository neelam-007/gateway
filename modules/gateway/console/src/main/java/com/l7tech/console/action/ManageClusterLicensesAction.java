/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.action;

import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import java.awt.*;

/**
 * Action to manage the Gateway's license from the SSM.
 */
public class ManageClusterLicensesAction extends SecureAction {
    public ManageClusterLicensesAction() {
        super(null);
    }

    public String getName() {
        return "Manage Gateway Licenses";
    }

    public String getDescription() {
        return "View/Install/Remove Gateway Cluster Licenses.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/cert16.gif";
    }

    protected void performAction() {
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        ManageLicensesDialog dlg = new ManageLicensesDialog(mainWindow);

        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setModal(true);
        DialogDisplayer.display(dlg);
    }
}
