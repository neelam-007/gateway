package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.AssertionIdentityTagSelector;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.IdentityTagable;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for selecting the tag for IdentityTagable assertions.
 *
 */
public class SelectIdentityTagAction extends NodeAction {
    private final IdentityTagable assertion;
    public SelectIdentityTagAction(AssertionTreeNode node) {
        super(node);
        assertion = (IdentityTagable) node.asAssertion();
    }

    @Override
    public String getName() {
        return "Identity Tag";
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
        final AssertionIdentityTagSelector dlg = new AssertionIdentityTagSelector(mw, assertion, !node.canEdit());
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
