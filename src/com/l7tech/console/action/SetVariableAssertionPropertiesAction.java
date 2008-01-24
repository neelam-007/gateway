/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.SetVariableAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SetVariableAssertionPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.SetVariableAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>SetVariableAssertionPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.SetVariableAssertion} properties.
 */
public class SetVariableAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(SetVariableAssertionPropertiesAction.class.getName());

    public SetVariableAssertionPropertiesAction(SetVariableAssertionPolicyNode node) {
        super(node, SetVariableAssertion.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Set Variable Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit Set Variable Assertion Properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/About16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        SetVariableAssertion sva = (SetVariableAssertion) node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final SetVariableAssertionDialog eqd = new SetVariableAssertionDialog(f, !node.canEdit(), sva);
        Utilities.setEscKeyStrokeDisposes(eqd);
        eqd.pack();
        Utilities.centerOnScreen(eqd);
        DialogDisplayer.display(eqd, new Runnable() {
            public void run() {
                if (eqd.isAssertionModified()) assertionChanged();
            }
        });
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            SetVariableAssertionPropertiesAction.log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
