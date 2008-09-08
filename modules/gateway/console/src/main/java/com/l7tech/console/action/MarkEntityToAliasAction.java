package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.List;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 20, 2008
 * Time: 10:00:36 AM
 * Action to allow the user to select what entity they want to alias. Aliases marked are remembered until either
 * they are pasted as new alisases or another set of entities are marked. Marked means to right click on an entity
 * or group of entities and select 'Create Alias'.
 * The entities selected are stored in RootNode.setEntitiesToAlias()
 */
public class MarkEntityToAliasAction extends SecureAction {
    static Logger log = Logger.getLogger(CreateFolderAction.class.getName());

    public MarkEntityToAliasAction(AbstractTreeNode abstractTreeNode)
    {
        super(new AttemptedCreate(EntityType.FOLDER), UI_PUBLISH_SERVICE_WIZARD);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Copy as Alias";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Copy as Alias";
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
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        List<AbstractTreeNode> emptyNodes = Collections.emptyList();
        RootNode.setEntitiesToAlias(emptyNodes);
        if (tree != null) {
            List<AbstractTreeNode> abstractTreeNodes = tree.getSmartSelectedNodes();
            long parentFolderOid = -1;
            long currentParnetFolderOid = -1;
            boolean first = true;
            //parentFolderOid must be the same for all nodes and they can't be folder nodes
            for(AbstractTreeNode atn: abstractTreeNodes){
                if(atn instanceof FolderNode){
                    DialogDisplayer.showMessageDialog(tree, "Cannot create alias of a folder", "Cannot select a folder", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                AbstractTreeNode node = (AbstractTreeNode)atn.getParent();
                FolderHeader folderHeader = (FolderHeader) node.getUserObject();
                parentFolderOid = folderHeader.getOid();
                if(first){
                    currentParnetFolderOid = parentFolderOid;
                    first = false;
                }else{
                    if(parentFolderOid != currentParnetFolderOid){
                        DialogDisplayer.showMessageDialog(tree, "All entites must be within the same folder", "Cannot select from multiple folders", JOptionPane.ERROR_MESSAGE, null);                        
                        return;
                    }
                }
                currentParnetFolderOid = parentFolderOid;
            }
            RootNode.setEntitiesToAlias(abstractTreeNodes);
        }
    }
}
