package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EmailAlertPropertiesDialog;
import com.l7tech.console.tree.policy.EmailAlertAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for viewing or editing the properties of a SnmpTrapAssertion.
 */
public class EmailAlertAssertionPropertiesAction extends SecureAction {
    public EmailAlertAssertionPropertiesAction(EmailAlertAssertionTreeNode subject) {
        this.subject = subject;
    }

    public String getName() {
        return "Email Alert Properties";
    }

    public String getDescription() {
        return "Change the properties of the email alert assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();
        EmailAlertPropertiesDialog dlg = new EmailAlertPropertiesDialog(f, subject.getSnmpTrapAssertion());
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
    
    private EmailAlertAssertionTreeNode subject;
}
