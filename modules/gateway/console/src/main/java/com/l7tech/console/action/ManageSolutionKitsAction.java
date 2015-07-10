package com.l7tech.console.action;

import com.l7tech.console.panels.solutionkit.ManageSolutionKitsDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action used to display {@link com.l7tech.console.panels.solutionkit.ManageSolutionKitsDialog}.
 */
public class ManageSolutionKitsAction extends SecureAction {

    public ManageSolutionKitsAction() {
        // todo (kpak) - Set correct entity type and feature set name.
        super(new AttemptedAnyOperation(EntityType.SOLUTION_KIT), ADMIN_FEATURESET_NAME);
    }

    @Override
    public String getName() {
        return "Manage Solution Kits";
    }

    @Override
    public String getDescription() {
        return "Install, upgrade, and uninstall solution kits.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/check_list_in_box_16.png";
    }

    @Override
    protected void performAction() {
        ManageSolutionKitsDialog dlg = new ManageSolutionKitsDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }
}