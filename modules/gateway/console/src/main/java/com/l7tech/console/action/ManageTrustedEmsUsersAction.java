package com.l7tech.console.action;

import com.l7tech.console.panels.TrustedEmsManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 *
 */
public class ManageTrustedEmsUsersAction extends SecureAction {
    public ManageTrustedEmsUsersAction() {
        super(new AttemptedReadAll(EntityType.TRUSTED_ESM), "service:EnterpriseManageable");
    }

    @Override
    public String getName() {
        return "Manage ESM User Mappings";
    }

    @Override
    public String getDescription() {
        return "View and edit Enterprise Service Manager registrations and user mappings";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/cert16.gif";
    }

    @Override
    protected void performAction() {
        TrustedEmsManagerWindow dlg = new TrustedEmsManagerWindow(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
