/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.*;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.util.Functions;
import org.apache.commons.lang.WordUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Abstract class which implements the logic of deleting an entity whether it's a service, policy or an alias of
 * either.
 */
public abstract class DeleteEntityNodeAction <HT extends EntityWithPolicyNode> extends EntityWithPolicyNodeAction<HT>  {
    public static final int LINE_CHAR_LIMIT = 80;
    private boolean confirmationEnabled;  // Check if a deletion confirmation is needed or not.

    public DeleteEntityNodeAction(HT node) {
        this(node, true);
    }

    public DeleteEntityNodeAction(HT node, boolean confirmationEnabled) {
        super(node);
        this.confirmationEnabled = confirmationEnabled;
    }

    @Override
    protected OperationType getOperation() {
        return OperationType.DELETE;
    }

    /**
     * Removing super impl by making abstract, forcing subclass to implement it
     * @return String name of the entity being deleted 
     */
    @Override
    public abstract String getName();

    /**
     * Removing super impl by making abstract, forcing subclass to implement it
     * @return String description of the entity being deleted
     */
    @Override
    public abstract String getDescription();

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     * Generate a message to show to the user when the user is asked to confirm this action.
     * @return a string probably containing multiple lines.  Each line has a max length, 80.
     */
    public String getUserConfirmationMessage() {
        String nodeEntityType;
        if (node instanceof ServiceNodeAlias) nodeEntityType = "service alias";
        else if (node instanceof ServiceNode) nodeEntityType = "service";
        else if (node instanceof PolicyEntityNodeAlias) nodeEntityType = "policy alias";
        else if (node instanceof PolicyEntityNode) nodeEntityType = "policy";
        else nodeEntityType = node.getClass().getSimpleName();

        StringBuilder sb = new StringBuilder("Are you sure you want to delete the '").append(node.getName()).append("' ").append(nodeEntityType).append("?");
        if (sb.length() <= LINE_CHAR_LIMIT) return sb.toString();

        sb = new StringBuilder("Are you sure you want to delete the " + nodeEntityType + ", ").append("'").append(node.getName()).append("'").append("?");
        return WordUtils.wrap(sb.toString(), LINE_CHAR_LIMIT, null, true);
    }

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
    @Override
    protected void performAction() {
        if (! confirmationEnabled) {
            deleteEntityNode();
            return;
        }

        boolean isPublishedToUddi = false;
        if(node instanceof ServiceNode && !(node instanceof ServiceNodeAlias)){
            final ServiceNode serviceNode = (ServiceNode) node;            
            //if confirmation is enabled, it means that it's a single delete, so we should check if UDDI has data
            final Set<Goid> serviceGoidSet = new HashSet<>();
            try {
                serviceGoidSet.add(serviceNode.getEntity().getGoid());
                final Collection<ServiceHeader> headers = Registry.getDefault().getUDDIRegistryAdmin().getServicesPublishedToUDDI(serviceGoidSet);
                isPublishedToUddi = !headers.isEmpty();
            } catch (FindException e) {
                log.log(Level.WARNING, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        if(isPublishedToUddi){
            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                    "Service has published information to UDDI. If service is deleted, data will be left in UDDI. Continue?",
                    "Services have published data to UDDI",
                    JOptionPane.WARNING_MESSAGE, new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if(option != JOptionPane.YES_OPTION) return;
                            handleDeleteAction();
                        }
                    });
        }else{
            handleDeleteAction();
        }
    }

    private void handleDeleteAction(){
        final String message = getUserConfirmationMessage();
        final String title = getUserConfirmationTitle();

        Actions.getUserConfirmationAndCallBack(message, title, new Functions.UnaryVoid<Boolean>() {
            @Override
            public void call(Boolean confirmed) {
                if (!confirmed) return;

                Registry.getDefault().getSecurityProvider().refreshPermissionCache();

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        deleteEntityNode();
                    }
                };
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

    private void deleteEntityNode() {
        // Delete the entity
        if(!deleteEntity()) return;

        //as entity successfully removed, update the tree
        final TopComponents creg = TopComponents.getInstance();
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)creg.getComponent(ServicesAndPoliciesTree.NAME);
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        model.removeNodeFromParent(node);

        //Remove any aliases
        OrganizationHeader oH = (OrganizationHeader) node.getUserObject();
        Goid oldServiceGoid = oH.getGoid();
        Object root = model.getRoot();
        RootNode rootNode = (RootNode) root;

        if(!oH.isAlias()){
            //fyi if an original is deleted, aliases are deleted on cascade in the model
            Set<AbstractTreeNode> foundNodes = rootNode.getAliasesForEntity(oldServiceGoid);
            if(!foundNodes.isEmpty()){
                for(AbstractTreeNode atn: foundNodes){
                    model.removeNodeFromParent(atn);
                }
                rootNode.removeEntity(oldServiceGoid);
            }
        }else{
            rootNode.removeAlias(oldServiceGoid, node);
        }

        final Goid policyGoid;
        try {
            policyGoid = entityNode.getPolicy().getGoid();
        } catch (FindException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                "Cannot find the policy, '" + entityNode.getName() + "'.",
                "Delete Policy Tab Settings Error", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // Delete policy tab settings from all policy tab properties, The entity node is used to retrieve the policy GOID.
        // So any policy tabs whose policy version has the policy GOID would be candidates.
        if (this instanceof DeletePolicyAction || this instanceof DeleteServiceAction) {
            creg.getCurrentWorkspace().deletePolicyTabSettingsByPolicyGoid(policyGoid);
        }

        // Close all tabs related to the deleted entity node.
        creg.getCurrentWorkspace().closeTabsRelatedToPolicyNode(policyGoid);

        // Update the list of the last opened policies
        creg.getCurrentWorkspace().saveLastOpenedPolicyTabs();
    }
}