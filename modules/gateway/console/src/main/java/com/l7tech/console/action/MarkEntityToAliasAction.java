package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
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

    public MarkEntityToAliasAction( final EntityHeaderNode entityHeaderNode ) {
        super( getOperation(entityHeaderNode) );
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Copy as Alias";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Copy as Alias";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     */
    @Override
    protected void performAction() {
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        List<AbstractTreeNode> emptyNodes = Collections.emptyList();
        RootNode.setEntitiesToAlias(emptyNodes);
        if (tree != null) {
            List<AbstractTreeNode> abstractTreeNodes = tree.getSmartSelectedNodes();
            Goid parentFolderGoid;
            Goid currentParentFolderGoid = null;
            boolean first = true;
            //parentFolderOid must be the same for all nodes and they can't be folder nodes
            for(AbstractTreeNode atn: abstractTreeNodes){
                if(atn instanceof FolderNode){
                    DialogDisplayer.showMessageDialog(tree, "Cannot create alias of a folder", "Cannot select a folder", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                AbstractTreeNode node = (AbstractTreeNode)atn.getParent();
                FolderHeader folderHeader = (FolderHeader) node.getUserObject();
                parentFolderGoid = folderHeader.getGoid();
                if(first){
                    first = false;
                }else{
                    if(!parentFolderGoid.equals(currentParentFolderGoid)){
                        DialogDisplayer.showMessageDialog(tree, "All entites must be within the same folder", "Cannot select from multiple folders", JOptionPane.ERROR_MESSAGE, null);                        
                        return;
                    }
                }
                currentParentFolderGoid = parentFolderGoid;
            }
            RootNode.setEntitiesToAlias(abstractTreeNodes);
        }
    }

    //- PRIVATE

    private static AttemptedOperation getOperation( final EntityHeaderNode entityHeaderNode ) {
        AttemptedOperation operation = new AttemptedDeleteAll(EntityType.ANY);

        switch ( entityHeaderNode.getEntityHeader().getType() ) {
            case SERVICE:
                operation = new AttemptedCreate( EntityType.SERVICE_ALIAS );
                break;
            case POLICY:
                operation = new AttemptedCreate( EntityType.POLICY_ALIAS );
                break;
        }

        return operation;
    }
}
