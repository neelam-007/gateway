/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.proxy.gui.dialogs.WsTrustCredentialExchangePropertiesDialog;

import javax.swing.JTree;
import java.awt.Frame;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class EditWsTrustCredentialExchangeAction extends NodeAction {
    private final WsTrustCredentialExchange wsTrustAssertion;

    /**
     * constructor accepting the node that this action will
     * act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public EditWsTrustCredentialExchangeAction(AbstractTreeNode node) {
        super(node);
        if (!(node.asAssertion() instanceof WsTrustCredentialExchange)) {
            throw new IllegalArgumentException();
        }
        wsTrustAssertion = (WsTrustCredentialExchange)node.asAssertion();
    }

    public String getName() {
        return "View/Edit WS-Trust Credential Exchange Properties";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        Frame parent = TopComponents.getInstance().getMainWindow();
        WsTrustCredentialExchangePropertiesDialog dlg = new WsTrustCredentialExchangePropertiesDialog(wsTrustAssertion, parent, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
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
}
