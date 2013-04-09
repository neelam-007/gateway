package com.l7tech.console.action;

import com.l7tech.console.security.rbac.SecurityZoneManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 *
 */
public class ManageSecurityZonesAction extends SecureAction {

    public ManageSecurityZonesAction() {
        super(new AttemptedAnyOperation(EntityType.SECURITY_ZONE), UI_MANAGE_SECURITY_ZONES);
    }

    @Override
    public String getName() {
        return "Manage Security Zones";
    }

    @Override
    public String getDescription() {
        return "View/edit security zones.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/RedYellowShield16.gif";
    }

    @Override
    protected void performAction() {
        SecurityZoneManagerWindow dlg = new SecurityZoneManagerWindow( TopComponents.getInstance().getTopParent() );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }
}
