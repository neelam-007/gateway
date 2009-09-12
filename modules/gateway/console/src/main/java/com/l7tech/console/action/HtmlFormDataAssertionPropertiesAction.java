/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.HtmlFormDataAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action used in context menu to bring up the properties dialog.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataAssertionPropertiesAction extends NodeActionWithMetaSupport {
    private static final Logger _logger = Logger.getLogger(HtmlFormDataAssertionPropertiesAction.class.getName());

    public HtmlFormDataAssertionPropertiesAction(AssertionTreeNode node) {
        super(node, HtmlFormDataAssertion.class, node.asAssertion());
    }

    protected void performAction() {
        HtmlFormDataAssertion assertion = (HtmlFormDataAssertion) node.asAssertion();
        Frame mainWindow = TopComponents.getInstance().getTopParent();
        final HtmlFormDataAssertionDialog dialog = new HtmlFormDataAssertionDialog(mainWindow, assertion, !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(dialog);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            public void run() {
                if (dialog.isAssertionModified()) assertionChanged();
            }
        });
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel) tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode) node);
        } else {
            _logger.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
