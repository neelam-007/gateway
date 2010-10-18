package com.l7tech.console.action;

import com.l7tech.console.panels.MigrateNamespacesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
* Action that opens the Migrate Namespaces dialog.
*/
public class MigrateNamespacesAction extends AbstractAction {
    public MigrateNamespacesAction() {
        super("Migrate Namespaces");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JTree policyTree = TopComponents.getInstance().getPolicyTree();
        if (policyTree == null)
            return;

        TreePath[] selectedPaths = policyTree.getSelectionPaths();
        if (selectedPaths == null || selectedPaths.length < 1) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "No policy assertions are selected.", "Nothing to Migrate", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        List<AssertionTreeNode> selectedAssertionNodes = new ArrayList<AssertionTreeNode>();
        for (TreePath path : selectedPaths) {
            Object obj = path.getLastPathComponent();
            if (obj instanceof AssertionTreeNode) {
                AssertionTreeNode node = (AssertionTreeNode) obj;
                selectedAssertionNodes.add(node);
            }
        }

        final MigrateNamespacesDialog dlg = new MigrateNamespacesDialog(TopComponents.getInstance().getTopParent(), selectedAssertionNodes);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, null);
    }
}
