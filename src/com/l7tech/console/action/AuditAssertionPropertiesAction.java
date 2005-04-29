/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.console.panels.AuditAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AuditAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.logging.Level;

/**
 * Display properties dialog for AuditAssertion.
 */
public class AuditAssertionPropertiesAction extends SecureAction {
    private AuditAssertionTreeNode subject;

    public AuditAssertionPropertiesAction(AuditAssertionTreeNode subject) {
        this.subject = subject;
    }
    public String getName() {
        return "Audit Assertion Properties";
    }

    public String getDescription() {
        return "Change the properties of the audit assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Level thresold = null;
        try {
            thresold = Registry.getDefault().getAuditAdmin().serverMessageAuditThreshold();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Cannot call AuditAdmin.serverMessageAuditThreshold", e);
            thresold = Level.INFO;
        }

        AuditAssertionDialog aad = new AuditAssertionDialog(TopComponents.getInstance().getMainWindow(), subject.getAssertion(), thresold.getName());
        aad.pack();
        Utilities.centerOnScreen(aad);
        Actions.setEscKeyStrokeDisposes(aad);
        aad.setVisible(true);
        if (aad.isModified()) {
            subject.setUserObject(aad.getAssertion());
            assertionChanged();
        }
    }

    public void assertionChanged() {
        JTree tree = (JTree)TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)subject);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

}
