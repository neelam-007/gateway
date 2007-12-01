package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.panels.LogSinkManagerWindow;
import com.l7tech.console.util.TopComponents;

import java.util.logging.Logger;

/**
 * Action to invoke LogSinkManagerWindow.
 */
public class ManageLogSinksAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageSsgConnectorsAction.class.getName());

    public ManageLogSinksAction() {
        super(new AttemptedAnyOperation(EntityType.SSG_CONNECTOR));
    }

    public String getName() {
        return "Manage Log Sinks";
    }

    public String getDescription() {
        return "View and manage log sinks";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    protected void performAction() {
        LogSinkManagerWindow lsmw = new LogSinkManagerWindow(TopComponents.getInstance().getTopParent());
        lsmw.pack();
        Utilities.centerOnScreen(lsmw);
        DialogDisplayer.display(lsmw);
    }
}
