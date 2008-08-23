package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 20, 2008
 * Time: 10:24:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class PasteAsAliasAction extends SecureAction {
    static Logger log = Logger.getLogger(PasteAsAliasAction.class.getName());

    private AbstractTreeNode parentNode;

    public PasteAsAliasAction(AbstractTreeNode parentNode)
    {
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
        //Make sure the folder is not the same
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
        //Create an alias of each node selected
        for(AbstractTreeNode atn: abstractTreeNodes){
            if(atn instanceof ServiceNode){
                ServiceNode sN = (ServiceNode) atn;
                try {
                    PublishedService ps = sN.getPublishedService();
                    //check if an alias already exists here
                    PublishedServiceAlias checkAlias = Registry.getDefault().getServiceManager().findAliasByServiceAndFolder(ps.getOid(), parentFolderOid);
                    if(checkAlias != null){
                        DialogDisplayer.showMessageDialog(tree,"Alias of service " + ps.displayName() + " already exists in folder " + parentFolderHeader.getName(), "Create Error", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }

                    ServiceHeader serviceHeader = new ServiceHeader(ps);
                    serviceHeader.setIsAlias(true);
                    ServiceNode child = (ServiceNode) TreeNodeFactory.asTreeNode(serviceHeader, RootNode.getComparator());

                    PublishedServiceAlias psa = new PublishedServiceAlias(ps, parentFolderOid);

                    Registry.getDefault().getServiceManager().savePublishedServiceAlias(psa);
                    int insertPosition = parentNode.getInsertPosition(child, RootNode.getComparator());
                    parentNode.insert(child, insertPosition);
                    model.nodesWereInserted(parentNode, new int[]{insertPosition});
                    rootNode.addAlias(serviceHeader.getOid(), child);
                } catch (Exception e) {
                    log.log(Level.INFO, "Could not create alias: " + e.getMessage());
                }

            }
        }
    }
}

