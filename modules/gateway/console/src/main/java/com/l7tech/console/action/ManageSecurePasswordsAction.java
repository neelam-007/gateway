package com.l7tech.console.action;

import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action that opens the Manage Secure Passwords window.
 */
public class ManageSecurePasswordsAction extends SecureAction {

    public ManageSecurePasswordsAction() {
        super(new AttemptedAnyOperation(EntityType.SECURE_PASSWORD), SECURE_PASSWORD_FEATURESET_NAME);
    }

    public String getName() {
        return "Manage Stored Passwords";
    }

    public String getDescription() {
        return "View and manage stored passwords";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/cert16.gif";
    }

    protected void performAction() {
        SecurePasswordManagerWindow skmw = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
        skmw.pack();
        Utilities.centerOnScreen(skmw);
        DialogDisplayer.display(skmw);
    }
}
