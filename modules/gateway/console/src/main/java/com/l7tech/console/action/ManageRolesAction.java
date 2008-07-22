package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.console.security.rbac.RoleManagementDialog;
import com.l7tech.console.util.TopComponents;

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

    public String getName() {
        return "Manage Roles";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "View Roles";
    }

    /**
     *
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        JDialog dlg = new RoleManagementDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
