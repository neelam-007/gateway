package com.l7tech.console.action;

import com.l7tech.console.tree.policy.RateLimitAssertionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.RateLimitAssertionPropertiesDialog;
import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action that triggers edit of RateLimitAssertion properties.
 * @see com.l7tech.policy.assertion.RateLimitAssertion
 */
public class RateLimitAssertionPropertiesAction extends SecureAction {
    private RateLimitAssertionPolicyNode subject;

    public RateLimitAssertionPropertiesAction(RateLimitAssertionPolicyNode subject) {
        super(null, RateLimitAssertion.class);
        this.subject = subject;
    }

    public String getName() {
        return "Rate Limit Properties";
    }

    public String getDescription() {
        return "Change the properties of the Rate Limit assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final RateLimitAssertionPropertiesDialog dlg = new RateLimitAssertionPropertiesDialog(f, (RateLimitAssertion)subject.asAssertion());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    dlg.getData((RateLimitAssertion)subject.asAssertion());
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged(subject);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the policy tree.");
                    }
                }
            }
        });
    }
}
