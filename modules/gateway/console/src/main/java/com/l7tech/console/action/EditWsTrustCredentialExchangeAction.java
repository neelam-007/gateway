/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.WsTrustCredentialExchangePropertiesDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class EditWsTrustCredentialExchangeAction extends NodeActionWithMetaSupport {
    private final WsTrustCredentialExchange wsTrustAssertion;

    /**
     * constructor accepting the node that this action will
     * act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public EditWsTrustCredentialExchangeAction(AbstractTreeNode node) {
        super(node, WsTrustCredentialExchange.class, node.asAssertion());
        if (!(node.asAssertion() instanceof WsTrustCredentialExchange)) {
            throw new IllegalArgumentException();
        }
        wsTrustAssertion = (WsTrustCredentialExchange)node.asAssertion();
    }

    @Override
    protected void performAction() {
        Frame parent = TopComponents.getInstance().getTopParent();
        final WsTrustCredentialExchangePropertiesDialog dlg = new WsTrustCredentialExchangePropertiesDialog(wsTrustAssertion, parent, true, !node.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isAssertionChanged()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }
}
