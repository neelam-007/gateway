/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.util.Functions;
import com.l7tech.objectmodel.OrganizationHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.Set;

/**
 * Abstract class which implements the logic of deleting an entity whether it's a service, policy or an alias of
 * either.
 */
public abstract class DeleteEntityNodeAction <HT extends EntityWithPolicyNode> extends EntityWithPolicyNodeAction<HT>  {

    public DeleteEntityNodeAction(HT node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.DELETE;
    }

    /**
     * Removing super impl by making abstract, forcing subclass to implement it
     * @return String name of the entity being deleted 
     */
    public abstract String getName();

    /**
     * Removing super impl by making abstract, forcing subclass to implement it
     * @return String description of the entity being deleted
     */
    public abstract String getDescription();

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     * @return String the message to show to the user when the user is asked to confirm this action
     */
    public abstract String getUserConfirmationMessage();

    /**
     * @return String the title to show with the message from getUserConfirmationMessage() 
     */
    public abstract String getUserConfirmationTitle();

    /**
     * Delete the actual entity, using the correct manager
     */
    public abstract boolean deleteEntity();

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        final String message = getUserConfirmationMessage();
        final String title = getUserConfirmationTitle();
        
        Actions.getUserConfirmationAndCallBack(message, title, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean confirmed) {
                if (!confirmed) return;

                Registry.getDefault().getSecurityProvider().refreshPermissionCache();

                Runnable runnable = new Runnable() {
                    public void run() {
                        // Delete the entity
                        if(!deleteEntity()) return;

                        //as entity successfully removed, update the tree
                        final TopComponents creg = TopComponents.getInstance();
                        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.removeNodeFromParent(node);

                        //Remove any aliases
                        OrganizationHeader oH = (OrganizationHeader) node.getUserObject();
                        long oldServiceOid = oH.getOid();
                        Object root = model.getRoot();
                        RootNode rootNode = (RootNode) root;

                        if(!oH.isAlias()){
                            //fyi if an original is deleted, aliases are deleted on cascade in the model
                            Set<AbstractTreeNode> foundNodes = rootNode.getAliasesForEntity(oldServiceOid);
                            if(!foundNodes.isEmpty()){
                                for(AbstractTreeNode atn: foundNodes){
                                    model.removeNodeFromParent(atn);
                                }
                                rootNode.removeEntity(oldServiceOid);
                            }
                        }else{
                            rootNode.removeAlias(oldServiceOid, node);
                        }

                        //Update the workspace if this service was being displayed
                        try {
                            final WorkSpacePanel cws = creg.getCurrentWorkspace();
                            JComponent jc = cws.getComponent();
                            if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                                return;
                            }
                            PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                            EntityWithPolicyNode pn = pe.getPolicyNode();
                            // if currently edited entity was deleted
                            if (Long.valueOf(entityNode.getEntity().getId()).equals(Long.valueOf(pn.getEntity().getId()))) {
                                cws.setComponent(new HomePagePanel());
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                SwingUtilities.invokeLater(runnable);
            }
        });

    }
    
}
