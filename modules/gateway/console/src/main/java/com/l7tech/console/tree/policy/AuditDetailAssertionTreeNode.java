/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.AuditDetailAssertionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an AuditDetailAssertion in the policy tree.
 */
public class AuditDetailAssertionTreeNode extends LeafAssertionTreeNode<AuditDetailAssertion> {
    protected static final Logger logger = Logger.getLogger(AuditDetailAssertionTreeNode.class.getName());

    public AuditDetailAssertionTreeNode(AuditDetailAssertion assertion) {
        super(assertion);
    }

    @Override
    public String getName() {
        return "Audit detail: \"" + assertion.getDetail() + "\"";
    }

    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    @Override
    public Action getPreferredAction() {
        return new SecureAction(null, AuditAssertion.class) {
            @Override
            public String getName() {
                return "Audit Detail Properties";
            }

            @Override
            public String getDescription() {
                return "Change the properties of the audit detail assertion.";
            }

            @Override
            protected String iconResource() {
                return "com/l7tech/console/resources/Properties16.gif";
            }

            @Override
            protected void performAction() {
                AuditDetailAssertionPropertiesDialog aad =
                        new AuditDetailAssertionPropertiesDialog(TopComponents.getInstance().getTopParent(), assertion, !canEdit());
                aad.pack();
                Utilities.centerOnScreen(aad);
                Utilities.setEscKeyStrokeDisposes(aad);
                aad.setVisible(true);
                if (aad.isModified()) {
                    assertionChanged();
                }
            }

            public void assertionChanged() {
                JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(AuditDetailAssertionTreeNode.this);
                } else {
                    AuditDetailAssertionTreeNode.logger.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        };
    }
}
