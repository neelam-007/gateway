package com.l7tech.external.assertions.policybundleexporter.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Used to load the Tasks > Additional Task menu to export contents of a folder as a Policy Bundle Installer.
 */
public class PolicyBundleExporterAction extends SecureAction {

    public PolicyBundleExporterAction() {
        super(new AttemptedCreate(EntityType.FOLDER));
    }

    @Override
    public String getName() {
        return "Create Policy Bundle Installer";
    }


    @Override
    protected void performAction() {
        CreateBundleInstallerDialog dlg = new CreateBundleInstallerDialog(TopComponents.getInstance().getTopParent());
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/external/assertions/policybundleexporter/console/rectangle_lines_down_arrow.png";
    }
}
