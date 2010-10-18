package com.l7tech.console.action;

import com.l7tech.console.panels.GlobalResourcesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.xml.SchemaValidation;

/**
 * Secure action for management of global resources.
 */
public class ManageGlobalResourcesAction extends SecureAction {
    
    public ManageGlobalResourcesAction() {
        super(new AttemptedAnyOperation( EntityType.RESOURCE_ENTRY), SchemaValidation.class);
    }

    @Override
    public String getName() {
        return "Manage Global Resources";
    }

    @Override
    public String getDescription() {
        return "View/edit shared resources.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/xmlObject16.gif";
    }

    @Override
    protected void performAction() {
        GlobalResourcesDialog dlg = new GlobalResourcesDialog( TopComponents.getInstance().getTopParent() );
        DialogDisplayer.display(dlg);
    }
}
