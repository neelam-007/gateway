/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.SamlBrowserArtifactPropertiesDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class EditSamlBrowserArtifactAction extends NodeAction {
    private final SamlBrowserArtifact samlBrowserArtifactAssertion;

    /**
     * constructor accepting the node that this action will
     * act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public EditSamlBrowserArtifactAction(AbstractTreeNode node) {
        super(node);
        if (!(node.asAssertion() instanceof SamlBrowserArtifact)) {
            throw new IllegalArgumentException();
        }
        samlBrowserArtifactAssertion = (SamlBrowserArtifact)node.asAssertion();
    }

    public String getName() {
        return "View/Edit SAML Browser Artifact Properties";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        Frame parent = TopComponents.getInstance().getMainWindow();
        SamlBrowserArtifactPropertiesDialog dlg = new SamlBrowserArtifactPropertiesDialog(samlBrowserArtifactAssertion, parent, true);
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
