package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

/**
 * Paste the list of entities stored in RootNode.getEntitiesToAlias() as new aliases.
 *
 * @author darmstrong
 */
public class PasteAsAliasAction extends SecureAction {
    private final FolderNode parentNode;

    public PasteAsAliasAction(FolderNode parentNode) {
        super(new AttemptedCreate(EntityType.FOLDER), UI_PUBLISH_SERVICE_WIZARD);
        this.parentNode = parentNode;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Paste as Alias";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Paste as Alias";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     */
    protected void performAction() {
        List<AbstractTreeNode> abstractTreeNodes = RootNode.getEntitiesToAlias();
        RootNode.clearEntitiesToAlias();
        JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        //Make sure the folder in which they are being created is not the same the folder they original entities are in
        //this constraint is also enforced in MarkEntityToAliasAction and also in db
        if(abstractTreeNodes.size() > 0){
            AbstractTreeNode parentNode = (AbstractTreeNode) abstractTreeNodes.get(0).getParent();
            FolderHeader fH = (FolderHeader) parentNode.getUserObject();
            FolderHeader parentHeader = (FolderHeader) this.parentNode.getUserObject();
            if(fH.getOid() == parentHeader.getOid()){
                DialogDisplayer.showMessageDialog(tree, "Cannot create alias in the same folder as original", "Create Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }
        }

        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        RootNode rootNode = (RootNode) model.getRoot();
        FolderHeader parentFolderHeader = (FolderHeader) this.parentNode.getUserObject();
        long parentFolderOid = parentFolderHeader.getOid();
        Folder parentFolder = parentNode.getFolder();
        //Create an alias of each node selected
        for(AbstractTreeNode atn: abstractTreeNodes){
            if(!(atn instanceof EntityWithPolicyNode)) return;
            EntityWithPolicyNode ewpn = (EntityWithPolicyNode) atn;
            Entity e;
            try {
                e = ewpn.getEntity();
            } catch (FindException e1) {
                throw new RuntimeException("Unable to load entity", e1);
            }

            final OrganizationHeader header;
            if (e instanceof PublishedService) {
                PublishedService ps = (PublishedService) e;
                //check if an alias already exists here
                PublishedServiceAlias checkAlias;
                try {
                    checkAlias = Registry.getDefault().getServiceManager().findAliasByEntityAndFolder(ps.getOid(), parentFolderOid);
                    if(checkAlias != null){
                        DialogDisplayer.showMessageDialog(tree,"Alias of service " + ps.displayName() + " already exists in folder " + parentFolderHeader.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }
                } catch (FindException e1) {
                    throw new RuntimeException("Unable to check for existing alias", e1);
                }

                try {
                    header = new ServiceHeader(ps);
                    PublishedServiceAlias psa = new PublishedServiceAlias(ps, parentFolder);
                    Registry.getDefault().getServiceManager().saveAlias(psa);
                } catch (Exception e1) {
                    throw new RuntimeException("Unable to save alias", e1);
                } 
            } else if (e instanceof Policy) {
                Policy policy = (Policy) e;
                //check if an alias already exists here
                PolicyAlias checkAlias;
                try {
                    checkAlias = Registry.getDefault().getPolicyAdmin().findAliasByEntityAndFolder(policy.getOid(), parentFolderOid);
                    if(checkAlias != null){
                        DialogDisplayer.showMessageDialog(tree,"Alias of policy " + policy.getName() + " already exists in folder " + parentFolderHeader.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }
                } catch (FindException e1) {
                    throw new RuntimeException("Unable to check for existing alias", e1);
                }

                try {
                    header = new PolicyHeader(policy);
                    PolicyAlias pa = new PolicyAlias(policy, parentFolder);
                    Registry.getDefault().getPolicyAdmin().saveAlias(pa);
                } catch (SaveException e1) {
                    throw new RuntimeException("Unable to save alias", e1);
                }
            } else {
                throw new IllegalStateException("Referent was neither a Policy nor a Service");
            }

            header.setAlias(true);
            header.setFolderOid(parentFolderOid);
            EntityWithPolicyNode childNode = (EntityWithPolicyNode) TreeNodeFactory.asTreeNode(header, RootNode.getComparator());

            int insertPosition = parentNode.getInsertPosition(childNode, RootNode.getComparator());
            parentNode.insert(childNode, insertPosition);
            model.nodesWereInserted(parentNode, new int[]{insertPosition});
            rootNode.addAlias(header.getOid(), childNode);
        }
    }
}

