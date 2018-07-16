package com.l7tech.external.assertions.salesforceinstaller.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class SalesforceInstallerAction extends SecureAction {

    public SalesforceInstallerAction() {
        super(new AttemptedCreate(EntityType.TRUSTED_CERT));

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
        return "Install Execute Salesforce Operation Assertion";
    }


    @Override
    protected void performAction() {
        SalesforceInstallerDialog dlg = new SalesforceInstallerDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/salesforce_16.png";
    }
}