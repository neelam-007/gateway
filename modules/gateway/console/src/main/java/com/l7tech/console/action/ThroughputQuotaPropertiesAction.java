/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.ThroughputQuotaForm;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.sla.ThroughputQuota;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action to edit the properties of an ThroughputQuota assertion
 */
public class ThroughputQuotaPropertiesAction extends NodeActionWithMetaSupport {

    public ThroughputQuotaPropertiesAction(AssertionTreeNode<ThroughputQuota> subject) {
        super(null, ThroughputQuota.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        final ThroughputQuotaForm dlg = new ThroughputQuotaForm(TopComponents.getInstance().getTopParent(),
                                                          subject.asAssertion(), null, !subject.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged(subject);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private AssertionTreeNode<ThroughputQuota> subject;
}
