/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.RequestSizeLimitDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.RequestSizeLimitTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

public class RequestSizeLimitDialogAction extends NodeAction{
    private RequestSizeLimitTreeNode treeNode;
    public RequestSizeLimitDialogAction(RequestSizeLimitTreeNode node) {
        super(node, RequestSizeLimit.class);
        treeNode = node;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Request Size Limit Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View and edit request size limit properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                final RequestSizeLimitDialog d;
                d = new RequestSizeLimitDialog(f, (RequestSizeLimit)node.asAssertion(), true, !node.canEdit());
                d.pack();
                Utilities.centerOnScreen(d);
                //d.addPolicyListener(listener);
                DialogDisplayer.display(d, new Runnable() {
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
