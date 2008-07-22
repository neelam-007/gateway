/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.ThroughputQuotaTreeNode;
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
public class ThroughputQuotaPropertiesAction extends SecureAction {

    public ThroughputQuotaPropertiesAction(ThroughputQuotaTreeNode subject) {
        super(null, ThroughputQuota.class);
        this.subject = subject;
    }

    @Override
    public String getName() {
        return "Throughput Quota Properties";
    }

    @Override
    public String getDescription() {
        return "View / Edit properties of a Throughput Quota Assertion";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        final ThroughputQuotaForm dlg = new ThroughputQuotaForm(TopComponents.getInstance().getTopParent(),
                                                          subject.asAssertion(), null, !subject.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
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
    private ThroughputQuotaTreeNode subject;
}
