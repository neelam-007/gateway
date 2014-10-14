package com.l7tech.external.assertions.simplepolicybundleinstaller.console;

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

public class SimplePolicyBundleInstallerAction extends SecureAction {

    public SimplePolicyBundleInstallerAction() {
        super(new AttemptedCreate(EntityType.FOLDER));

        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                SimplePolicyBundleInstallerAction.this.setActionValues();
            }
        });
    }

    @Override
    public String getName() {
        final Pair<String,Goid> selectedFolderAndGoid = SimplePolicyBundleInstallerDialog.getSelectedFolderAndGoid();
        if (selectedFolderAndGoid.left != null) {
            return "Install Simple Policy Bundle in " + selectedFolderAndGoid.left + SimplePolicyBundleInstallerDialog.BASE_FOLDER_NAME;
        } else {
            return "Install Simple Policy Bundle";
        }
    }


    @Override
    protected void performAction() {
        SimplePolicyBundleInstallerDialog dlg = new SimplePolicyBundleInstallerDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/external/assertions/simplepolicybundleinstaller/console/dashed_square_down_arrow.png";
    }
}