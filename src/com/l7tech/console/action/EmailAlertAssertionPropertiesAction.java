package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.EmailAlertPropertiesDialog;
import com.l7tech.console.tree.policy.EmailAlertAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for viewing or editing the properties of a EmailAlertAssertion.
 */
public class EmailAlertAssertionPropertiesAction extends SecureAction {
    public EmailAlertAssertionPropertiesAction(EmailAlertAssertionTreeNode subject) {
        super(null, EmailAlertAssertion.class);
        this.subject = subject;
    }

    @Override
    public String getName() {
        return "Email Alert Properties";
    }

    @Override
    public String getDescription() {
        return "Change the properties of the email alert assertion.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final EmailAlertPropertiesDialog dlg = new EmailAlertPropertiesDialog(f, subject.asAssertion(), !subject.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
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
        });
    }
    
    private EmailAlertAssertionTreeNode subject;
}
