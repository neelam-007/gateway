/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.RequestSizeLimitDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

public class RequestSizeLimitDialogAction extends NodeActionWithMetaSupport{
    private AssertionTreeNode<RequestSizeLimit> treeNode;
    public RequestSizeLimitDialogAction(AssertionTreeNode<RequestSizeLimit> node) {
        super(node, RequestSizeLimit.class, node.asAssertion());
        treeNode = node;
    }

    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                final RequestSizeLimitDialog d;
                d = new RequestSizeLimitDialog(f, (RequestSizeLimit)node.asAssertion(), true, !node.canEdit());
                d.pack();
                Utilities.centerOnScreen(d);
                //d.addPolicyListener(listener);
                DialogDisplayer.display(d, new Runnable() {
                    @Override
                    public void run() {
                        if (d.isModified()) {
                            treeNode.setUserObject(d.getAssertion());
                            fireAssertionChanged();
                        }
                    }
                });
            }
        });
    }

    private void fireAssertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(treeNode);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
