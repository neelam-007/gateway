package com.l7tech.external.assertions.portalbootstrap.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action that opens Enroll With Portal dialog.
 */
public class EnrollWithPortalAction extends SecureAction {

    public EnrollWithPortalAction( ) {
        super( new AttemptedUpdateAny( EntityType.USER ), "Enroll With Portal", "Enroll Gateway cluster with API Portal",
                "com/l7tech/console/resources/ManageUserAccounts16.png" );
    }

    @Override
    protected void performAction() {
        EnrollWithPortalDialog dlg = new EnrollWithPortalDialog( TopComponents.getInstance().getTopParent() );
        dlg.pack();
        Utilities.centerOnParentWindow( dlg );
        DialogDisplayer.display( dlg );
    }
}
