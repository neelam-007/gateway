package com.l7tech.external.assertions.whichmodule.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action to open Manage Demo Generic Entities dialog.
 */
public class ManageDemoGenericEntitiesAction extends SecureAction {
    public ManageDemoGenericEntitiesAction() {
        super(new AttemptedAnyOperation(EntityType.GENERIC));
    }

    public String getName() {
        return "Manage Demo Generic Entities";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Bean16.gif";
    }

    @Override
    protected void performAction() {
        ManageDemoGenericEntitiesDialog dlg = new ManageDemoGenericEntitiesDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
