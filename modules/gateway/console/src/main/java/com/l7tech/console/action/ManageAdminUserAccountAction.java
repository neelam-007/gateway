package com.l7tech.console.action;

import com.l7tech.console.panels.AdminUserAccountPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
* @author: wlui
*/
public class ManageAdminUserAccountAction extends SecureAction {
    public ManageAdminUserAccountAction() {
        super(new AttemptedAnyOperation(EntityType.CLUSTER_PROPERTY), "service:Admin");
    }

    @Override
    public String getName() {
        return "Manage Administrative User Account Policy";
    }

    @Override
    public String getDescription() {
        return "Create, edit, and remove administrative user account policies";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        AdminUserAccountPropertiesDialog mgrWindow = new AdminUserAccountPropertiesDialog(TopComponents.getInstance().getTopParent(), !isAuthorized());
        mgrWindow.pack();
        Utilities.centerOnScreen(mgrWindow);
        DialogDisplayer.display(mgrWindow);
    }

    @Override
    public boolean isAuthorized() {
        return super.isAuthorized();
    }
}
