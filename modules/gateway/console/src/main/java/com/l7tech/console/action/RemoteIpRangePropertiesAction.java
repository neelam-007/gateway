/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.RemoteIpRangePropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.RemoteIpRange;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a RemoteIpRange assertion.
 */
public class RemoteIpRangePropertiesAction extends NodeActionWithMetaSupport {

    public RemoteIpRangePropertiesAction(AssertionTreeNode<RemoteIpRange> subject) {
        super(null, RemoteIpRange.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final RemoteIpRangePropertiesDialog dlg = new RemoteIpRangePropertiesDialog(f, false, !subject.canEdit(), subject.asAssertion());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOked()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged(subject);
                        log.finest("model invalidated");
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private AssertionTreeNode<RemoteIpRange> subject;
}
