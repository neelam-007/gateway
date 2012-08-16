package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

public class OAuthInstallerAction extends SecureAction {

    public OAuthInstallerAction() {
        super(new AttemptedCreate(EntityType.FOLDER));
    }

    @Override
    public String getName() {
        return "Install OAuth Toolkit";
    }

    @Override
    protected void performAction() {
        OAuthInstallerTaskDialog dlg = new OAuthInstallerTaskDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
