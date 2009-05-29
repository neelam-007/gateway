package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.IdentityTargetSelector;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action for selecting the target for MessageTargetable assertions.
 */
public class SelectIdentityTargetAction extends NodeAction {

    //- PUBLIC

    public SelectIdentityTargetAction( final AssertionTreeNode<? extends IdentityTargetable> node ) {
        super(node);
        assertion = node.asAssertion();
    }

    @Override
    public String getName() {
        return "Select Target Identity";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final IdentityTargetSelector dlg = new IdentityTargetSelector(mw, !node.canEdit(), assertion );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
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

    //- PRIVATE

    private final Assertion assertion;

}