package com.l7tech.console.action;

import com.l7tech.console.panels.AuditSinkGlobalPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action that opens the AuditSinkGlobalPropertiesDialog.
 */
public class ManageAuditSinkAction extends SecureAction {
    public ManageAuditSinkAction() {
        super(new AttemptedUpdateAny(EntityType.CLUSTER_PROPERTY), UI_MANAGE_LOG_SINKS);
    }

    @Override
    public String getName() {
        return "Manage Audit Sink";
    }

    @Override
    public String getDescription() {
        return "View and manage global audit sink";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        AuditSinkGlobalPropertiesDialog dlg = new AuditSinkGlobalPropertiesDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
