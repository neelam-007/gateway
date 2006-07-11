package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.security.rbac.RoleManagementDialog;
import com.l7tech.console.util.TopComponents;

import java.awt.*;

/**
 * User: megery
 * Date: Jul 7, 2006
 * Time: 9:49:40 AM
 */
public class ManageRolesAction extends SecureAction {
    public String getName() {
        return "Manage Access Controls";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "View Access Controls";
    }

    /**
     *
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Dialog dlg = new RoleManagementDialog(TopComponents.getInstance().getMainWindow());
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }
}
