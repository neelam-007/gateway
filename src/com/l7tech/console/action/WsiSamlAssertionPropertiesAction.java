/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WsiSamlPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.WsiSamlAssertionPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.WsiSamlAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edit properties action for WSI-SAML Token Profile assertion.
 */
public class WsiSamlAssertionPropertiesAction extends SecureAction {

    //- PUBLIC

    public WsiSamlAssertionPropertiesAction(WsiSamlAssertionPolicyNode node) {
        super(null, WsiSamlAssertion.class);
        this.node = node;
    }

    public String getName() {
        return "WSI-SAML Properties";
    }

    public String getDescription() {
        return "View/Edit properties of the WS-I SAML assertion.";
    }

    //- PROTECTED

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final WsiSamlPropertiesDialog dlg = new WsiSamlPropertiesDialog(node.asAssertion(), f, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if(dlg.isAssertionChanged()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged(node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

    //- PRIVATE

    private final Logger log = Logger.getLogger(getClass().getName());

    private WsiSamlAssertionPolicyNode node;
}
