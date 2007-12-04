/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.util.Functions;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action to edit the policy properties
 */
public class EditPolicyProperties extends PolicyNodeAction {
    public EditPolicyProperties(PolicyEntityNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.READ;
    }

    public String getName() {
        return "Policy Properties";
    }

    public String getDescription() {
        return "View/Edit the properties of the policy";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final PolicyEntityNode policyNode = (PolicyEntityNode) node;
        boolean canUpdate;
        final Policy policy;
        try {
            policy = policyNode.getPolicy();
            canUpdate = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.POLICY, policy));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get policy", e);
            throw new RuntimeException(e);
        }

        Functions.UnaryVoid<Boolean> callback = new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean changed) {
                if (changed) {
                    policyNode.clearCachedEntities();
                    JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                    }

                    // update name on top of editor if that policy is being edited
                    final WorkSpacePanel cws = TopComponents.getInstance().getCurrentWorkspace();
                    JComponent jc = cws.getComponent();
                    if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                        return;
                    }
                    PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                    try {
                        final PolicyEntityNode pn = pe.getPolicyNode();
                        // if currently edited policy was deleted
                        if (policyNode.getPolicy().getOid() == pn.getPolicy().getOid()) {
                            // update name on top of editor
                            pe.changeSubjectName(policyNode.getName());
                            pe.updateHeadings();
                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "problem modifying policy editor title");
                    }
                }
            }
        };
        editPolicyProperties(policy, callback, canUpdate);
    }

    private void editPolicyProperties(final Policy policy, final Functions.UnaryVoid<Boolean> resultCallback, boolean canUpdate) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final OkCancelDialog<Policy> dlg = PolicyPropertiesPanel.makeDialog(mw, policy, canUpdate);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    try {
                        Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                    } catch (Exception e) {
                        String msg = "Error while changing policy properties";
                        logger.log(Level.INFO, msg, e);
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) msg += ":\n" + errorMessage;
                        JOptionPane.showMessageDialog(mw, msg);
                    }
                    resultCallback.call(true);
                    return;
                }
                resultCallback.call(false);
            }
        });
    }
}
