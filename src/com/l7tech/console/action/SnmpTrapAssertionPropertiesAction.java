package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.SnmpTrapPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SnmpTrapAssertionTreeNode;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for viewing or editing the properties of a SnmpTrapAssertion.
 */
public class SnmpTrapAssertionPropertiesAction extends SecureAction {
    public SnmpTrapAssertionPropertiesAction(SnmpTrapAssertionTreeNode subject) {
        this.subject = subject;
    }

    public String getName() {
        return "SNMP Trap Properties";
    }

    public String getDescription() {
        return "Change the properties of the SNMP trap assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();
        SnmpTrapPropertiesDialog dlg = new SnmpTrapPropertiesDialog(f, subject.getSnmpTrapAssertion());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();

        if (dlg.getResult() != null) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(subject);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }

    }
    
    private SnmpTrapAssertionTreeNode subject;
}
