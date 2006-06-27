package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.HardcodedResponseTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.HardcodedResponseDialog;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Action that brings up HardcodedResponsePropertyDialog.
 */
public class HardcodedResponseDialogAction extends NodeAction{
    private HardcodedResponseTreeNode treeNode;
    public HardcodedResponseDialogAction(HardcodedResponseTreeNode node) {
        super(node);
        treeNode = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Template Response Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and edit template response properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = TopComponents.getInstance().getMainWindow();
                HardcodedResponseDialog d = null;
                d = new HardcodedResponseDialog(f, (HardcodedResponseAssertion)node.asAssertion(), true);
                d.pack();
                Utilities.centerOnScreen(d);
                //d.addPolicyListener(listener);
                d.setVisible(true);
                if (d.isModified()) {
                    treeNode.setUserObject(d.getAssertion());
                    fireAssertionChanged();
                }
            }
        });
    }

    private void fireAssertionChanged() {
        JTree tree = (JTree)TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)treeNode);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
