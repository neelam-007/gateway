/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.util.Functions;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
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
public class EditPolicyProperties extends EntityWithPolicyNodeAction<PolicyEntityNode> {
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
                    final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        //model.nodeChanged(node);
                        model.reload(node);
                    }

                    //if this is an original entity, update any aliases it may have, in case it's name or 
                    //something else show to the user in the tree changes
                    if(!(policyNode instanceof PolicyEntityNodeAlias)){
                        if (tree !=null) {
                            PolicyHeader pH = (PolicyHeader) policyNode.getUserObject();
                            tree.updateAllAliases(pH.getOid());

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    RefreshTreeNodeAction refresh = new RefreshTreeNodeAction((RootNode)tree.getModel().getRoot());
                                    refresh.setTree(tree);
                                    refresh.invoke();
                                }
                            });
                        }
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
                    try {
                        Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                    } catch ( DuplicateObjectException doe) {
                        String msg =
                              "Unable to save the policy '" + policy.getName() + "'.\n" +
                              "The policy name is already used, please choose a different\n name and try again.";
                        DialogDisplayer.showMessageDialog(mw, "Duplicate policy name", msg, null);
                    } catch (SaveException e) {
                        String msg = "Error updating policy:" + e.getMessage();
                        logger.log(Level.INFO, msg, e);
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                    } catch (PolicyAssertionException e) {
                        String msg = "Error while changing policy properties since there is a problem with the policy.";
                        logger.log(Level.INFO, msg, e);
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                    }
                    resultCallback.call(true);
                } else {
                    resultCallback.call(false);
                }
            }
        });
    }
}
