package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.SqlAttackDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SqlAttackAssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.SqlAttackAssertion;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 4:06:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackDialogAction extends NodeAction {
    private SqlAttackAssertionTreeNode treeNode;
    public SqlAttackDialogAction(SqlAttackAssertionTreeNode node) {
        super(node, SqlAttackAssertion.class);
        treeNode = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "SQL Attack Protection Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and edit SQL attack protection properties";
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
                SqlAttackDialog d = null;
                d = new SqlAttackDialog(f, (SqlAttackAssertion)node.asAssertion(), true);
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
