/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.CodeInjectionProtectionAssertionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.CodeInjectionProtectionAssertionDialog;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;

/**
 * Action used in context menu to bring up the properties dialog.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertionPropertiesAction extends NodeAction {
    private static final Logger _logger = Logger.getLogger(CodeInjectionProtectionAssertionPropertiesAction.class.getName());

    public CodeInjectionProtectionAssertionPropertiesAction(CodeInjectionProtectionAssertionPolicyNode node) {
        super(node, CodeInjectionProtectionAssertion.class);
    }

    public String getName() {
        return "Code Injection Protection Properties";
    }

    public String getDescription() {
        return "View/Edit Code Injection Protection Properties";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        CodeInjectionProtectionAssertion assertion = (CodeInjectionProtectionAssertion) node.asAssertion();
        Frame mainWindow = TopComponents.getInstance().getTopParent();
        final CodeInjectionProtectionAssertionDialog dialog = new CodeInjectionProtectionAssertionDialog(mainWindow, assertion, !node.canEdit());
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
