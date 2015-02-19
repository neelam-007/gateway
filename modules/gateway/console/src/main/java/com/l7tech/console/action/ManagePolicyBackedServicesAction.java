package com.l7tech.console.action;

import com.l7tech.console.panels.polback.PolicyBackedServiceManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action that opens the Manage Encapsulated Assertions dialog.
 */
public class ManagePolicyBackedServicesAction extends SecureAction {

    private Runnable callbackOnClose;

    public ManagePolicyBackedServicesAction() {
        super(new AttemptedAnyOperation(EntityType.POLICY_BACKED_SERVICE), UI_MANAGE_ENCAPSULATED_ASSERTIONS);
    }

    /**
     * @param callbackOnClose callback to invoke when the Manage dialog has been closed, or null.
     */
    public ManagePolicyBackedServicesAction( Runnable callbackOnClose ) {
        super(new AttemptedAnyOperation(EntityType.POLICY_BACKED_SERVICE), UI_MANAGE_ENCAPSULATED_ASSERTIONS);
        this.callbackOnClose = callbackOnClose;
    }

    @Override
    public String getName() {
        return "Manage Policy-Backed Services";
    }

    @Override
    public String getDescription() {
        return "View/edit policy-backed services.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/polback16.gif";
    }

    @Override
    protected void performAction() {
        PolicyBackedServiceManagerWindow dlg = new PolicyBackedServiceManagerWindow( TopComponents.getInstance().getTopParent() );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, callbackOnClose);
    }
}
