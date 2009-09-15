/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.AuditAssertionDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.AuditAssertion;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Display properties dialog for AuditAssertion.
 */
public class AuditAssertionPropertiesAction extends NodeActionWithMetaSupport {
    private final AssertionTreeNode<AuditAssertion> subject;

    public AuditAssertionPropertiesAction(AssertionTreeNode<AuditAssertion> subject) {
        super(null, AuditAssertion.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        Level thresold;
        thresold = Registry.getDefault().getAuditAdmin().serverMessageAuditThreshold();

        final AuditAssertionDialog aad = new AuditAssertionDialog(TopComponents.getInstance().getTopParent(), subject.asAssertion(), thresold.getName(), !subject.canEdit());
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        DialogDisplayer.display(aad, TopComponents.getInstance().getRootSheetHolder(), new Runnable() {
            @Override
            public void run() {
                if (aad.isModified()) {
                    subject.setUserObject(aad.getAssertion());
                    assertionChanged();
                }
            }
        });
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
