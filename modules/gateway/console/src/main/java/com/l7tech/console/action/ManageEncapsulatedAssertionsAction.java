package com.l7tech.console.action;

import com.l7tech.console.panels.encass.EncapsulatedAssertionManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action that opens the Manage Encapsulated Assertions dialog.
 */
public class ManageEncapsulatedAssertionsAction extends SecureAction {

    public ManageEncapsulatedAssertionsAction() {
        super(new AttemptedAnyOperation(EntityType.ENCAPSULATED_ASSERTION), UI_MANAGE_ENCAPSULATED_ASSERTIONS);
    }

    @Override
    public String getName() {
        return "Manage Encapsulated Assertions";
    }

    @Override
    public String getDescription() {
        return "View/edit encapsulated assertion configurations.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/star16.gif";
    }

    @Override
    protected void performAction() {
        EncapsulatedAssertionManagerWindow dlg = new EncapsulatedAssertionManagerWindow( TopComponents.getInstance().getTopParent() );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }
}
