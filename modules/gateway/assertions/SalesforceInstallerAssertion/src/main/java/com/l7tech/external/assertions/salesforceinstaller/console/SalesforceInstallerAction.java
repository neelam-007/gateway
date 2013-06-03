package com.l7tech.external.assertions.salesforceinstaller.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Pair;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class SalesforceInstallerAction extends SecureAction {

    public SalesforceInstallerAction() {
        super(new AttemptedCreate(EntityType.FOLDER));

        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                SalesforceInstallerAction.this.setActionValues();
            }
        });
    }

    @Override
    public String getName() {
        final Pair<String,Long> selectedFolderAndOid = SalesforceInstallerDialog.getSelectedFolderAndOid();
        if (selectedFolderAndOid.left != null) {
            return "Install Salesforce Toolkit in " + selectedFolderAndOid.left + SalesforceInstallerDialog.SALESFORCE_FOLDER;
        } else {
            return "Install Salesforce Toolkit";
        }
    }


    @Override
    protected void performAction() {
        SalesforceInstallerDialog dlg = new SalesforceInstallerDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }

//    @Override
//    protected String iconResource() {
//        return "com/l7tech/external/assertions/salesforce/salesforce-16x16.png";
//    }
}
