package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.action.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class FolderNode extends AbstractTreeNode implements FolderNodeBase{
 
    private FolderHeader folderHeader;

    private final List<Action> actions;

    public FolderNode(FolderHeader folderHeader, PolicyServiceTreeNodeCreator nodeCreator) {
        super(null, RootNode.getComparator());
        this.folderHeader = folderHeader;

        final Folder folder = new Folder(folderHeader.getName(), folderHeader.getParentFolderOid());
        folder.setOid(folderHeader.getOid());

        FolderAdmin folderAdmin = Registry.getDefault().getServiceManager();
        actions = new ArrayList<Action>();
        actions.add(new EditServiceFolderAction(folder, folderHeader, this, folderAdmin));
        actions.add(new CreateFolderAction(folderHeader.getOid(), this, nodeCreator, folderAdmin));
        actions.add(new DeleteFolderAction(folderHeader.getOid(), this, folderAdmin));

        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(EntityType.FOLDER,
                                                                OperationType.UPDATE,
                                                                ServicesAndPoliciesTree.ClipboardActionType.CUT);
        if(secureCut != null) actions.add(secureCut);
        Action securePaste = ServicesAndPoliciesTree.getSecuredAction(EntityType.FOLDER,
                                                                OperationType.UPDATE,
                                                                ServicesAndPoliciesTree.ClipboardActionType.PASTE);
        if(securePaste != null) actions.add(securePaste);
    }

    public String getName() {
        return folderHeader.getName();
    }

    public long getOid() {
        return folderHeader.getOid();
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    public void addChild(AbstractTreeNode child) {
        insert(child, getInsertPosition(child, RootNode.getComparator()));
    }

    public void addEntityNode(EntityHeader entityHeader) {
        AbstractTreeNode child = TreeNodeFactory.asTreeNode(entityHeader, null);
        insert(child, getInsertPosition(child, RootNode.getComparator()));
    }

    @Override
    public Action[] getActions() {
        return actions.toArray(new Action[]{});
    }
}
