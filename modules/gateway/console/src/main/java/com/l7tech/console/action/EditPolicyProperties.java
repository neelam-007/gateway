/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.policy.Policy;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.util.Functions;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.policy.assertion.PolicyAssertionException;

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

    @Override
    protected OperationType getOperation() {
        return OperationType.READ;
    }

    @Override
    public String getName() {
        return "Policy Properties";
    }

    @Override
    public String getDescription() {
        return "View/Edit the properties of the policy";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    @Override
    protected void performAction() {
        final PolicyEntityNode policyNode = (PolicyEntityNode) node;
        boolean canUpdate;
        final Policy policy;
        try {
            policy = new Policy(policyNode.getPolicy());
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
                        final EntityWithPolicyNode pn = pe.getPolicyNode();
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
                    boolean wasOk = false;
                    try {
                        Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                        wasOk = true;
                    } catch ( DuplicateObjectException doe) {
                        String msg =
                              "Unable to save the policy '" + policy.getName() + "'.\n" +
                              "The policy name is already used, please choose a different\n name and try again.";
                        DialogDisplayer.showMessageDialog(mw, "Duplicate policy name", msg, null);
                    } catch (SaveException e) {
                        String msg = "Error while updating policy due to improper policy properties.";
                        logger.log(Level.INFO, msg, e);
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                    } catch (PolicyAssertionException e) {
                        String msg = "Error while changing policy properties since there is a problem with the policy.";
                        logger.log(Level.INFO, msg, e);
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                    }
                    resultCallback.call(wasOk);
                } else {
                    resultCallback.call(false);
                }
            }
        });
    }
}
