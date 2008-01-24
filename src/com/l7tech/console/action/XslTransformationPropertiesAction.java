/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.XslTransformationTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xml.XslTransformation;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a Xsl Transformation Assertion node.
 */
public class XslTransformationPropertiesAction extends SecureAction {

    public XslTransformationPropertiesAction(XslTransformationTreeNode node) {
        super(null, XslTransformation.class);
        this.node = node;
    }

    @Override
    public String getName() {
        return "XSL Transformation Properties";
    }

    @Override
    public String getDescription() {
        return "View/Edit properties of the xsl transformation assertion.";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(f, true, !node.canEdit(), node.asAssertion());
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (!dlg.wasOKed()) {
                    return;
                }
                JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(node);
                    log.finest("model invalidated");
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private XslTransformationTreeNode node;
}
