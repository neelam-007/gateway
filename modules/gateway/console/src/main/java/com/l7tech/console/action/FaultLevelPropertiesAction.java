package com.l7tech.console.action;

import com.l7tech.console.tree.policy.FaultLevelTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.FaultLevelPropertiesDialog;
import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action that triggers edit of FaultLevel properties.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 */
public class FaultLevelPropertiesAction extends NodeActionWithMetaSupport {
    private AssertionTreeNode subject;

    public FaultLevelPropertiesAction(AssertionTreeNode subject) {
        super(null, FaultLevel.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final FaultLevelPropertiesDialog dlg = new FaultLevelPropertiesDialog(f, (FaultLevel)subject.asAssertion(), !subject.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.oked) {
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
