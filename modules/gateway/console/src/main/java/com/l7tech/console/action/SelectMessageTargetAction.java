package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.AssertionMessageTargetSelector;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.MessageTargetable;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for selecting the target for MessageTargetable assertions.
 *
 */
public class SelectMessageTargetAction extends NodeAction {
    private final MessageTargetable assertion;
    public SelectMessageTargetAction(AssertionTreeNode node) {
        super(node);
        assertion = (MessageTargetable)node.asAssertion();
    }

    @Override
    public String getName() {
        return "Select Target Message";
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final AssertionMessageTargetSelector dlg = new AssertionMessageTargetSelector(mw, assertion, !node.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.hasAssertionChanged()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }
}
