/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Functions;
import com.l7tech.console.panels.PolicyPropertiesPanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.PolicyAssertionException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Enumeration;

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

        final Functions.UnaryVoid<Boolean> callback = new Functions.UnaryVoid<Boolean>() {
            @Override
            public void call(Boolean changed) {
                if (changed != null && changed) {
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
                            tree.updateAllAliases(pH.getGoid());

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    refreshTree(policyNode, tree);
                                }
                            });
                        }
                    }

                    // update the tab title of any specific policy editor, which could be currently opened or in idle.
                    try {
                        policyNode.firePropertyChange(this, PolicyEditorPanel.TAB_TITLE_CHANGE_PROPERTY, null, policyNode.getPolicy().getName());
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "problem modifying policy editor title");
                    }
                } else {
                    //change failed, reload policy
                    try {
                        policyNode.updateUserObject();
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "cannot update policy following failed update.");
                    }
                }
            }
        };
        editPolicyProperties(policy, callback, canUpdate);
    }

    private void editPolicyProperties(final Policy policy, final Functions.UnaryVoid<Boolean> resultCallback, final boolean canUpdate) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final OkCancelDialog<Policy> dlg = PolicyPropertiesPanel.makeDialog(mw, policy, canUpdate);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    Boolean success = null;
                    try {
                        Registry.getDefault().getPolicyAdmin().savePolicy(policy);
                        success = true;
                    } catch ( DuplicateObjectException doe) {
                        String message = "Unable to save the policy '" + policy.getName() + "'.\n";
                        if ( policy.getType() == PolicyType.GLOBAL_FRAGMENT ) {
                            message += "The policy name is already in use or there is an existing\n" +
                                       "Global Policy Fragment with the '"+policy.getInternalTag()+"' tag.";
                        } else if (policy.getType() == PolicyType.INTERNAL && PolicyType.getAuditMessageFilterTags().contains(policy.getInternalTag())){
                            message += "The policy name is already in use or there is an existing\n" +
                                   "Internal Policy with the '"+policy.getInternalTag()+"' tag.";
                        }
                        else {
                            message += "The policy name is already used, please choose a different\n name and try again.";
                        }

                        DialogDisplayer.showMessageDialog(mw, "Duplicate policy", message, null, new Runnable() {
                            @Override
                            public void run() {
                                // callback when dialog dismissed
                                editPolicyProperties( policy, resultCallback, canUpdate );
                            }
                        });
                    } catch (SaveException e) {
                        String msg = "Error updating policy:" + e.getMessage();
                        logger.log(Level.INFO, msg, e);
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                        success = false;
                    } catch (PolicyAssertionException e) {
                        String msg = "Error while changing policy properties since there is a problem with the policy.";
                        logger.log(Level.INFO, msg, e);
                        DialogDisplayer.showMessageDialog(mw, null, msg, null);
                        success = false;
                    } catch (final PermissionDeniedException e) {
                        success = false;
                        PermissionDeniedErrorHandler.showMessageDialog(e, logger);
                    }

                    resultCallback.call(success);
                } else {
                    resultCallback.call(false);
                }
            }
        });
    }

    private void sortChildren(AbstractTreeNode node){
        if(!(node instanceof FolderNode)){
            return;
        }

        java.util.List<AbstractTreeNode> childNodes = new ArrayList<AbstractTreeNode>();
        for(int i = 0; i < node.getChildCount(); i++){
            AbstractTreeNode childNode = (AbstractTreeNode)node.getChildAt(i);
            childNodes.add(childNode);
            if(childNode instanceof FolderNode){
                sortChildren(childNode);
            }
        }

        //Detach all children
        node.removeAllChildren();
        for(AbstractTreeNode atn: childNodes){
            node.insert(atn, node.getInsertPosition(atn, RootNode.getComparator()));
        }
    }

    private void refreshTree(EntityWithPolicyNode policyNode, final JTree tree) {
        try {
            policyNode.updateUserObject();

            TreePath rootPath = tree.getPathForRow(0);
            final TreePath selectedNodeTreePath = tree.getSelectionPath();
            final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            RootNode rootNode = (RootNode) model.getRoot();
            sortChildren(rootNode);
            model.nodeStructureChanged(rootNode);

            while (pathEnum.hasMoreElements()) {
                Object pathObj = pathEnum.nextElement();
                TreePath tp = (TreePath) pathObj;
                tree.expandPath(tp);
            }
            tree.setSelectionPath(selectedNodeTreePath);

        } catch (FindException fe) {
            logger.info("Cannot the new policy to update policy node.");
        }
    }
}
