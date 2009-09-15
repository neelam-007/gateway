/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.action;

import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.console.panels.AuditDetailAssertionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditDetailsPropertiesAction extends NodeActionWithMetaSupport{
    static final Logger log = Logger.getLogger(AuditDetailsPropertiesAction.class.getName());
    private final AssertionTreeNode<AuditDetailAssertion> subject;

    public AuditDetailsPropertiesAction(AssertionTreeNode<AuditDetailAssertion> subject) {
        super(null, AuditDetailAssertion.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        AuditDetailAssertionPropertiesDialog aad =
                new AuditDetailAssertionPropertiesDialog(TopComponents.getInstance().getTopParent(), subject.asAssertion(), !subject.canEdit());
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        aad.setVisible(true);
        if (aad.isModified()) {
            assertionChanged();
        }
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(subject);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
