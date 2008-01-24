/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.SqlAttackDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SqlAttackAssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.SqlAttackAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

public class SqlAttackDialogAction extends NodeAction {
    private SqlAttackAssertionTreeNode treeNode;
    public SqlAttackDialogAction(SqlAttackAssertionTreeNode node) {
        super(node, SqlAttackAssertion.class);
        treeNode = node;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "SQL Attack Protection Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View and edit SQL attack protection properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                final SqlAttackDialog d = new SqlAttackDialog(f, (SqlAttackAssertion)node.asAssertion(), true, !node.canEdit());
                d.pack();
                Utilities.centerOnScreen(d);
                //d.addPolicyListener(listener);
                DialogDisplayer.display(d, new Runnable() {
                    public void run() {
                        if (d.isModified()) {
                            treeNode.setUserObject(d.getAssertion());
                            fireAssertionChanged();
                        }
                    }
                });
            }
        });
    }

    private void fireAssertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(treeNode);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
