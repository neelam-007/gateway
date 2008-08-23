package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;

import java.util.logging.Logger;
import java.util.List;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 20, 2008
 * Time: 10:00:36 AM
 * Action to allow the user to select what entity they want to alias
 */
public class MarkEntityToAliasAction extends SecureAction {
    static Logger log = Logger.getLogger(CreateFolderAction.class.getName());

    private AbstractTreeNode abstractTreeNode;

    public MarkEntityToAliasAction(AbstractTreeNode abstractTreeNode)
    {
        super(new AttemptedCreate(EntityType.FOLDER), UI_PUBLISH_SERVICE_WIZARD);
        this.abstractTreeNode = abstractTreeNode;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Alias";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Create Alias";
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
        RootNode.setEntitiesToAlias(Collections.EMPTY_LIST);
        if (tree != null) {
            List<AbstractTreeNode> abstractTreeNodes = tree.getSmartSelectedNodes();
            long parentFolderOid = -1;
            long currentParnetFolderOid = -1;
            boolean first = true;
            //parentFolderOid must be the same for all nodes and they can't be folder nodes
            //todo [Donal] update error messages when we user selelction is not valid?
            for(AbstractTreeNode atn: abstractTreeNodes){
                if(atn instanceof FolderNode){
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
                        return;
                    }
                }
                currentParnetFolderOid = parentFolderOid;
            }
            RootNode.setEntitiesToAlias(abstractTreeNodes);
        }
    }
}
