package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.EmailAlertPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for viewing or editing the properties of a EmailAlertAssertion.
 */
public class EmailAlertAssertionPropertiesAction extends NodeActionWithMetaSupport {
    public EmailAlertAssertionPropertiesAction(AssertionTreeNode<EmailAlertAssertion> subject) {
        super(null, EmailAlertAssertion.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final EmailAlertPropertiesDialog dlg = new EmailAlertPropertiesDialog(f, subject.asAssertion(), !subject.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
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
    
    private AssertionTreeNode<EmailAlertAssertion> subject;
}
