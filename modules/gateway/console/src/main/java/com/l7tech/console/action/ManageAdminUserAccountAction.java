package com.l7tech.console.action;

import com.l7tech.console.panels.AdminUserAccountPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.*;
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
        if(!super.isAuthorized()) return false;

        // check permission for accessing the cluster properties associated with the configurations
        // test for permission to get "logon.maxAllowableAttempts"
        
        for (Permission perm : getSecurityProvider().getUserPermissions()) {
            if (perm.getOperation() == OperationType.READ) {
                EntityType etype = perm.getEntityType();
                if (etype == EntityType.ANY)
                    return true;
                if( etype == EntityType.CLUSTER_PROPERTY){
                    if(perm.getScope().isEmpty()) return true;
                    for (ScopePredicate predicate : perm.getScope()) {
                        if(predicate instanceof AttributePredicate){
                            if(((AttributePredicate)predicate).getValue().equals("logon.maxAllowableAttempts"))
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
