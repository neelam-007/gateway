/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.throughputquota.console.action;

import com.l7tech.console.action.NodeActionWithMetaSupport;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.throughputquota.ThroughputQuotaAssertion;
import com.l7tech.external.assertions.throughputquota.console.ThroughputQuotaForm;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action to edit the properties of an ThroughputQuotaAssertion
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ThroughputQuotaPropertiesAction extends NodeActionWithMetaSupport {

    public ThroughputQuotaPropertiesAction(AssertionTreeNode<ThroughputQuotaAssertion> subject) {
        super(null, ThroughputQuotaAssertion.class, subject.asAssertion());
        this.subject = subject;
    }

    @Override
    protected void performAction() {
        final ThroughputQuotaAssertion ass = subject.asAssertion();
        final ThroughputQuotaForm dlg = new ThroughputQuotaForm(TopComponents.getInstance().getTopParent(),ass, !subject.canEdit());
        dlg.setData(ass);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    subject.setUserObject(dlg.getData(ass));
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
    private AssertionTreeNode<ThroughputQuotaAssertion> subject;
}
