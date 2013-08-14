package com.l7tech.console.action;

import com.l7tech.console.security.rbac.RoleManagerWindow;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.security.rbac.RoleManagementDialog;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;

/**
 * User: megery
 * Date: Jul 7, 2006
 * Time: 9:49:40 AM
 */
public class ManageRolesAction extends SecureAction {
    // temp flag to indicate whether new/old role editor should be used
    // TODO delete me
    final boolean useNewRoleEditor;
    public ManageRolesAction(final boolean useNewRoleEditor) {
        super(new AttemptedAnyOperation(EntityType.RBAC_ROLE), UI_RBAC_ROLE_EDITOR, true);
        this.useNewRoleEditor = useNewRoleEditor;
        setActionValues();
    }

    @Override
    public String getName() {
        return useNewRoleEditor ? "Manage Roles" : "LEGACY ROLE MANAGER";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "View Roles";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        JDialog dlg;
        if (useNewRoleEditor) {
            dlg = new RoleManagerWindow(TopComponents.getInstance().getTopParent());
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
        } else {
            dlg = new RoleManagementDialog(TopComponents.getInstance().getTopParent());
        }
        DialogDisplayer.display(dlg);
    }
}
