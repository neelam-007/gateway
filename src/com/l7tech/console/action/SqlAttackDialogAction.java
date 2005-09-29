package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.SqlAttackAssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.BridgeRoutingAssertionPropertiesDialog;
import com.l7tech.console.panels.SqlAttackDialog;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 4:06:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackDialogAction extends NodeAction {
    public SqlAttackDialogAction(SqlAttackAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Bridge Routing Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and edit bridge routing properties";
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
            }
        });
    }
}
