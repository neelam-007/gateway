package com.l7tech.console.action;

import com.l7tech.console.security.rbac.RoleManagerWindow;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.security.rbac.RoleManagementDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.util.ConfigFactory;

import javax.swing.*;

/**
 * User: megery
 * Date: Jul 7, 2006
 * Time: 9:49:40 AM
 */
public class ManageRolesAction extends SecureAction {
    public ManageRolesAction() {
        super(new AttemptedAnyOperation(EntityType.RBAC_ROLE), UI_RBAC_ROLE_EDITOR);
    }

    @Override
    public String getName() {
        return "Manage Roles";
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
        if (ConfigFactory.getBooleanProperty("com.l7tech.rbac.useNewRoleManager", Boolean.FALSE)) {
            dlg = new RoleManagerWindow(TopComponents.getInstance().getTopParent());
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
        } else {
            dlg = new RoleManagementDialog(TopComponents.getInstance().getTopParent());
        }
        DialogDisplayer.display(dlg);
    }
}
