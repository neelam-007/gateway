package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class OAuthInstallerAction extends SecureAction {

    public OAuthInstallerAction() {
        super(new AttemptedCreate(EntityType.FOLDER));

        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                OAuthInstallerAction.this.setActionValues();
            }
        });
    }

    @Override
    public String getName() {
        final Pair<String,Goid> selectedFolderAndGoid = OAuthInstallerTaskDialog.getSelectedFolderAndGoid();
        if (selectedFolderAndGoid.left != null) {
            return "Install OAuth Toolkit in " + selectedFolderAndGoid.left + OAuthInstallerTaskDialog.OAUTH_FOLDER;
        } else {
            return "Install OAuth Toolkit";
        }
    }


    @Override
    protected void performAction() {
        OAuthInstallerTaskDialog dlg = new OAuthInstallerTaskDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/oauth.png";
    }
}
