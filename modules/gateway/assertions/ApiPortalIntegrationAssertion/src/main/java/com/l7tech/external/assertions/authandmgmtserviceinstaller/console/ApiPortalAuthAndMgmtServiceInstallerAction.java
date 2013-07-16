package com.l7tech.external.assertions.authandmgmtserviceinstaller.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 * A global action for invoking API Portal Authentication and Management Service Installer
 *
 * @author ghuang
 */
public class ApiPortalAuthAndMgmtServiceInstallerAction extends SecureAction {
    public static final String API_PORTAL_AUTH_AND_MGMT_SERVICE_NAME = "Install API Portal Authentication and Management Service";

    public ApiPortalAuthAndMgmtServiceInstallerAction() {
        super(new AttemptedCreate(EntityType.FOLDER));

        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                ApiPortalAuthAndMgmtServiceInstallerAction.this.setActionValues();
            }
        });
    }

    @Override
    public String getName() {
        String selectedFolderPath = ApiPortalAuthAndMgmtConfigurationDialog.getSelectedFolder().left;
        if (selectedFolderPath == null || selectedFolderPath.equals("/")) {
            return API_PORTAL_AUTH_AND_MGMT_SERVICE_NAME;
        } else {
            return API_PORTAL_AUTH_AND_MGMT_SERVICE_NAME + " in " + selectedFolderPath;
        }
    }

    @Override
    protected void performAction() {
        ApiPortalAuthAndMgmtConfigurationDialog dlg = new ApiPortalAuthAndMgmtConfigurationDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}