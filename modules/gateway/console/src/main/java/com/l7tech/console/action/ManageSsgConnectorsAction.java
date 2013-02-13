package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.SsgConnectorManagerWindow;
import com.l7tech.console.util.TopComponents;

import java.util.logging.Logger;

/**
 * Action to invoke SsgConnectorManagerWindow.
 */
public class ManageSsgConnectorsAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageSsgConnectorsAction.class.getName());

    public ManageSsgConnectorsAction() {
        super(new AttemptedAnyOperation(EntityType.SSG_CONNECTOR), "service:Admin");
    }

    public String getName() {
        return "Manage Listen Ports";
    }

    public String getDescription() {
        return "View and manage listen ports";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    protected void performAction() {
        SsgConnectorManagerWindow pkmw = new SsgConnectorManagerWindow(TopComponents.getInstance().getTopParent());
        pkmw.pack();
        Utilities.centerOnScreen(pkmw);
        DialogDisplayer.display(pkmw);
    }
}
