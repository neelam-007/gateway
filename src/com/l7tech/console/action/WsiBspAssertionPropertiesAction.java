/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.WsiBspPropertiesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.WsiBspAssertionPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.WsiBspAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edit properties action for WSI-BSP assertion.
 */
public class WsiBspAssertionPropertiesAction extends SecureAction {

    //- PUBLIC

    public WsiBspAssertionPropertiesAction(WsiBspAssertionPolicyNode node) {
        super(null, WsiBspAssertion.class);
        this.node = node;
    }

    public String getName() {
        return "WSI-BSP Properties";
    }

    public String getDescription() {
        return "View/Edit properties of the WS-I BSP assertion.";
    }

    //- PROTECTED

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final WsiBspPropertiesDialog dlg = new WsiBspPropertiesDialog(node.asAssertion(), f, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if(dlg.isAssertionChanged()) {
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

    //- PRIVATE

    private final Logger log = Logger.getLogger(getClass().getName());

    private WsiBspAssertionPolicyNode node;
}
