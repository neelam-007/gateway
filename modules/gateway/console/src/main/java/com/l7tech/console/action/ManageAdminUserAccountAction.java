package com.l7tech.console.action;

import com.l7tech.console.panels.AdminUserAccountPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.util.EnumSet;

/**
 * @author: wlui
 */
public class ManageAdminUserAccountAction extends SecureAction {
    public ManageAdminUserAccountAction() {
        super(null, "service:Admin");
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
        return "com/l7tech/console/resources/policy16.gif";
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
        if (!super.isAuthorized()) return false;

        AttemptedOperation op = new AttemptedUpdate(EntityType.CLUSTER_PROPERTY, new ClusterProperty("logon.maxAllowableAttempts", ""));
        AttemptedOperation op1 = new AttemptedUpdate(EntityType.CLUSTER_PROPERTY, new ClusterProperty("logon.lockoutTime", ""));
        AttemptedOperation op2 = new AttemptedUpdate(EntityType.CLUSTER_PROPERTY, new ClusterProperty("logon.sessionExpiry", ""));
        AttemptedOperation op3 = new AttemptedUpdate(EntityType.CLUSTER_PROPERTY, new ClusterProperty("logon.inactivityPeriod", ""));
        boolean can = canAttemptOperation(op);
        can = can && canAttemptOperation(op1);
        can = can && canAttemptOperation(op2);
        can = can && canAttemptOperation(op3);
        return can;
    }
}
