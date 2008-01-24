package com.l7tech.console.action;

import com.l7tech.console.tree.policy.FaultLevelTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.FaultLevelPropertiesDialog;
import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action that triggers edit of FaultLevel properties.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 */
public class FaultLevelPropertiesAction extends SecureAction {
    private FaultLevelTreeNode subject;

    public FaultLevelPropertiesAction(FaultLevelTreeNode subject) {
        super(null, FaultLevel.class);
        this.subject = subject;
    }

    @Override
    public String getName() {
        return "Fault Level Properties";
    }

    @Override
    public String getDescription() {
        return "Change the properties of the Fault Level assertion.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final FaultLevelPropertiesDialog dlg = new FaultLevelPropertiesDialog(f, (FaultLevel)subject.asAssertion(), !subject.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
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
