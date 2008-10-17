package com.l7tech.console.action;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.EmailListenerManagerWindow;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.util.logging.Logger;

/**
 * Menu action for managing email listeners.
 */
public class ManageEmailListenersAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageEmailListenersAction.class.getName());

    public ManageEmailListenersAction() {
        super(new AttemptedAnyOperation(EntityType.EMAIL_LISTENER), UI_MANAGE_EMAIL_LISTENERS);
    }

    @Override
    public String getName() {
        return "Manage Email Listeners";
    }

    @Override
    public String getDescription() {
        return "View and manage email listeners";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        EmailListenerManagerWindow lsmw = new EmailListenerManagerWindow(TopComponents.getInstance().getTopParent());
        lsmw.pack();
        Utilities.centerOnScreen(lsmw);
        DialogDisplayer.display(lsmw);
    }
}
