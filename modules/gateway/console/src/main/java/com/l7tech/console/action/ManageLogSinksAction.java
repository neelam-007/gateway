package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.LogSinkManagerWindow;
import com.l7tech.console.util.TopComponents;

import java.util.logging.Logger;

/**
 * Action to invoke LogSinkManagerWindow.
 */
public class ManageLogSinksAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageLogSinksAction.class.getName());

    public ManageLogSinksAction() {
        super(new AttemptedAnyOperation(EntityType.LOG_SINK), UI_MANAGE_LOG_SINKS);
    }

    @Override
    public String getName() {
        return "Manage Log/Audit Sinks";
    }

    @Override
    public String getDescription() {
        return "View and manage log and audit sinks";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        LogSinkManagerWindow lsmw = new LogSinkManagerWindow(TopComponents.getInstance().getTopParent());
        lsmw.pack();
        Utilities.centerOnScreen(lsmw);
        DialogDisplayer.display(lsmw);
    }
}
