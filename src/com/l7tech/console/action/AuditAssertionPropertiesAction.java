/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AuditAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
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
        Frame f = TopComponents.getInstance().getMainWindow();

        String[] options = new String[] {
            Level.ALL.getName(),
            Level.CONFIG.getName(),
            Level.FINEST.getName(),
            Level.FINER.getName(),
            Level.FINE.getName(),
            Level.INFO.getName(),
            Level.WARNING.getName(),
            Level.SEVERE.getName(),
            Level.OFF.getName(),
        };

        String s =
          (String)JOptionPane.showInputDialog(TopComponents.getInstance().getMainWindow(),
            "Audit level shall be no lower than:\n",
            "Audit Properties",
            JOptionPane.PLAIN_MESSAGE,
            new ImageIcon(subject.getIcon()),
            options,
            subject.getAssertion().getLevel());

        if (s != null) {
            subject.getAssertion().setLevel(s);
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
