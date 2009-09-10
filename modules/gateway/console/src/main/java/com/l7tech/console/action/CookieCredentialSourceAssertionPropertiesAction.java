/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.CookieCredentialSourceAssertionPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a cookie credential source assertion.
 */
public class CookieCredentialSourceAssertionPropertiesAction extends NodeActionWithMetaSupport {
    private final Logger log = Logger.getLogger(getClass().getName());
    private final AssertionTreeNode subject;

    public CookieCredentialSourceAssertionPropertiesAction(AssertionTreeNode subject) {
        super(null, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final CookieCredentialSourceAssertion assertion = (CookieCredentialSourceAssertion)subject.asAssertion();
        final CookieCredentialSourceAssertionPropertiesDialog dlg =
                new CookieCredentialSourceAssertionPropertiesDialog(f, true, assertion, !subject.canEdit());

        dlg.pack();
        Utilities.centerOnScreen(dlg);

        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (!dlg.isConfirmed()) return;

                dlg.getData(assertion);
                JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(subject);
                    log.finest("model invalidated");
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }
}
